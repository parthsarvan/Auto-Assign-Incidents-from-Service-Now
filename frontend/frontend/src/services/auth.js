// src/services/auth.js
import { createApiClient } from './api';

function emitUserSessionChanged() {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event('incteam:user-session-changed'));
  }
}

// Base Axios instance
const api = createApiClient('/auth');

export async function discoverOrganization({ organizationName, workEmail }) {
  const response = await api.post('/organization-discovery', {
    organizationName: organizationName.trim(),
    workEmail: workEmail.trim().toLowerCase(),
  });
  return response.data;
}

// Sign-In: POST /api/auth/login  { username, password }
export async function signIn(username, password) {
  const response = await api.post('/login', {
    username: username.trim(),
    password,
  });
  // response.data should be { token, u_id, username, role }

  // 1) Save the raw JWT token string
  sessionStorage.setItem('token', response.data.token);

  // 2) Build a plain object containing only the user fields you need
  const userObj = {
    u_id:     response.data.u_id,
    username: response.data.username,
    workEmail: response.data.workEmail || null,
    role:     response.data.role,
    workspace: response.data.workspace || null,
  };

  // 3) Convert that object to a JSON string
  sessionStorage.setItem('user', JSON.stringify(userObj));
  emitUserSessionChanged();

  return response.data;
}

// Sign-Up: POST /api/auth/signup  { username, password, role }
export async function signUp({
  username,
  firstName,
  lastName,
  workEmail,
  password,
  inviteCode = '',
  organizationName = '',
  teamName = '',
}) {
  const response = await api.post('/signup', {
    username: username.trim(),
    firstName: firstName.trim(),
    lastName: lastName.trim(),
    workEmail: workEmail.trim().toLowerCase(),
    password,
    inviteCode: inviteCode.trim().toUpperCase(),
    organizationName: organizationName.trim(),
    teamName: teamName.trim(),
  });
  return response.data; // e.g. { message: 'User created', user: { … } }
}

// Sign-Out: simply clear sessionStorage
export function signOut() {
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('user');
  emitUserSessionChanged();
}

// Helper: get current user (or null)
export function getCurrentUser() {
  const userJson = sessionStorage.getItem('user');
  return userJson ? JSON.parse(userJson) : null;
}

export function setCurrentUser(userObj) {
  sessionStorage.setItem('user', JSON.stringify(userObj));
  emitUserSessionChanged();
}

// Helper: get auth header
export function authHeader() {
  const token = sessionStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}
