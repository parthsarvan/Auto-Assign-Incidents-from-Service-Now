import { authHeader } from './auth';
import { createApiClient } from './api';

const api = createApiClient('/notifications');

export async function fetchNotificationSettings() {
  const response = await api.get('/settings', { headers: authHeader() });
  return response.data;
}

export async function updateNotificationSettings(settings) {
  const response = await api.put('/settings', settings, { headers: authHeader() });
  return response.data;
}

export async function sendNotificationTestEmail() {
  const response = await api.post('/settings/test-email', null, { headers: authHeader() });
  return response.data;
}
