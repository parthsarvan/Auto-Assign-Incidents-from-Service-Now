# Auto-Assign-Incidents-from-Service-Now
Whenever an incident is created, it will be auto assigned to user based on CI-User mapping in DB by checking if user is on leave or not. If multiple user to one CI mapping then assign incident in round robin fashion. Also detect the Geo and shift timing and then assign incident to user available on shift.
