<div align="center">

# Mingle

**An event-centred social platform built as Spring Cloud microservices.**

Find events, get tickets, and meet people who are going to the same place.

![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-2023.0-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?logo=docker&logoColor=white)

</div>

---

## Architecture

<p align="center">
  <img src="docs/screenshots/architecture.png" alt="Mingle service topology" width="100%" />
</p>

| Service | Port | Responsibility |
|---|---|---|
| `eureka-server` | 8761 | Service discovery (Netflix Eureka) |
| `api-gateway` | 8080 | Single entry point: routing, JWT validation, token blacklist |
| `a2srv-service` | 8051 | Authentication and user profiles |
| `event-service` | 8090 | Event catalogue, imported on a schedule from the Ticketmaster Discovery API |
| `ticket-service` | 8058 | Users' tickets for events (calls `event-service` via Feign) |
| `postgres` | — | Shared PostgreSQL 16 database |
| `redis` | — | Revoked-token blacklist |

All services register with Eureka, and the gateway forwards traffic with `lb://<service-name>` load-balanced routes defined in [`api-gateway/src/main/resources/authorization.yaml`](api-gateway/src/main/resources/authorization.yaml).

## Features

- **Gateway security** — a global JWT filter validates tokens against `a2srv-service`; public paths (login, registration, health, API docs) are whitelisted.
- **Token lifecycle** — access and refresh tokens, logout with server-side token blacklisting.
- **User profiles** — registration with email / username availability checks, profile and password updates, and search by city, age range, gender, goal, date preference, online status, recency, verification and proximity.
- **Events** — CRUD and listing, city list, manual import trigger, nightly Ticketmaster import (03:00) and cleanup of expired events (04:00).
- **Tickets** — create, delete and list tickets, per user or per event.
- **API documentation** — springdoc / Swagger UI on every service.
- **Observability** — Spring Boot Actuator health endpoints, used by Docker Compose health checks.

## API Overview

All requests go through the gateway at `http://localhost:8080`.

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/login` · `POST /auth/refresh` · `POST /auth/logout` · `POST /auth/validate` |
| Users | `POST /user/register` · `GET /user/check-email` · `GET /user/check-username` · `GET/PUT /user/me` · `PUT /user/password` · `GET /user/search/*` |
| Events | `GET/POST /event` · `GET/PUT/DELETE /event/{id}` · `GET /event/cities` · `POST /event/import` |
| Tickets | `POST /ticket/add` · `GET /ticket` · `GET/DELETE /ticket/{id}` · `GET /ticket/user/{userId}` · `GET /ticket/event/{eventId}` |

Swagger UI per service (local only): `http://localhost:<port>/swagger-ui.html`.

## Tech Stack

| Area | Technology |
|---|---|
| Language / framework | Java 17 · Spring Boot 3.2 · Spring Cloud 2023.0 |
| Edge | Spring Cloud Gateway (reactive) |
| Discovery | Netflix Eureka |
| Security | Spring Security · JJWT |
| Persistence | Spring Data JPA · PostgreSQL |
| Token blacklist | Redis 7 |
| Inter-service calls | OpenFeign |
| Mapping | MapStruct · Lombok |
| Docs | springdoc-openapi |
| Build / run | Maven · Docker Compose |

Each service follows the same package layout: `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception`, `validation`, `config`.

## Getting Started

### Prerequisites

- Docker and Docker Compose
- (Optional) A Ticketmaster API key for event imports

### Run

```bash
git clone https://github.com/codevivus/mingle.git
cd mingle

cp .env.example .env   # set JWT_SECRET (and TICKETMASTER_API_KEY if you have one)
docker compose up -d --build
```

Services start in dependency order (PostgreSQL / Redis → Eureka → Gateway → auth → event / ticket) using health checks. Only the gateway is published on all interfaces; the other service ports are bound to `127.0.0.1` for local debugging.

- Gateway: <http://localhost:8080>
- Eureka dashboard: <http://localhost:8761>

### Run a single service locally

```bash
cd event-service
./mvnw spring-boot:run
```

Configuration comes from environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `REDIS_PASSWORD`, `TICKETMASTER_API_KEY`) with local-development defaults in each service's `application.properties`.
