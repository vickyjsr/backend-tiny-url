#!/usr/bin/env bash
# Bootstrap SlashURL stack on Ubuntu 24.04 (Lightsail).
# Run as ubuntu with sudo. Does NOT restore MySQL/Redis data.
set -euo pipefail

export DEBIAN_FRONTEND=noninteractive
REPO_DEPLOY_DIR="${1:-}"

if [[ -z "${REPO_DEPLOY_DIR}" || ! -d "${REPO_DEPLOY_DIR}" ]]; then
  echo "Usage: $0 /path/to/TinyUrlBackend/deploy"
  exit 1
fi

echo "==> Swap (2G) if missing"
if ! swapon --show | grep -q .; then
  sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  grep -q '/swapfile' /etc/fstab || echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
  echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-swappiness.conf
  sudo sysctl -p /etc/sysctl.d/99-swappiness.conf
fi

echo "==> Packages"
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jre-headless nginx mysql-server redis-server \
  certbot python3-certbot-nginx ufw curl ca-certificates gnupg

if ! command -v node >/dev/null; then
  curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
  sudo apt-get install -y nodejs
fi
sudo npm install -g pm2

echo "==> MySQL / Redis low-mem"
sudo cp "${REPO_DEPLOY_DIR}/mysql/zz-lowmem.cnf" /etc/mysql/mysql.conf.d/zz-lowmem.cnf
# Redis tweaks
if ! sudo grep -q '^maxmemory 32mb' /etc/redis/redis.conf; then
  echo 'maxmemory 32mb' | sudo tee -a /etc/redis/redis.conf
  echo 'maxmemory-policy allkeys-lru' | sudo tee -a /etc/redis/redis.conf
fi
sudo sed -i 's/^bind .*/bind 127.0.0.1 -::1/' /etc/redis/redis.conf || true
sudo systemctl enable mysql redis-server nginx
sudo systemctl restart mysql redis-server

echo "==> Dirs"
sudo mkdir -p /var/www/build /var/log/app /opt/tinyurl/frontend/{app,logs}
sudo chown -R ubuntu:ubuntu /opt/tinyurl

echo "==> Systemd backend unit"
sudo cp "${REPO_DEPLOY_DIR}/systemd/tinyurl-backend.service" /etc/systemd/system/tinyurl-backend.service
sudo systemctl daemon-reload
sudo systemctl enable tinyurl-backend
# Do not start until JAR + properties exist

echo "==> Nginx site (HTTP first; certbot will add SSL)"
# Install a HTTP-only bootstrap if needed; for full SSL copy after certs:
sudo cp "${REPO_DEPLOY_DIR}/nginx/slashurl.conf" /etc/nginx/sites-available/slashurl
sudo ln -sfn /etc/nginx/sites-available/slashurl /etc/nginx/sites-enabled/slashurl
sudo rm -f /etc/nginx/sites-enabled/default
# If certs do not exist yet, certbot step is required before nginx -t succeeds on SSL lines.
if [[ ! -d /etc/letsencrypt/live/slashurl.com ]]; then
  echo "NOTE: TLS certs missing. After DNS points here, run:"
  echo "  sudo certbot --nginx -d slashurl.com -d www.slashurl.com -d apis.slashurl.com"
fi

echo "==> UFW"
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
echo y | sudo ufw enable || true

echo "==> Next steps"
cat <<'NEXT'
1) Create MySQL DB/user tiny / password
2) Copy JAR → /var/www/build/Tiny-0.0.1-SNAPSHOT.jar
3) Copy properties from deploy/backend/application-prod.properties.example
4) sudo systemctl start tinyurl-backend
5) Deploy frontend build + server.js + ecosystem; pm2 start; pm2 save; pm2 startup
6) certbot --nginx ...
7) Point DNS / Cloudflare origin to this public IP
NEXT
