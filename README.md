# InciTeam

InciTeam is a shared incident-assignment and team-operations platform built as a PagerDuty-style alternative for organizations that want team-aware setup, schedules, ServiceNow-driven assignment, and operational visibility.

Today the product includes:
- organization and team workspaces
- setup wizard for team configuration
- ServiceNow polling and assignment
- team-scoped logs and diagnostics
- invite-based team joining
- schedules, leaves, and breaks
- role-aware access control

## Repo Layout

- `backend/backend`: Spring Boot API
- `frontend/frontend`: React app

## Local Development

### Backend

From `backend/backend`:

```bash
./mvnw spring-boot:run
```

The backend reads runtime configuration from environment variables, with local defaults defined in `application.properties`.

Flyway now manages schema evolution. On an existing local database, the app will baseline the current schema once and then apply newer migrations from `backend/backend/src/main/resources/db/migration`.

### Frontend

From `frontend/frontend`:

```bash
npm start
```

By default the frontend talks to the same origin. For local split frontend/backend development, set:

```bash
REACT_APP_API_BASE_URL=http://localhost:8080
```

## Environment Variables

### Backend

These are the main backend runtime variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JPA_DDL_AUTO`
- `JPA_SHOW_SQL`
- `JPA_FORMAT_SQL`
- `FLYWAY_ENABLED`
- `FLYWAY_LOCATIONS`
- `FLYWAY_BASELINE_ON_MIGRATE`
- `FLYWAY_BASELINE_VERSION`
- `FLYWAY_BASELINE_DESCRIPTION`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `CORS_ALLOWED_ORIGINS`
- `CORS_ALLOWED_METHODS`
- `CORS_ALLOWED_HEADERS`
- `SERVICENOW_INSTANCE_URL`
- `SERVICENOW_USERNAME`
- `SERVICENOW_PASSWORD`
- `SERVICENOW_POLL_INTERVAL_MS`
- `SERVICENOW_LOG_RETENTION`
- `SERVICENOW_ASSIGNMENT_ENABLED`

See [backend/backend/.env.example](backend/backend/.env.example) for a full example.

### Frontend

Main frontend runtime variable:

- `REACT_APP_API_BASE_URL`

See [frontend/frontend/.env.example](frontend/frontend/.env.example) for an example.

## Shared Deployment Direction

The current recommended deployment model is:

1. Deploy one shared backend
2. Deploy one shared PostgreSQL database
3. Host the frontend as a client of that shared backend

At the moment, ServiceNow configuration is shared at the backend/app level, while team data and operations are team-scoped inside the product.

See [DEPLOYMENT.md](DEPLOYMENT.md) for the recommended first hosted setup using Spring Boot, PostgreSQL, React, Nginx, and AWS-style infrastructure.

## Verification

Backend compile:

```bash
cd backend/backend
./mvnw -q -DskipTests compile
```

Frontend production build:

```bash
cd frontend/frontend
npm run build
```
