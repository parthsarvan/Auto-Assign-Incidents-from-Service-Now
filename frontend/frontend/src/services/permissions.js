export function isOrgAdmin(user) {
  return user?.role === 'Admin';
}

export const INCOMPLETE_SETUP_ALLOWED_PATHS = new Set([
  '/setup',
  '/teams',
  '/user-access',
]);

const TEAM_MANAGER_PATHS = new Set([
  '/summary',
  '/setup',
  '/geos',
  '/shifts',
]);

const TEAM_VIEWER_PATHS = new Set([
  '/team-members',
  '/geo-shift-mappings',
  '/ci-user-mappings',
  '/team-members',
  '/ci-user-mappings',
  '/schedules',
  '/leaves',
  '/breaks',
  '/logs',
  '/assignment-diagnostics',
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

export function canViewCurrentTeam(user) {
  return Boolean(user);
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
  if (TEAM_VIEWER_PATHS.has(pathname)) {
    return canViewCurrentTeam(user);
  }
  return true;
}

export function getDefaultRouteForUser(user) {
  return canManageCurrentTeam(user) ? '/summary' : '/dashboard';
}

export function resolveLandingRoute(user, requestedPath) {
  if (requestedPath && canAccessPath(user, requestedPath)) {
    return requestedPath;
  }
  return getDefaultRouteForUser(user);
}
