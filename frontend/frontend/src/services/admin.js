import { authHeader } from './auth';
import { createApiClient } from './api';

const api = createApiClient();

function withAuth() {
  return { headers: authHeader() };
}

export async function fetchGeos() {
  const response = await api.get('/geos', withAuth());
  return response.data;
}

export async function createGeo(payload) {
  const response = await api.post('/geos', payload, withAuth());
  return response.data;
}

export async function updateGeo(id, payload) {
  const response = await api.put(`/geos/${id}`, payload, withAuth());
  return response.data;
}

export async function deleteGeo(id) {
  return api.delete(`/geos/${id}`, withAuth());
}

export async function fetchShifts() {
  const response = await api.get('/shifts', withAuth());
  return response.data;
}

export async function createShift(payload) {
  const response = await api.post('/shifts', payload, withAuth());
  return response.data;
}

export async function updateShift(id, payload) {
  const response = await api.put(`/shifts/${id}`, payload, withAuth());
  return response.data;
}

export async function deleteShift(id) {
  return api.delete(`/shifts/${id}`, withAuth());
}

export async function fetchTeamMembers() {
  const response = await api.get('/team-members', withAuth());
  return response.data;
}

export async function createTeamMember(payload) {
  const response = await api.post('/team-members', payload, withAuth());
  return response.data;
}

export async function fetchJoinedTeamUsers() {
  const response = await api.get('/team-members/joined-users', withAuth());
  return response.data;
}

export async function updateTeamMember(id, payload) {
  const response = await api.put(`/team-members/${id}`, payload, withAuth());
  return response.data;
}

export async function deleteTeamMember(id) {
  return api.delete(`/team-members/${id}`, withAuth());
}

export async function fetchConfigurationItems() {
  const response = await api.get('/configuration-items', withAuth());
  return response.data;
}

export async function createConfigurationItem(payload) {
  const response = await api.post('/configuration-items', payload, withAuth());
  return response.data;
}

export async function updateConfigurationItem(id, payload) {
  const response = await api.put(`/configuration-items/${id}`, payload, withAuth());
  return response.data;
}

export async function deleteConfigurationItem(id) {
  return api.delete(`/configuration-items/${id}`, withAuth());
}

export async function fetchGeoShiftMappings() {
  const response = await api.get('/geo-shift-mappings', withAuth());
  return response.data;
}

export async function createGeoShiftMapping(payload) {
  const response = await api.post('/geo-shift-mappings', payload, withAuth());
  return response.data;
}

export async function updateGeoShiftMapping(id, payload) {
  const response = await api.put(`/geo-shift-mappings/${id}`, payload, withAuth());
  return response.data;
}

export async function deleteGeoShiftMapping(id) {
  return api.delete(`/geo-shift-mappings/${id}`, withAuth());
}

export async function fetchCiUserMappings() {
  const response = await api.get('/ci-user-mappings', withAuth());
  return response.data;
}

export async function createCiUserMapping(payload) {
  const response = await api.post('/ci-user-mappings', payload, withAuth());
  return response.data;
}

export async function updateCiUserMapping(id, payload) {
  const response = await api.put(`/ci-user-mappings/${id}`, payload, withAuth());
  return response.data;
}

export async function replaceCiUserMappingsForCi(payload) {
  const response = await api.post('/ci-user-mappings/bulk', payload, withAuth());
  return response.data;
}

export async function deleteCiUserMapping(id) {
  return api.delete(`/ci-user-mappings/${id}`, withAuth());
}

export async function fetchSchedules() {
  const response = await api.get('/team-member-schedules', withAuth());
  return response.data;
}

export async function createSchedule(payload) {
  const response = await api.post('/team-member-schedules', payload, withAuth());
  return response.data;
}

export async function updateSchedule(id, payload) {
  const response = await api.put(`/team-member-schedules/${id}`, payload, withAuth());
  return response.data;
}

export async function deleteSchedule(id) {
  return api.delete(`/team-member-schedules/${id}`, withAuth());
}

export async function fetchLeaves() {
  const response = await api.get('/leaves', withAuth());
  return response.data;
}

export async function createLeave(payload) {
  const response = await api.post('/leaves', payload, withAuth());
  return response.data;
}

export async function updateLeave(id, payload) {
  const response = await api.put(`/leaves/${id}`, payload, withAuth());
  return response.data;
}

export async function deleteLeave(id) {
  return api.delete(`/leaves/${id}`, withAuth());
}

export async function fetchBreaks() {
  const response = await api.get('/breaks', withAuth());
  return response.data;
}

export async function createBreak(payload) {
  const response = await api.post('/breaks', payload, withAuth());
  return response.data;
}

export async function updateBreak(id, payload) {
  const response = await api.put(`/breaks/${id}`, payload, withAuth());
  return response.data;
}

export async function deleteBreak(id) {
  return api.delete(`/breaks/${id}`, withAuth());
}

export async function fetchUsers() {
  const response = await api.get('/users', withAuth());
  return response.data;
}

export async function updateUserRole(id, role) {
  const response = await api.put(`/users/${id}/role`, { role }, withAuth());
  return response.data;
}

export async function assignUserToTeam(id, teamId) {
  const response = await api.post(`/users/${id}/teams`, { teamId }, withAuth());
  return response.data;
}

export async function removeUserFromTeam(id, teamId) {
  const response = await api.delete(`/users/${id}/teams/${teamId}`, withAuth());
  return response.data;
}

export async function updateUserTeamRole(id, teamId, role) {
  const response = await api.put(`/users/${id}/teams/${teamId}/role`, { role }, withAuth());
  return response.data;
}
