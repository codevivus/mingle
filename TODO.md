# TODO

Open work, roughly in priority order.

## Needs a decision
- [ ] **Payment provider for chat unlock.** `ChatUnlockService` is a stub (`StubChatUnlockService`) that grants every unlock for free. Pick a provider (iyzico is the suggestion for TL / Turkey, Stripe for international), add a real implementation next to the stub and select it with `CHAT_UNLOCK_PROVIDER`. Until then the paid lock does not protect anything. Also decide: one-off price per chat, who pays (suggested: either participant, chat opens for both), and refunds (suggested: none at first).
- [ ] **Read window length.** `CHAT_READ_ONLY_DAYS` defaults to 30 (the "reading becomes paid later" period); confirm the real value.

## Product
- [ ] Per-event time zone. Event times are venue-local with no zone; `EVENT_TIME_ZONE` (default `Europe/Istanbul`) applies to all events.

## Technical debt
- [ ] Delete the merged branches on GitHub (docs/readme-ci, feat/chat, feat/gateway-resilience, feat/identity-ownership, feat/match-service, fix/ci-mvnw-permission, fix/stack-startup), or turn on "Automatically delete head branches" in the repository settings.
- [ ] Candidate profiles are fetched one by one (page of at most 100); add a batch endpoint in a2srv-service if that gets slow.

## Done
- [x] Chat read state: unread counts, last message, read receipts, total unread badge.
- [x] Account export and deletion across services; logout now really reaches the gateway (it failed with "Host is not specified" and never blacklisted a token); a2srv talks to other services through Eureka.
- [x] Block, unmatch and report (users) plus an admin list of open reports.
- [x] Testcontainers 1.21.4 (works with Docker Engine 29, no local workaround), `mvnw` committed executable.
- [x] Ticketmaster import tested against a local stand-in (documented response shape). Found and fixed: venue and city were never read (they sit under each event`s `_embedded`), and the nightly cleanup crashed on a missing cities list and could never match imported events (no end time). Still not run against the real API: needs `TICKETMASTER_API_KEY`.
- [x] Production overlay: Caddy HTTPS, closed ports, Swagger off, non-root JRE images, secret length checks, backup and restore scripts (docs/DEPLOYMENT.md).
- [x] Flyway for a2srv, event and ticket services (V1 baseline is idempotent, so existing databases are baselined); Hibernate only validates. New schema changes are new `V<n>__*.sql` files.
- [x] Email verification (mailed link, hashed token, 24 h); unverified users cannot add tickets or match; `ADMIN_EMAIL` only promotes verified accounts. Real SMTP provider is still to be chosen (MailHog in development).
- [x] Live end-to-end run on the compose stack: `node scripts/e2e.mjs`.
- [x] Candidates include name, age, city and photo.
- [x] Logout blacklists a token for its remaining lifetime in seconds (it was milliseconds).
- [x] `ADMIN_EMAIL` promotes an existing account at startup.
- [x] `EVENT_TIME_ZONE` for the chat deadlines.
