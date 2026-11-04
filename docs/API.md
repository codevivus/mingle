# API guide

For front-end and mobile developers. Every request goes through the gateway (`http://localhost:8080` in development,
`https://$DOMAIN` in production). The machine-readable contracts are in [`openapi/`](openapi) (one file per service,
regenerated with `node scripts/export-openapi.mjs`); this page explains how the pieces fit together.

## Conventions

| | |
|---|---|
| Format | JSON, `Content-Type: application/json` |
| Auth | `Authorization: Bearer <token>` from `POST /auth/login`. Public: login, validate, verify, resend-verification, register, check-email, check-username, health |
| Identity | Never sent by the client. The gateway validates the token and tells the services who you are; any `X-User-Id`, `X-User-Role` or `X-User-Verified` you send is discarded |
| Ids | numbers (`Long`) |
| Times | event times are venue-local without a zone (`2030-05-01T20:00:00`); everything else is ISO-8601 UTC (`2030-05-01T17:00:00Z`) |
| Errors | `{ "status": 403, "error": "Forbidden", "message": "...", "path": "..." }` |

### Status codes

| Code | Meaning |
|---|---|
| 400 | invalid body or parameter |
| 401 | missing, invalid, expired or logged-out token; wrong password |
| 402 | unlocking a chat needs payment |
| 403 | not allowed: not your resource, admin only, email not verified, no ticket for that event |
| 404 | not found (also: a user you cannot see because of a block) |
| 410 | the conversation ended (block or unmatch) |
| 423 | chat is closed for sending (`READ_ONLY`) or for reading too (`LOCKED`) |
| 429 | rate limited; slow down |
| 502 / 503 | a dependency or the service itself is down (circuit breaker) |

## Sign-up and login

1. `POST /user/register` with `username, email, password, name, surname, age, gender, city, birthDate` (plus optional
   profile fields). A verification mail is sent.
2. The user opens the mailed link: `GET /auth/verify?token=...` -> `{ "verified": true }`. Links work once and expire
   after 24 hours; `POST /auth/resend-verification {"email": "..."}` always answers `202`.
3. `POST /auth/login {"email", "password"}` -> `{ "token", "refreshToken", "userId", "username", "verified" }`.
   The access token lives 1 hour; `POST /auth/refresh {"refreshToken"}` issues a new one.
4. `POST /auth/logout` blacklists the token.

An unverified account can log in and edit its profile but **cannot add tickets or match** (`403`).

## The main flow

```
events -> add a ticket -> candidates -> swipe -> match -> chat
```

| Step | Call |
|---|---|
| Browse events | `GET /event`, `GET /event/{id}`, `GET /event/cities` |
| Add your ticket | `POST /ticket/add {"eventId": 10}` (verified users; the owner is always you) |
| Your tickets | `GET /ticket`, `GET /ticket/event/{eventId}`, `DELETE /ticket/{id}` |
| People going to the same event | `GET /match/events/{eventId}/candidates?limit=50` -> `[{userId, name, surname, age, city, photoUrl, bio}]` |
| Like or pass | `POST /match/events/{eventId}/swipe {"targetUserId": 2, "action": "LIKE"}` -> `{ "matched": true, "matchId": 7 }` |
| Your matches | `GET /match/matches[?eventId=]` -> `[{id, eventId, otherUserId, createdAt}]` |

Matching is per event: the same two people can match again at another event. A mutual like opens a conversation.

## Chat

REST:

| Call | Notes |
|---|---|
| `GET /chat/conversations` | each has `status`, `lockAt`, `readUntil`, `unreadCount`, `lastMessage`, `otherLastReadMessageId` |
| `GET /chat/{id}` | one conversation |
| `GET /chat/{id}/messages?page=0&size=50` | oldest first within the page; page 0 is the newest 50 |
| `POST /chat/{id}/messages {"content": "..."}` | up to 2000 characters |
| `POST /chat/{id}/read {"upToMessageId": 42}` | body optional (default: everything); never moves backwards |
| `GET /chat/unread-count` | `{ "count": 3 }` for a badge |
| `POST /chat/{id}/unlock` | reopens a closed chat after payment (currently a stub that always succeeds) |

Real time: STOMP over a plain WebSocket.

```
ws://localhost:8080/ws?access_token=<jwt>        (browsers cannot set headers on a WebSocket)
SUBSCRIBE  /topic/conversations/{id}             new messages arrive here as MessageResponse JSON
SEND       /app/chat/{id}    {"content": "..."}  send a message
SUBSCRIBE  /user/queue/errors                    {status, message} when a send is refused (for example 423)
```

You can only subscribe to conversations you belong to. Messages sent over REST are broadcast too.

### Chat lifecycle

| Status | When | Send | Read |
|---|---|---|---|
| `ACTIVE` | until 24 h after the event ends | yes | yes |
| `READ_ONLY` | the next 30 days | `423` | yes |
| `LOCKED` | after that | `423` | `423` |
| ended | block or unmatch | `410` | `410` |

`POST /chat/{id}/unlock` brings a `READ_ONLY` or `LOCKED` chat back to `ACTIVE`. An ended conversation cannot be unlocked.

## Safety

| Call | Effect |
|---|---|
| `POST /match/blocks/{userId}` | hides you from each other, ends every match between you and closes the conversations |
| `DELETE /match/blocks/{userId}` | unblock (ended matches stay ended) |
| `GET /match/blocks` | people you blocked |
| `DELETE /match/matches/{id}` | unmatch: ends that match and closes its conversation |
| `POST /match/reports {"reportedUserId", "reason", "note", "eventId", "block"}` | `reason`: `SPAM, HARASSMENT, FAKE_PROFILE, INAPPROPRIATE_CONTENT, UNDERAGE, OTHER`; `"block": true` also blocks |

Admins: `GET /match/admin/reports` (open reports), `POST /match/admin/reports/{id}/resolve`.

## Your account and data

| Call | |
|---|---|
| `GET /user/me`, `PUT /user/me` | profile |
| `PUT /user/password` | change password |
| `GET /user/me/export` | everything stored about you (profile, tickets, swipes, matches, your messages, blocks, reports you filed) |
| `DELETE /user/me {"password": "..."}` | erases the account and its data in every service, and logs the token out. Reports other people filed about you stay for moderation |

`GET /user/search/*` (city, gender, goal, age range, verified, online, nearby) lists profiles for browsing.

## Admin

Admins (`role = ADMIN`) can create, update, delete and import events: `POST /event`, `PUT /event/{id}`,
`DELETE /event/{id}`, `POST /event/import?city=...`.

## Limits

Requests are rate limited per user (per IP before login): auth and user routes 5/s with bursts of 10, other routes 20/s
with bursts of 40, WebSocket handshakes 5/s. Over the limit you get `429`.
