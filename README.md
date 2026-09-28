# CacheFlow API

[![CI](https://github.com/Mohangab/cacheflow-api/actions/workflows/ci.yml/badge.svg)](https://github.com/Mohangab/cacheflow-api/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7-red?logo=redis)](https://redis.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Spring Boot 3 + Redis** product-catalog demo by [Mohan Venkatesh Kola](https://github.com/Mohangab).

Resume line this project proves:

> *Used Redis with Spring Cache (`@Cacheable` / `@CacheEvict`) for hot reference data, TTL expiry, and write-path eviction.*

Small, polished, recruiter-friendly — Redis is the star. **JWT / security was intentionally skipped** so the cache story stays front and center.

---

## Why Redis here?

| Layer | Role |
|--------|------|
| **H2 (JPA)** | Source of truth — durable product rows for local demos |
| **Redis** | Cache-aside layer for hot reads (`GET /api/products/{id}`, `GET /api/products/category/{category}`) |

Reference data (product catalog) is read far more than written. Caching it in Redis:

1. **Cuts latency** on repeat reads (see curl timings below — first call ~400ms+ simulated DB, second call near-instant).
2. **TTL** auto-expires stale entries (`products` = 5 min, `productsByCategory` = 2 min by default).
3. **Eviction on writes** keeps the cache coherent when you create / update / delete.

```mermaid
flowchart TD
  Client[Client / curl / Swagger]
  API[Spring Boot ProductService]
  Redis[(Redis cache)]
  DB[(H2 / JPA)]

  Client -->|GET by id or category| API
  API -->|@Cacheable| Redis
  Redis -->|HIT| API
  Redis -->|MISS| DB
  DB -->|load + TTL populate| Redis
  API -->|JSON response| Client
  Client -->|POST / PUT / DELETE| API
  API -->|@CacheEvict / @CachePut| Redis
```

**Miss vs hit in one glance:** first `GET /api/products/1` sleeps ~400ms on the DB path; the second call is served from Redis and is near-instant. Watch logs for `CACHE MISS` / `CACHE HIT`.

---

## Stack

- Java 17, Spring Boot 3.3, Maven
- Spring Data JPA + **H2** (in-memory DB)
- Spring Data Redis + Spring Cache (`@Cacheable`, `@CachePut`, `@CacheEvict`)
- springdoc OpenAPI (Swagger UI)
- Actuator (`/actuator/health`, `/actuator/caches`)
- Docker multi-stage build + Compose (`app` + `redis:7-alpine`)
- GitHub Actions CI (`mvn test`)

---

## Project layout

```
cacheflow-api/
├── src/main/java/com/mohan/cacheflow/
│   ├── config/          # RedisCacheConfig, LoggingCacheManager, OpenAPI, seeder
│   ├── controller/      # Product + Demo (/api/demo/cache-stats)
│   ├── dto/             # ProductRequest / ProductResponse
│   ├── entity/          # Product (JPA)
│   ├── repository/      # ProductRepository
│   └── service/         # ProductService (cache annotations live here)
├── Dockerfile
├── docker-compose.yml
└── .github/workflows/ci.yml
```

---

## How the cache works

| API | Cache | On write |
|-----|--------|----------|
| `GET /api/products/{id}` | `products` key=`id`, TTL 300s | — |
| `GET /api/products/category/{cat}` | `productsByCategory` key=`cat.lower`, TTL 120s | — |
| `POST /api/products` | — | evict **all** `productsByCategory` |
| `PUT /api/products/{id}` | `@CachePut` refreshes `products` | evict `productsByCategory` |
| `DELETE /api/products/{id}` | evict `products` key | evict `productsByCategory` |
| `GET /api/products` (list all) | **not cached** — full scan stays on DB | — |

**Visibility for demos**

- Application logs: `CACHE HIT` / `CACHE MISS` from `LoggingCacheManager`, plus `DB LOAD` when the service method body runs.
- `GET /api/demo/cache-stats` — DB-load counter + sample Redis keys.
- Artificial **~400ms** sleep on DB load so curl timings make MISS vs HIT obvious.

---

## Quick start

### Option A — Docker Compose (recommended)

Needs Docker. Starts Redis + the API.

```bash
docker compose up --build
```

API: http://localhost:8080  
Swagger: http://localhost:8080/swagger-ui.html  
Health: http://localhost:8080/actuator/health  
Cache demo: http://localhost:8080/api/demo/cache-stats

### Option B — Maven + local Redis

1. Start Redis 7 (any of):

   ```bash
   docker run --rm -p 6379:6379 redis:7-alpine
   # or: redis-server
   ```

2. Run the app:

   ```bash
   mvn spring-boot:run
   ```

Optional env (see `.env.example`): `REDIS_HOST`, `REDIS_PORT`, `CACHE_PRODUCTS_TTL`, `CACHE_CATEGORY_TTL`.

### Tests

```bash
mvn -B test
# or full package:
mvn -B package
```

Cache tests use **embedded Redis** (`com.github.codemonstur:embedded-redis`) — no Docker required in CI.

---

## Curl demo — miss then hit

Seed data loads six products on startup. Product `1` is *Wireless Mouse*.

```bash
# Clear demo counters / caches (optional)
curl -s -X DELETE http://localhost:8080/api/demo/cache

# 1st call → CACHE MISS → ~400ms+ (DB load)
time curl -s http://localhost:8080/api/products/1 | jq .

# 2nd call → CACHE HIT  → much faster (Redis)
time curl -s http://localhost:8080/api/products/1 | jq .

# Watch Redis keys + DB-load counter
curl -s http://localhost:8080/api/demo/cache-stats | jq .

# Category list — same miss/hit pattern
time curl -s http://localhost:8080/api/products/category/Electronics | jq 'length'
time curl -s http://localhost:8080/api/products/category/Electronics | jq 'length'

# Write path eviction — update then re-read category (miss again)
curl -s -X PUT http://localhost:8080/api/products/1 \
  -H 'Content-Type: application/json' \
  -d '{"name":"Wireless Mouse Pro","category":"Electronics","price":34.99,"description":"Updated"}' | jq .

time curl -s http://localhost:8080/api/products/category/Electronics | jq 'length'
```

Watch the app logs for lines like:

```
CACHE MISS — cache='products' key=1 (loading from DB next)
DB LOAD — product id=1 from H2 (cache miss path)
CACHE HIT  — cache='products' key=1
CACHE EVICT — cache='productsByCategory' ...
```

---

## API map

| Method | Path | Notes |
|--------|------|--------|
| GET | `/api/products` | All products (uncached) |
| GET | `/api/products/{id}` | **Cached** |
| GET | `/api/products/category/{category}` | **Cached** |
| POST | `/api/products` | Create + evict category cache |
| PUT | `/api/products/{id}` | Update + refresh/evict |
| DELETE | `/api/products/{id}` | Delete + evict |
| GET | `/api/demo/cache-stats` | Demo visibility |
| DELETE | `/api/demo/cache` | Clear both caches |
| GET | `/swagger-ui.html` | OpenAPI UI |
| GET | `/actuator/health` | Liveness |

---

## Design choices (interview talking points)

1. **Cache-aside** via Spring Cache annotations — idiomatic Spring, easy to explain.
2. **JSON values in Redis** (`GenericJackson2JsonRedisSerializer`) — inspectable with `redis-cli`.
3. **Per-cache TTLs** — lists expire faster than single-product entries.
4. **Evict category lists on any write** — simple coherence; acceptable at catalog scale.
5. **H2 for local / Compose** — zero external DB friction; swap URL to Postgres later if needed.
6. **No JWT** — deliberate. Auth would dilute the Redis showcase.

---

## License

MIT © Mohan Venkatesh Kola
