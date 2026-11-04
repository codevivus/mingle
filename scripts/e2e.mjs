// Live end-to-end check against a running `docker compose up` stack (Node 22+, run from the repo root):
//   node scripts/e2e.mjs
// Registers two users, promotes one to admin, creates an event, buys tickets, matches, chats over REST and STOMP.
import { execSync } from "node:child_process";

const GW = "http://localhost:8080";
const sfx = Date.now().toString().slice(-6);
const log = (...a) => console.log(...a);

async function call(method, path, { token, body } = {}) {
  const res = await fetch(GW + path, {
    method,
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: "Bearer " + token } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* not json */ }
  return { status: res.status, json, text };
}

function expect(label, cond, extra = "") {
  log((cond ? "PASS " : "FAIL ") + label + (cond ? "" : "  -> " + extra));
  if (!cond) process.exitCode = 1;
}

async function register(name) {
  const r = await call("POST", "/user/register", {
    body: { username: name, email: name + "@example.com", password: "Passw0rd!123", name, surname: "Test",
            age: 28, gender: "male", city: "Istanbul", birthDate: "1997-01-01" },
  });
  expect("register " + name, r.status >= 200 && r.status < 300, r.status + " " + r.text.slice(0, 200));
  return r.json;
}

async function login(name) {
  const r = await call("POST", "/auth/login", { body: { email: name + "@example.com", password: "Passw0rd!123" } });
  expect("login " + name, r.status === 200, r.status + " " + r.text.slice(0, 200));
  return r.json;
}

const a = "e2ea" + sfx, b = "e2eb" + sfx;
const ra = await register(a), rb = await register(b);
const la = await login(a), lb = await login(b);
const tokA = la.token, tokB = lb.token;
if (!tokA || !tokB) { log("login payload keys:", Object.keys(la || {})); process.exit(1); }
const idA = Number(la.userId), idB = Number(lb.userId);
log("user ids", idA, idB);

// Without the admin role an event cannot be created.
const ev = { title: "E2E event " + sfx, description: "e2e", city: "Istanbul", venue: "Hall",
  startTime: new Date(Date.now() + 3600e3).toISOString().slice(0, 19),
  endTime: new Date(Date.now() + 2 * 3600e3).toISOString().slice(0, 19), imageUrl: "http://x/y.png" };
let r = await call("POST", "/event", { token: tokA, body: ev });
expect("non-admin cannot create event (403)", r.status === 403, r.status);

execSync(`docker compose exec -T postgres psql -U mingle -d mingle -c "UPDATE users SET role='ADMIN' WHERE username='${a}'"`,
  { stdio: "pipe" });
r = await call("POST", "/event", { token: tokA, body: ev });
expect("admin creates event (201)", r.status === 201, r.status + " " + r.text.slice(0, 200));
const eventId = r.json?.id;

for (const [n, t] of [[a, tokA], [b, tokB]]) {
  r = await call("POST", "/ticket/add", { token: t, body: { eventId, userId: 999999 } });
  expect("ticket for " + n + " (userId in body ignored)", r.status === 200, r.status + " " + r.text.slice(0, 200));
}
r = await call("GET", "/ticket", { token: tokA });
expect("A sees only own ticket", r.status === 200 && r.json?.length === 1, r.status + " " + r.text.slice(0, 200));
r = await call("GET", "/ticket/user/" + idB, { token: tokA });
expect("A cannot list B's tickets (403)", r.status === 403, r.status);

r = await call("GET", `/match/events/${eventId}/candidates`, { token: tokA });
expect("A's candidates = [B]", r.status === 200 && r.json?.length === 1 && r.json[0].userId === idB, r.status + " " + r.text);
r = await call("POST", `/match/events/${eventId}/swipe`, { token: tokA, body: { targetUserId: idB, action: "LIKE" } });
expect("A likes B, no match yet", r.status === 200 && r.json?.matched === false, r.status + " " + r.text);
r = await call("POST", `/match/events/${eventId}/swipe`, { token: tokB, body: { targetUserId: idA, action: "LIKE" } });
expect("B likes A, match", r.status === 200 && r.json?.matched === true, r.status + " " + r.text);
r = await call("GET", `/match/events/${eventId}/candidates`, { token: tokA });
expect("liked user leaves candidates", r.status === 200 && r.json?.length === 0, r.text);

r = await call("GET", "/chat/conversations", { token: tokA });
expect("A has an ACTIVE conversation", r.status === 200 && r.json?.length === 1 && r.json[0].status === "ACTIVE", r.status + " " + r.text);
const convId = r.json?.[0]?.id;
r = await call("POST", `/chat/${convId}/messages`, { token: tokA, body: { content: "hello via REST" } });
expect("REST message sent", r.status === 200, r.status + " " + r.text);
r = await call("GET", `/chat/${convId}/messages`, { token: tokB });
expect("B reads history", r.status === 200 && r.json?.length === 1, r.status + " " + r.text);

// STOMP over WebSocket through the gateway.
function stompClient(token, label) {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(`ws://localhost:8080/ws?access_token=${token}`, ["v12.stomp"]);
    const inbox = [];
    const waiters = [];
    const frame = (cmd, headers, body = "") =>
      cmd + "\n" + Object.entries(headers).map(([k, v]) => `${k}:${v}`).join("\n") + "\n\n" + body + "\0";
    ws.onerror = () => reject(new Error(label + " websocket error"));
    ws.onclose = (e) => { if (!inbox.connected) reject(new Error(label + " closed before CONNECTED, code " + e.code)); };
    ws.onmessage = (ev) => {
      for (const raw of String(ev.data).split("\0").filter((s) => s.trim())) {
        const [head, ...rest] = raw.replace(/^\n+/, "").split("\n\n");
        const cmd = head.split("\n")[0];
        if (cmd === "CONNECTED") {
          inbox.connected = true;
          resolve({
            subscribe: (dest) => ws.send(frame("SUBSCRIBE", { id: "sub-" + dest, destination: dest })),
            send: (dest, payload) => ws.send(frame("SEND", { destination: dest, "content-type": "application/json" }, JSON.stringify(payload))),
            next: (ms = 5000) => new Promise((res) => {
              if (inbox.length) return res(inbox.shift());
              const t = setTimeout(() => res(null), ms);
              waiters.push((m) => { clearTimeout(t); res(m); });
            }),
            close: () => ws.close(),
          });
        } else if (cmd === "MESSAGE" || cmd === "ERROR") {
          const msg = { cmd, headers: head, body: rest.join("\n\n") };
          const w = waiters.shift();
          w ? w(msg) : inbox.push(msg);
        }
      }
    };
    ws.onopen = () => ws.send(frame("CONNECT", { "accept-version": "1.2", host: "localhost" }));
  });
}

try {
  const sa = await stompClient(tokA, "A"), sb = await stompClient(tokB, "B");
  expect("both STOMP sessions connected through the gateway", true);
  sb.subscribe("/topic/conversations/" + convId);
  await new Promise((r) => setTimeout(r, 500));
  sa.send("/app/chat/" + convId, { content: "hello via STOMP" });
  const m = await sb.next();
  expect("B receives A's STOMP message", m?.cmd === "MESSAGE" && m.body.includes("hello via STOMP"), JSON.stringify(m));
  sa.close(); sb.close();
} catch (e) {
  expect("STOMP flow", false, e.message);
}

try {
  await stompClient("not-a-token", "bad");
  expect("handshake with a bad token is refused", false, "connected");
} catch {
  expect("handshake with a bad token is refused", true);
}
log(process.exitCode ? "RESULT: FAILURES" : "RESULT: ALL PASSED");
