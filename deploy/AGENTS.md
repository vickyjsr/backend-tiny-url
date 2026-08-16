# SlashURL production deploy (agent runbook)

This folder is the **source of truth for infrastructure config** (nginx, systemd, PM2, MySQL/Redis tuning).
It intentionally does **not** include MySQL dumps or Redis RDB/AOF data.

Last captured from Lightsail (`ip-172-26-10-167`, public `3.6.60.6`) before prod shutdown (2026-08-16).

## Architecture

```
Internet → Cloudflare (www / apis) and/or apex A record
        → Lightsail Ubuntu 24.04 (ports 22/80/443)
            nginx :443
              ├─ slashurl.com / www → PM2 Express :3000 (static React)
              └─ apis.slashurl.com  → Java Spring Boot :8080
            MySQL :3306 (localhost only)  DB: tiny_url  user: tiny
            Redis :6379 (localhost only)
```

Domains:
- `slashurl.com`, `www.slashurl.com` — frontend
- `apis.slashurl.com` — API (+ CORS handled in nginx for OPTIONS)
- Optional: `prod-jenkins.slashurl.com` → :9091 (Jenkins not required for core product)

## Repos

| Piece | Repo / path |
|-------|-------------|
| Backend | this repo (`TinyUrlBackend`) |
| Frontend | sibling `tiny-url-fe` (build → copy to `/opt/tinyurl/frontend/app`) |
| Deploy templates | `deploy/` (this directory) |

## Target paths on server

| Path | Purpose |
|------|---------|
| `/var/www/build/Tiny-0.0.1-SNAPSHOT.jar` | Backend JAR |
| `/var/www/build/application-prod.properties` | Backend prod config |
| `/etc/systemd/system/tinyurl-backend.service` | Backend unit |
| `/opt/tinyurl/frontend/app/` | Frontend static + `server.js` + PM2 |
| `/etc/nginx/sites-available/slashurl` | Nginx vhost |
| `/etc/letsencrypt/live/slashurl.com/` | TLS certs (certbot) |
| `/var/log/app/backend.log` | Backend logs |
| `/etc/mysql/mysql.conf.d/zz-lowmem.cnf` | MySQL low-mem |
| Swap `/swapfile` 2G | Required on ≤1GB RAM |

## Fresh setup (agent checklist)

1. **Create Lightsail** Ubuntu 24.04, ≥1GB RAM recommended (512MB works with heavy swap tuning; prefer 1–2GB).
2. **Attach static IPv4**; open Lightsail firewall **22, 80, 443**.
3. **SSH** as `ubuntu` (see `ssh-config.example`).
4. Run `scripts/bootstrap-ubuntu.sh` (or follow steps below).
5. Create MySQL DB/user (empty schema is fine — no prod dump shipped):
   ```bash
   sudo mysql -e "CREATE DATABASE tiny_url CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'tiny'@'localhost' IDENTIFIED BY '<PASSWORD>';
   GRANT ALL ON tiny_url.* TO 'tiny'@'localhost'; FLUSH PRIVILEGES;"
   ```
6. Deploy backend JAR + `application-prod.properties` (from `backend/application-prod.properties.example`, set password).
7. Install systemd unit from `systemd/tinyurl-backend.service`; `daemon-reload && enable --now tinyurl-backend`.
8. Deploy frontend build to `/opt/tinyurl/frontend/app`, copy `frontend/server.js`, `ecosystem.config.js`, `npm i --omit=dev`, `pm2 start && pm2 save && pm2 startup`.
9. Install nginx site from `nginx/slashurl.conf`; enable site; obtain certs:
   ```bash
   sudo certbot --nginx -d slashurl.com -d www.slashurl.com -d apis.slashurl.com
   ```
10. DNS / Cloudflare:
    - Apex `slashurl.com` A → static IP (DNS only or proxied)
    - `www` / `apis` A → same IP (**proxied OK**; nslookup will show Cloudflare IPs)
11. Verify: `curl -I https://slashurl.com` and `curl -X POST 'https://apis.slashurl.com/new?originalUrl=https://example.com'`.

## Low-memory notes (nano / $5 plan)

- 2G swap (`swappiness=10`)
- JVM: `-Xms64m -Xmx192m -XX:+UseSerialGC` (see systemd unit)
- MySQL: `deploy/mysql/zz-lowmem.cnf`
- Redis: `deploy/redis/redis-lowmem.conf.snippet`
- Mask `unattended-upgrades` during first boot if it fights Java for RAM

## Secrets

Never commit real DB passwords. Use env or local `application-prod.properties` on the server only.
Template: `backend/application-prod.properties.example`.

## Shutdown / teardown

Prod was taken offline intentionally. To stop billing: delete the Lightsail instance and release the static IP in AWS console; leave Cloudflare DNS records or point them elsewhere.

## What is NOT stored here

- MySQL `tiny_url` row data
- Redis cache / click counters
- Let's Encrypt private keys (re-issue with certbot on next deploy)
- Live `application-prod.properties` with password
