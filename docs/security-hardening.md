# InciTeam Security Hardening

This app now enforces auth throttling, safer auth responses, stronger signup validation,
security headers, request-size limits, and environment-only secrets in the backend.

Production still needs the infrastructure layer to match the app layer.

## Required Production Environment

Set these on the backend host before deploying:

- `JWT_SECRET`: Base64 secret that decodes to at least 64 bytes.
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `CORS_ALLOWED_ORIGINS`: for example `https://www.inciteam.com`
- ServiceNow, APNs, FCM, and email credentials only when those integrations are enabled.

Generate a JWT secret with:

```sh
openssl rand -base64 64
```

## Recommended Edge / Nginx Controls

Apply these in the public reverse proxy or AWS edge layer:

- Redirect all HTTP traffic to HTTPS.
- Keep backend ports closed to the public internet; expose only the reverse proxy.
- Set `client_max_body_size 1m`.
- Rate-limit `/api/auth/login`, `/api/auth/signup`, and `/api/auth/organization-discovery`.
- Add security headers for the static web app:

```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
add_header X-Frame-Options "DENY" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "camera=(), geolocation=(), microphone=(), payment=(), usb=()" always;
add_header Content-Security-Policy "default-src 'self'; connect-src 'self'; img-src 'self' data: blob:; script-src 'self'; style-src 'self' 'unsafe-inline'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'; upgrade-insecure-requests" always;
```

## Recommended AWS Controls

- Put CloudFront + AWS WAF in front of the app when you are ready for production users.
- Enable AWS managed rule groups and a rate-based rule for public auth endpoints.
- Restrict EC2 security groups to ports 80/443 publicly and SSH only from trusted IPs.
- Move PostgreSQL to RDS when real customer data begins, with automated backups enabled.
- Keep OS, Java, Nginx, and dependency patches current.
- Rotate any credential that was ever committed, even if it has now been removed.
