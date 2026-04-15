export function isOrgAdmin(user) {
  return user?.role === 'Admin';
}

const TEAM_MANAGER_PATHS = new Set([
  '/summary',
  '/setup',
  '/geos',
  '/shifts',
  '/team-members',
  '/configuration-items',
  '/geo-shift-mappings',
  '/ci-user-mappings',
  '/schedules',
  '/leaves',
  '/breaks',
]);

const ORG_ADMIN_PATHS = new Set([
  '/teams',
  '/user-access',
]);

export function getCurrentTeamRole(user) {
  return user?.workspace?.teamRole || null;
}

export function canManageCurrentTeam(user) {
  if (isOrgAdmin(user)) {
    return true;
  }
  const teamRole = getCurrentTeamRole(user);
  return teamRole === 'TEAM_ADMIN' || teamRole === 'MANAGER';
}

export function isCurrentTeamAdmin(user) {
  if (isOrgAdmin(user)) {
    return true;
  }
  return getCurrentTeamRole(user) === 'TEAM_ADMIN';
}

export function canAccessPath(user, pathname) {
  if (!user || !pathname) {
    return false;
  }
  if (ORG_ADMIN_PATHS.has(pathname)) {
    return isOrgAdmin(user);
  }
  if (TEAM_MANAGER_PATHS.has(pathname)) {
    return canManageCurrentTeam(user);
  }
  return true;
}

export function getDefaultRouteForUser(user) {
  return canManageCurrentTeam(user) ? '/summary' : '/';
}

export function resolveLandingRoute(user, requestedPath) {
  if (requestedPath && canAccessPath(user, requestedPath)) {
    return requestedPath;
  }
  return getDefaultRouteForUser(user);
}
