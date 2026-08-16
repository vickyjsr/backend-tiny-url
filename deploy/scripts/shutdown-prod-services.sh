#!/usr/bin/env bash
# Gracefully stop SlashURL production services (keeps files on disk).
set -euo pipefail
pm2 stop tinyurl-frontend 2>/dev/null || true
pm2 save 2>/dev/null || true
sudo systemctl stop tinyurl-backend nginx 2>/dev/null || true
sudo systemctl stop mysql redis-server 2>/dev/null || true
sudo systemctl disable tinyurl-backend 2>/dev/null || true
echo "Services stopped. Instance still running — delete Lightsail + static IP to stop billing."
