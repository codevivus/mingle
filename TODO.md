# TODO

Open work, roughly in priority order.

## Needs a decision
- [ ] **Payment provider for chat unlock.** `ChatUnlockService` is a stub (`StubChatUnlockService`) that grants every unlock for free. Pick a provider (iyzico is the suggestion for TL / Turkey, Stripe for international), add a real implementation next to the stub and select it with `CHAT_UNLOCK_PROVIDER`. Until then the paid lock does not protect anything. Also decide: one-off price per chat, who pays (suggested: either participant, chat opens for both), and refunds (suggested: none at first).
- [ ] **Read window length.** `CHAT_READ_ONLY_DAYS` defaults to 30 (the "reading becomes paid later" period); confirm the real value.

## Product
- [ ] Per-event time zone. Event times are venue-local with no zone; `EVENT_TIME_ZONE` (default `Europe/Istanbul`) applies to all events.
- [ ] Email verification (`verified` is never set to true). Until then `ADMIN_EMAIL` must be registered by its owner before the service is exposed.

## Technical debt
- [ ] Upgrade Testcontainers (1.19.x fails against Docker Engine 29 without `api.version=1.44` in `~/.docker-java.properties`).
- [ ] Commit the executable bit for the `mvnw` scripts instead of `chmod +x` in CI.
- [ ] Candidate profiles are fetched one by one (page of at most 100); add a batch endpoint in a2srv-service if that gets slow.
- [ ] Delete merged branches on GitHub.

## Done
- [x] Live end-to-end run on the compose stack: `node scripts/e2e.mjs`.
- [x] Candidates include name, age, city and photo.
- [x] Logout blacklists a token for its remaining lifetime in seconds (it was milliseconds).
- [x] `ADMIN_EMAIL` promotes an existing account at startup.
- [x] `EVENT_TIME_ZONE` for the chat deadlines.
