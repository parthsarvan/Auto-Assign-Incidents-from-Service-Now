# InciTeam iOS

Native iOS companion app workspace for InciTeam.

## Role

The iOS app is intended for post-setup operational use. Organization setup, ServiceNow connection, CI-user mapping, shifts, geos, and other admin-heavy workflows stay in the web app.

## Initial Demo Scope

- Login with existing InciTeam credentials.
- View current team, geo, and shift.
- View roster.
- View summary alerts.
- View latest poll and assignment logs.
- Trigger manual poll now for authorized users.
- Add quick break/leave entries.

## Planned Later

- Apple Push Notification service for incident assignment alerts.
- Notification preferences.
- TestFlight distribution.

## Xcode Setup

Create the SwiftUI project inside this directory after installing Xcode.

Suggested settings:

- Product name: `InciTeam`
- Interface: `SwiftUI`
- Language: `Swift`
- Bundle identifier: `com.inciteam.mobile`
- Minimum iOS version: latest practical simulator/device target available in your Xcode install

## Backend

The app should use the same backend as the web app.

Production base URL:

```text
https://www.inciteam.com
```

Local development can point to:

```text
http://localhost:8080
```
