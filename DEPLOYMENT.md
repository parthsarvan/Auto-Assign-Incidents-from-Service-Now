# InciTeam Deployment Guide

This is the recommended first hosted shape for InciTeam:

1. One shared Spring Boot backend
2. One shared PostgreSQL database
3. One built React frontend served through Nginx

For the first production version, the simplest path is:

- AWS EC2 for backend + frontend static files
- AWS RDS PostgreSQL for the database
- Nginx as reverse proxy

## Recommended Architecture

Use one Linux EC2 instance for the app server:

- Nginx listens on `80` and `443`
- React production build is served by Nginx
- Spring Boot runs on `localhost:8080`
- Nginx proxies `/api/*` to the backend

Use one PostgreSQL database:

- local dev: local Postgres
- hosted: RDS PostgreSQL

This keeps the frontend and backend on the same public origin, which is the cleanest production setup for the current codebase.

## Production Environment Variables

### Backend

Set these on the server for the Spring Boot app:

```env
DB_URL=jdbc:postgresql://<rds-host>:5432/incteam
DB_USERNAME=<db-user>
DB_PASSWORD=<db-password>

JPA_DDL_AUTO=validate
JPA_SHOW_SQL=false
JPA_FORMAT_SQL=false

FLYWAY_ENABLED=true
FLYWAY_LOCATIONS=classpath:db/migration
FLYWAY_BASELINE_ON_MIGRATE=true
FLYWAY_BASELINE_VERSION=1
FLYWAY_BASELINE_DESCRIPTION=Pre-Flyway Hibernate Schema

JWT_SECRET=<long-base64-secret>
JWT_EXPIRATION_MS=3600000

CORS_ALLOWED_ORIGINS=https://your-domain.com
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*

SERVICENOW_INSTANCE_URL=https://your-instance.service-now.com
SERVICENOW_USERNAME=<servicenow-username>
SERVICENOW_PASSWORD=<servicenow-password>
SERVICENOW_POLL_INTERVAL_MS=300000
SERVICENOW_LOG_RETENTION=100
SERVICENOW_ASSIGNMENT_ENABLED=true
```

### Frontend

For same-origin hosting behind Nginx, leave the frontend API base blank:

```env
REACT_APP_API_BASE_URL=
```

That lets the frontend call `/api/...` on the same domain it was loaded from.

If you host frontend and backend on separate origins later, set:

```env
REACT_APP_API_BASE_URL=https://api.your-domain.com
```

## Build Commands

### Backend

```bash
cd backend/backend
./mvnw clean package
```

This produces a runnable JAR in `target/`.

### Frontend

```bash
cd frontend/frontend
npm install
npm run build
```

This produces the production frontend in `build/`.

## EC2 Deployment Flow

### 1. Provision infrastructure

- Create one EC2 instance
- Create one PostgreSQL RDS instance
- Open inbound `80` and `443` to the public
- Keep backend port `8080` private if possible

### 2. Install runtime packages on EC2

Install:

- Java 17
- Nginx
- Node only if you plan to build on the server

### 3. Copy the app artifacts

Backend:

- copy the built Spring Boot JAR to the server

Frontend:

- copy the React `build/` output to an Nginx-served directory such as:
  - `/var/www/incteam`

### 4. Run backend as a service

Use a systemd service for the Spring Boot app.

Example:

```ini
[Unit]
Description=InciTeam Backend
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/incteam
EnvironmentFile=/opt/incteam/backend.env
ExecStart=/usr/bin/java -jar /opt/incteam/backend.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable incteam
sudo systemctl start incteam
```

## Nginx Reverse Proxy

Recommended Nginx shape:

- serve frontend files from `/var/www/incteam`
- proxy `/api/` to `http://127.0.0.1:8080/api/`
- send all non-file routes to `index.html`

Example:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /var/www/incteam;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri /index.html;
    }
}
```

Add TLS with Let’s Encrypt after the app is reachable over HTTP.

## First Production Startup Checklist

Before opening access to real users:

1. Confirm Flyway runs cleanly
2. Confirm the backend starts with `JPA_DDL_AUTO=validate`
3. Confirm the frontend can sign in through the public domain
4. Confirm ServiceNow health check passes
5. Confirm first admin can:
   - sign in
   - create/copy a team
   - invite a user
   - see logs and diagnostics

## Notes For InciTeam Specifically

- ServiceNow configuration is currently shared across the whole backend/app
- orgs and teams are product-level scoped in the database
- Flyway is now the source of truth for schema changes
- local defaults are still fine for development, but production should use explicit env vars

## Next Production-Readiness Items After This

After first deployment planning, the next strong steps are:

1. add a small systemd/env bootstrap script
2. add SSL/HTTPS setup notes
3. add backup/restore notes for PostgreSQL
4. optionally add Docker later if you want repeatable server packaging
