import { authHeader, signOut } from './auth';
import { createApiClient } from './api';

const api = createApiClient('/account');

function withAuth() {
  return { headers: authHeader() };
}

export async function deleteCurrentAccount() {
  const response = await api.delete('', withAuth());
  signOut();
  return response.data;
}
