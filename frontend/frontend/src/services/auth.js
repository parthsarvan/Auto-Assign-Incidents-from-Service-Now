// src/services/auth.js
import axios from 'axios';

// Base Axios instance
const api = axios.create({
  baseURL: 'http://localhost:8080/api/auth', // adjust if your backend sits under a different path
  headers: { 'Content-Type': 'application/json' },
});

// Sign-In: POST /api/auth/login  { username, password }
export async function signIn(username, password) {
  const response = await api.post('/login', { username, password });
  // response.data should be { token, u_id, username, role }

  // 1) Save the raw JWT token string
  sessionStorage.setItem('token', response.data.token);

  // 2) Build a plain object containing only the user fields you need
  const userObj = {
    u_id:     response.data.u_id,
    username: response.data.username,
    role:     response.data.role,
  };

  // 3) Convert that object to a JSON string
  sessionStorage.setItem('user', JSON.stringify(userObj));

  return response.data;
}

// Sign-Up: POST /api/auth/signup  { username, password, role }
export async function signUp(username, password, role) {
  const response = await api.post('/signup', { username, password, role });
  return response.data; // e.g. { message: 'User created', user: { … } }
}

// Sign-Out: simply clear sessionStorage
export function signOut() {
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('user');
}

// Helper: get current user (or null)
export function getCurrentUser() {
  const userJson = sessionStorage.getItem('user');
  return userJson ? JSON.parse(userJson) : null;
}

// Helper: get auth header
export function authHeader() {
  const token = sessionStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}
