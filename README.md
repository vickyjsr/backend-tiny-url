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

## Production notes

- Profile: `prod`
- Override DB password with `DB_PASSWORD`
- Prefer lean Hikari/Tomcat settings on small Lightsail instances
