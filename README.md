# SlashURL Backend

Spring Boot 3 / Java 17 API for [slashurl.com](https://slashurl.com).

## Package layout

```
com.tiny.url
├── TinyUrlBackendApplication   # entrypoint
├── config/                     # CORS, Redis, OpenAPI (non-prod)
├── controller/                 # HTTP layer
├── service/                    # business logic + Redis cache
├── repository/                 # JPA
├── entity/                     # persistence models
├── dto/                        # API responses
├── mapper/                     # entity → dto
├── exception/                  # domain errors + advice
└── util/                       # code gen, validation, constants
```

## Run locally

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Swagger (non-prod): `http://localhost:8080/swagger-ui.html`

## Main endpoints

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/new?originalUrl=` | Create short URL |
| `GET`  | `/get?tinyUrl=` | Lookup original URL |
| `GET`  | `/{code}` | 302 redirect |

## Redis cache keys

- `url:tiny:{code}`
- `url:original:{originalUrl}`
- `analytics:clicks:{urlId}`

## Production / redeploy

Infrastructure templates and agent runbook live in **`deploy/`** (nginx, systemd, PM2, MySQL/Redis tuning).  
**No MySQL or Redis data dumps** are stored — schema is recreated empty on next deploy.

Start here: [`deploy/AGENTS.md`](deploy/AGENTS.md)

```bash
# On a fresh Ubuntu 24.04 Lightsail box:
scp -r deploy slashurl:~/
ssh slashurl 'bash ~/deploy/scripts/bootstrap-ubuntu.sh ~/deploy'
```

## Production notes

- Profile: `prod`
- Override DB password with `SPRING_DATASOURCE_PASSWORD` / server properties (see `deploy/backend/`)
- Prefer lean Hikari/Tomcat/JVM settings on ≤1GB Lightsail (unit file in `deploy/systemd/`)
