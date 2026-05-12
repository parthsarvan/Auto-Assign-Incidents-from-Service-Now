import { authHeader } from './auth';
import { createApiClient } from './api';

const api = createApiClient('/routing');

export async function fetchCurrentRoutingWindow() {
  const response = await api.get('/current-window', { headers: authHeader() });
  return response.data;
}
