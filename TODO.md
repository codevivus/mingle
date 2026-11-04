# TODO

Open work, roughly in priority order.

## Needs a decision
- [ ] **Payment provider for chat unlock.** `ChatUnlockService` is a stub (`StubChatUnlockService`) that grants every unlock for free. Pick a provider, add a real implementation next to the stub and select it with `CHAT_UNLOCK_PROVIDER`. Until then the paid lock does not protect anything. Also decide: one-off price per chat or per user, who pays (either participant or both), and refunds.
- [ ] **Read window length.** `CHAT_READ_ONLY_DAYS` defaults to 30 (the "some time later, reading becomes paid" period); confirm the real value.

## Verification
- [x] Live end-to-end run on the compose stack: `node scripts/e2e.mjs` (all checks pass).
- [ ] Check the first CI run on `main` after the merges.

## Product
- [ ] Enrich `GET /match/events/{id}/candidates` with name and photo from a2srv-service (it returns only `userId` today).
- [ ] Admin bootstrap: there is no admin by default; promote one by SQL (see README) or add a seed / admin endpoint.
- [ ] Event dates have no time zone; they are treated as UTC when computing the chat deadlines.

## Technical debt
- [ ] Logout sends the blacklist TTL in milliseconds but the gateway treats it as seconds (entries live about 1000x too long).
- [ ] Upgrade Testcontainers (1.19.x fails against Docker Engine 29 without `api.version=1.44` in `~/.docker-java.properties`).
- [ ] Commit the executable bit for the `mvnw` scripts instead of `chmod +x` in CI.
- [ ] Delete merged branches on GitHub.
