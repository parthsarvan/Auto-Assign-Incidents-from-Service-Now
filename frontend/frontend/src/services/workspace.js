import { authHeader, getCurrentUser, setCurrentUser } from './auth';
import { createApiClient } from './api';

const api = createApiClient('/workspace');

export async function fetchWorkspaceTeams() {
  const response = await api.get('/teams', { headers: authHeader() });
  return response.data;
}

export async function createWorkspaceTeam(name, description, copyFromTeamId = null) {
  const response = await api.post('/teams', {
    name: name.trim(),
    description: description.trim(),
    copyFromTeamId,
  }, { headers: authHeader() });
  updateWorkspace(response.data);
  return response.data;
}

export async function switchWorkspaceTeam(teamId) {
  const response = await api.post('/switch-team', { teamId }, { headers: authHeader() });
  updateWorkspace(response.data);
  return response.data;
}

export async function regenerateWorkspaceInvite(teamId) {
  const response = await api.post(`/teams/${teamId}/regenerate-invite`, {}, { headers: authHeader() });
  return response.data;
}

function updateWorkspace(workspace) {
  const user = getCurrentUser();
  if (!user) {
    return;
  }
  setCurrentUser({
    ...user,
    workspace,
  });
}
