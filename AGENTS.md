# InciTeam Agent Notes

This repository is the InciTeam monorepo.

## Project Shape

- `backend/backend`: Spring Boot API and polling/assignment logic.
- `frontend/frontend`: React web app for admin setup and operations.
- `ios`: native iOS companion app workspace.
- `docs`: shared product and API notes.

## Product Direction

The web app remains the main administration and setup platform. The iOS app should be a lightweight post-setup operations companion, not a duplicate of every web setup screen.

Mobile-first scope:

- Login with existing InciTeam account.
- Show current geo and shift.
- Show roster and schedule-aware availability.
- Show summary alerts and latest poll status.
- Show assignment logs and assignment outcomes.
- Trigger manual poll now when authorized.
- Add quick break/leave actions.
- Later: Apple Push Notification service for assignment and coverage alerts.

## Development Notes

- Keep backend API changes backward compatible with the web app.
- If adding mobile-only API endpoints, document them in `docs/mobile-api.md`.
- Keep generated build artifacts out of Git.
- Do not rename `backend/backend` or `frontend/frontend` without updating deployment scripts and docs.
