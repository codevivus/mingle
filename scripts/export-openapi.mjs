// Writes each service's OpenAPI document to docs/openapi/<service>.json from a running
// `docker compose up` stack (Node 22+, run from the repo root):  node scripts/export-openapi.mjs
// The ports are the loopback ports docker-compose.yml publishes for local debugging.
import { mkdir, writeFile } from "node:fs/promises";

const services = { "a2srv-service": 8051, "event-service": 8090, "ticket-service": 8058, "match-service": 8095 };

await mkdir("docs/openapi", { recursive: true });
let failed = false;
for (const [name, port] of Object.entries(services)) {
  try {
    const res = await fetch(`http://127.0.0.1:${port}/v3/api-docs`);
    if (!res.ok) throw new Error("HTTP " + res.status);
    const doc = await res.json();
    // Stable output: no server URL (it differs per environment), sorted paths.
    delete doc.servers;
    doc.paths = Object.fromEntries(Object.entries(doc.paths ?? {}).sort(([a], [b]) => a.localeCompare(b)));
    await writeFile(`docs/openapi/${name}.json`, JSON.stringify(doc, null, 2) + "\n");
    console.log(`${name}: ${Object.keys(doc.paths).length} paths`);
  } catch (e) {
    failed = true;
    console.error(`${name}: ${e.message}`);
  }
}
process.exit(failed ? 1 : 0);
