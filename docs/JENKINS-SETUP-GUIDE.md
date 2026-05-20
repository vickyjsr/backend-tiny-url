# Jenkins Setup Guide: From Server to Deploy Pipeline

This document describes the full process from installing Jenkins on a server to having a working Deploy/Revert pipeline with Nginx, DNS, and the TinyURL backend service.

---

## Table of Contents

1. [Server and Jenkins Installation](#1-server-and-jenkins-installation)
2. [Change Jenkins Port (e.g. 9091)](#2-change-jenkins-port-eg-9091)
3. [Nginx Reverse Proxy for Jenkins](#3-nginx-reverse-proxy-for-jenkins)
4. [DNS for Jenkins URL](#4-dns-for-jenkins-url)
5. [Jenkins Security (Require Login)](#5-jenkins-security-require-login)
6. [Optional: Google Login](#6-optional-google-login)
7. [Create the Pipeline Job](#7-create-the-pipeline-job)
8. [Deploy Directory and Permissions](#8-deploy-directory-and-permissions)
9. [Sudoers for Service Restart and Health Check](#9-sudoers-for-service-restart-and-health-check)
10. [TinyURL Backend Systemd Service](#10-tinyurl-backend-systemd-service)
11. [Pipeline Behaviour (Deploy / Revert)](#11-pipeline-behaviour-deploy--revert)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Server and Jenkins Installation

**Assumptions:** Ubuntu server (e.g. EC2), SSH access, domain `slashurl.com` and server IP (e.g. `13.205.140.204`).

### Install Jenkins (Ubuntu/Debian)

```bash
# Add Jenkins repo and install
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt-get update
sudo apt-get install -y jenkins

# Jenkins runs as user `jenkins`, typically on port 8080 by default
```

### Verify Jenkins is running

```bash
sudo systemctl status jenkins
# Open http://<server-ip>:8080 in browser for initial setup (unlock with password from /var/lib/jenkins/secrets/initialAdminPassword)
```

---

## 2. Change Jenkins Port (e.g. 9091)

To avoid conflicts (e.g. with another app on 8080), run Jenkins on a different port (e.g. **9091**).

### Using systemd override

```bash
sudo mkdir -p /etc/systemd/system/jenkins.service.d
sudo vim /etc/systemd/system/jenkins.service.d/override.conf
```

Add (or edit to):

```ini
[Service]
Environment="JENKINS_PORT=9091"
```

Then:

```bash
sudo systemctl daemon-reload
sudo systemctl restart jenkins
```

### Verify

```bash
sudo ss -tlnp | grep 9091
# Jenkins should listen on 9091
```

---

## 3. Nginx Reverse Proxy for Jenkins

Expose Jenkins over HTTPS at e.g. **prod-jenkins.slashurl.com** using Nginx and Certbot.

### Add Jenkins server block

Edit your Nginx site config (e.g. `/etc/nginx/sites-available/slashurl`):

**HTTPS server (with your other server blocks):**

```nginx
# Jenkins UI - HTTPS
server {
    server_name prod-jenkins.slashurl.com;

    location / {
        proxy_pass http://localhost:9091;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;
    }

    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/slashurl.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/slashurl.com/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;
}
```

**HTTP redirect (so http → https):**

```nginx
# Jenkins - HTTP redirect to HTTPS
server {
    listen 80;
    server_name prod-jenkins.slashurl.com;
    return 301 https://$server_name$request_uri;
}
```

### Test and reload Nginx

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 4. DNS for Jenkins URL

So that **prod-jenkins.slashurl.com** resolves to your server:

- **A record:** Name `prod-jenkins`, Value `13.205.140.204` (your server IP), or  
- **CNAME:** Name `prod-jenkins`, Target `slashurl.com` (or the hostname that already has the A record).

Create this in the same place where **slashurl.com** is managed (e.g. Cloudflare, Route53, Namecheap). After propagation:

```bash
dig prod-jenkins.slashurl.com +short
# Should print your server IP
```

Then open **https://prod-jenkins.slashurl.com** in a browser.

### Set Jenkins root URL

In Jenkins: **Manage Jenkins** → **System** → **Jenkins URL**: set to `https://prod-jenkins.slashurl.com/` and save.

---

## 5. Jenkins Security (Require Login)

By default, anonymous users may see the dashboard. To require login for everything:

1. **Manage Jenkins** → **Security** (or **Configure Global Security**).
2. **Security Realm:** keep your current realm (e.g. Jenkins’ own user database or Google OAuth).
3. **Authorization:** choose **Matrix-based security** (or **Project-based Matrix**).
4. In the matrix, find **Anonymous** and **uncheck every permission** (especially Read, View, Job/Read).
5. Grant **Authenticated** (or specific users/groups) the permissions you need.
6. **Save.**

After this, unauthenticated users only see the login page.

---

## 6. Optional: Google Login

To allow “Sign in with Google”:

1. **Install plugin:** **Manage Jenkins** → **Plugins** → **Available** → search **Google Login** → Install, restart if needed.
2. **Google Cloud Console:** Create (or select) a project → **APIs & Services** → **Credentials** → **Create credentials** → **OAuth client ID** → Application type **Web application** → Authorized redirect URI: `https://prod-jenkins.slashurl.com/securityRealm/finishLogin` → Create, copy **Client ID** and **Client secret**.
3. **Jenkins:** **Manage Jenkins** → **Security** → **Security Realm** → **Google OAuth 2.0** → paste Client ID and Client secret → optionally set domain restriction → **Save.**

Ensure **Jenkins URL** is set to `https://prod-jenkins.slashurl.com/` (see section 4).

---

## 7. Create the Pipeline Job

### Option A: Pipeline script from SCM (recommended)

1. **New Item** → name (e.g. `tiny-url`) → **Pipeline** → OK.
2. **Pipeline** section:
   - **Definition:** Pipeline script from SCM
   - **SCM:** Git
   - **Repository URL:** `https://github.com/vickyjsr/backend-tiny-url.git` (or your repo)
   - **Branch:** `*/main`
   - **Script Path:** `Jenkinsfile`
3. Save. Jenkins will use the **Jenkinsfile** from the repo; no need to paste script.

### Option B: Standalone Pipeline (script in job)

1. **New Item** → **Pipeline** → OK.
2. **Pipeline** → **Definition:** Pipeline script → paste the contents of **Jenkinsfile**.
3. Under **Environment** (or job config), set **GIT_URL** to your repo URL so the Clone stage can run `git clone`.
4. Save.

After the first run (or after saving with parameters), use **Build with Parameters** to choose **Deploy** or **Revert** and set **BRANCH** when deploying.

---

## 8. Deploy Directory and Permissions

The pipeline copies the built JAR to **/var/www/build** and keeps a backup in **/var/www/build/previous** for Revert. The **jenkins** user must own these directories.

On the server (one-time):

```bash
sudo mkdir -p /var/www/build /var/www/build/previous
sudo chown -R jenkins:jenkins /var/www
sudo chmod -R 755 /var/www
```

Verify:

```bash
sudo -u jenkins touch /var/www/build/.test && sudo -u jenkins rm /var/www/build/.test && echo "OK"
```

---

## 9. Sudoers for Service Restart and Health Check

The pipeline runs as user **jenkins** and must restart the TinyURL backend service and check its status without a password.

```bash
sudo visudo
```

Add these lines (adjust service name if different):

```
jenkins ALL=(ALL) NOPASSWD: /bin/systemctl restart tinyurl-backend.service
jenkins ALL=(ALL) NOPASSWD: /bin/systemctl is-active tinyurl-backend.service
```

Save and exit. Test:

```bash
sudo -u jenkins sudo systemctl restart tinyurl-backend.service
sudo -u jenkins sudo systemctl is-active tinyurl-backend.service
# Should print "active" and not ask for password
```

---

## 10. TinyURL Backend Systemd Service

The pipeline deploys the JAR to **/var/www/build** and runs **systemctl restart tinyurl-backend.service**. You need a systemd unit that runs that JAR.

Example **/etc/systemd/system/tinyurl-backend.service**:

```ini
[Unit]
Description=TinyURL Backend Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/var/www/build
ExecStart=/usr/bin/java -jar /var/www/build/Tiny-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:/var/www/build/application-prod.properties
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Adjust **User**, **jar path**, and **spring config** as needed. Then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable tinyurl-backend.service
sudo systemctl start tinyurl-backend.service
```

---

## 11. Pipeline Behaviour (Deploy / Revert)

- **Parameters:** **ACTION** (Deploy / Revert), **BRANCH** (used only for Deploy; default `main`).
- **Validate:** Only for Deploy; checks BRANCH is non-empty and valid.
- **Clone:** Only for Deploy; clones the repo for the chosen **BRANCH**.
- **Build:** Only for Deploy; runs `./gradlew clean bootJar -x test` and checks that a JAR exists in `build/libs/`.
- **Deploy:** Only when ACTION = Deploy. Archives JAR, backs up current JAR to **/var/www/build/previous/previous.jar**, copies new JAR to **/var/www/build**, restarts **tinyurl-backend.service**, then:
  - Waits up to 60s (poll every 5s) for the service to become active.
  - Runs a **45s soak**: sleeps 45s, then checks again. If the service is no longer active, the pipeline fails (catches services that die after 30–60s).
- **Revert:** Only when ACTION = Revert. Copies **previous.jar** back to **/var/www/build**, restarts the service, and runs the same startup + soak check.

**Concurrency:** The pipeline uses **disableConcurrentBuilds()** so only one run (Deploy or Revert) runs at a time.

---

## 12. Troubleshooting

| Issue | What to check |
|-------|----------------|
| **Permission denied** copying JAR to `/var/www/build` | Run `sudo chown -R jenkins:jenkins /var/www` and ensure no step uses `sudo` for the copy (see section 8). |
| **sudo: password required** during pipeline | Add NOPASSWD for `systemctl restart` and `systemctl is-active` in sudoers (section 9). |
| **Deploy/Revert stage skipped** | Fix the stage that failed earlier (e.g. Build or a `sh` step). Check logs for the first failure. |
| **prod-jenkins.slashurl.com not loading** | Add DNS A or CNAME (section 4). Test with `dig prod-jenkins.slashurl.com +short`. |
| **404 on http://prod-jenkins.slashurl.com** | Add the HTTP server block that redirects to HTTPS (section 3). |
| **Service dies after 30–60s** | Pipeline will fail at the soak check. Fix the app or JVM (e.g. config, memory, DB). Soak duration is the `sleep 45` in the Jenkinsfile. |
| **findFiles / NoSuchMethodError** | Do not use `findFiles`; the guide uses a shell check for JARs in `build/libs/` (no extra plugin). |

---

## Summary Checklist

- [ ] Jenkins installed and running (port 9091 via systemd override).
- [ ] Nginx configured for prod-jenkins.slashurl.com (HTTPS + HTTP redirect).
- [ ] DNS A or CNAME for prod-jenkins.slashurl.com.
- [ ] Jenkins URL set to https://prod-jenkins.slashurl.com/
- [ ] Security: Matrix auth, Anonymous has no permissions.
- [ ] (Optional) Google Login plugin and OAuth client configured.
- [ ] Pipeline job created (from SCM or with GIT_URL).
- [ ] /var/www/build and /var/www/build/previous exist, owned by jenkins.
- [ ] Sudoers: jenkins NOPASSWD for systemctl restart and systemctl is-active (tinyurl-backend.service).
- [ ] tinyurl-backend.service unit exists and runs the JAR from /var/www/build.

After this, **Build with Parameters** → choose **Deploy** (and branch) or **Revert** → Build.
