# Mobile API Notes

This document tracks backend endpoints the iOS and Android companion apps should use.

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

Implemented flow:

1. iOS app requests notification permission.
2. iOS app receives APNs device token.
3. Android app requests notification permission and receives an FCM registration token when Firebase config is present.
4. Mobile app sends device token to backend for the logged-in user.
5. Backend stores token, platform, environment, and notification preferences.
6. Backend sends APNs or FCM notification after a successful incident assignment.

Endpoint:

```text
POST /api/mobile/device-token
POST /api/mobile/device-token/unregister
```

Request:

```json
{
  "deviceToken": "provider-token",
  "platform": "ios | android",
  "environment": "development | production"
}
```

Incident assignment payload fields:

- `incidentNumber`
- `title`
- `priority`
- `ci`
