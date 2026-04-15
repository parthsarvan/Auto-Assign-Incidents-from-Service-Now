import axios from 'axios';
import { authHeader } from './auth';
import { buildApiUrl, createApiClient } from './api';

const api = createApiClient('/servicenow');

export async function fetchServiceNowHealth() {
  const response = await api.get('/health', { headers: authHeader() });
  return response.data;
}

export async function fetchAssignmentDiagnostics() {
  const response = await api.get('/assignment-diagnostics', { headers: authHeader() });
  return response.data;
}

export async function fetchServiceNowValidation() {
  const response = await api.get('/validation', { headers: authHeader() });
  return response.data;
}

export async function fetchCoverageSummary(days = 7) {
  const response = await axios.get(buildApiUrl('/coverage/summary'), {
    params: { days },
    headers: authHeader(),
  });
  return response.data;
}
