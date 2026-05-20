# Mobile API Notes

This document tracks backend endpoints the iOS companion app should use.

## Base URLs

Production:

```text
https://www.inciteam.com
```

Local backend:

```text
http://localhost:8080
```

## Target Mobile Screens

- Login
- Current geo/shift
- Roster
- Summary alerts
- Latest polling status
- Assignment logs
- Manual poll now
- Quick break/leave

## API Inventory

To be filled as the iOS app is implemented. Prefer existing web APIs first. Add mobile-specific endpoints only when existing endpoints are too broad, inefficient, or web-shaped.

## Push Notifications

Planned flow:

1. iOS app requests notification permission.
2. iOS app receives APNs device token.
3. iOS app sends device token to backend for the logged-in user.
4. Backend stores token and notification preferences.
5. Backend sends APNs notification after assignment, skipped assignment, unsupported CI, no coverage, or failed poll events.
