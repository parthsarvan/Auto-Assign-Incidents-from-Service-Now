import { authHeader } from './auth';
import { createApiClient } from './api';

const api = createApiClient('/setup');

export async function fetchSetupStatus() {
  const response = await api.get('/status', { headers: authHeader() });
  return response.data;
}
