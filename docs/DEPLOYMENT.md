# Deployment

Everything runs from `docker compose`. For production the base file is combined with
[`docker-compose.prod.yml`](../docker-compose.prod.yml), which puts Caddy (automatic HTTPS) in front of the gateway and
closes every other port.

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

## What the production overlay changes

| Area | Development | Production |
|---|---|---|
| Public ports | gateway `8080`, services on `127.0.0.1` | only Caddy on `80` / `443` |
| TLS | none | Caddy obtains and renews the certificate for `$DOMAIN` |
| Blocked at the edge | nothing | `/internal/*`, `/swagger-ui*`, `/v3/api-docs*`, `/webjars/*`, and `/actuator/*` except `/actuator/health` |
| Swagger UI | on | off (`SWAGGER_ENABLED=false`) |
| Mail | MailHog (inbox at `http://localhost:8025`) | a real SMTP provider is required |
| Restart policy | none | `unless-stopped` |
| Containers | non-root user, JRE image | same |

## Before the first start

Copy `.env.example` to `.env` and set:

| Variable | Notes |
|---|---|
| `JWT_SECRET` | at least 32 characters, random, never reused: `openssl rand -base64 48` |
| `INTERNAL_API_KEY` | at least 16 characters, random; the gateway refuses to start without it |
| `DB_PASSWORD`, `REDIS_PASSWORD` | change the development defaults |
| `DOMAIN` | public host name, for example `app.example.com`; its DNS record must point at the server |
| `CORS_ALLOWED_ORIGINS` | comma separated front-end origins, for example `https://app.example.com`; also used for WebSocket origins |
| `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH=true`, `SMTP_STARTTLS=true`, `MAIL_FROM` | the mail provider for verification links |
| `ADMIN_EMAIL` | optional; this account becomes `ADMIN` at startup once its email is verified |
| `TICKETMASTER_API_KEY` | optional; enables the nightly event import |

`.env` is git-ignored. Keep it out of backups that leave the server, or encrypt them.

## First start checklist

1. `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build`
2. `docker compose ps` shows every container `healthy`.
3. Register the admin account on the front end (or `POST /user/register`), open the verification mail, then set
   `ADMIN_EMAIL` and restart `a2srv-service`:
   `docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d a2srv-service`.
4. `https://$DOMAIN/actuator/health` answers `UP`; `https://$DOMAIN/swagger-ui.html` and `https://$DOMAIN/internal/token/blacklist` answer `404`.

## Database

Each service owns its schema through Flyway migrations (`src/main/resources/db/migration`); Hibernate only validates.
To change a table, add a new `V<n>__description.sql`, never edit an applied file. Migrations run on start, so a
deployment migrates the database as part of `up -d --build`.

### Backups

```bash
./scripts/backup.sh                 # writes backups/mingle-<timestamp>.sql.gz, keeps the newest 14
KEEP=30 BACKUP_DIR=/srv/backups ./scripts/backup.sh
```

Schedule it, for example with cron: `0 3 * * * cd /opt/mingle && ./scripts/backup.sh`. Copy the files off the server;
a backup on the same disk does not survive losing the disk. The script refuses to keep an empty dump.

### Restore

`./scripts/restore.sh <dump>` restores into an **empty** database and refuses to touch one that has tables. To
restore over a live database: stop the application containers, drop and recreate the `mingle` database, run the
script, start the containers again. Test a restore on a spare machine before you need it.

## Updating

```bash
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Take a backup first. Services restart one by one as their images change; Flyway applies new migrations on start.

## Not covered yet

- Central log shipping and alerting (logs are on each container's stdout: `docker compose logs`).
- A replicated database; PostgreSQL runs as one container with a volume.
- Horizontal scaling: the chat uses an in-memory STOMP broker, so run a single `match-service` instance until it is
  moved to a shared broker.
- The payment provider for chat unlock (see [`TODO.md`](../TODO.md)).
