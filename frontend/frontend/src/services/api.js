import axios from 'axios';

const SESSION_EXPIRED_MESSAGE = 'Your session expired. Please sign in again.';

function shouldIgnoreLocalDevBaseUrl(baseUrl) {
  if (!baseUrl || typeof window === 'undefined') {
    return false;
  }

  const trimmedBaseUrl = baseUrl.trim().toLowerCase();
  const isLocalDevApi =
    trimmedBaseUrl === 'http://localhost:8080'
    || trimmedBaseUrl === 'https://localhost:8080'
    || trimmedBaseUrl === 'http://127.0.0.1:8080'
    || trimmedBaseUrl === 'https://127.0.0.1:8080';

  const isLocalBrowser =
    window.location.hostname === 'localhost'
    || window.location.hostname === '127.0.0.1';

  return isLocalDevApi && !isLocalBrowser;
}

const configuredBaseUrl = process.env.REACT_APP_API_BASE_URL || '';
const rawBaseUrl = shouldIgnoreLocalDevBaseUrl(configuredBaseUrl)
  ? ''
  : configuredBaseUrl;
const normalizedBaseUrl = rawBaseUrl.endsWith('/')
  ? rawBaseUrl.slice(0, -1)
  : rawBaseUrl;

export const API_BASE_URL = normalizedBaseUrl;
export const API_ROOT = `${API_BASE_URL}/api`;

export function buildApiUrl(path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_ROOT}${normalizedPath}`;
}

export function createApiClient(basePath = '') {
  const normalizedPath = basePath
    ? (basePath.startsWith('/') ? basePath : `/${basePath}`)
    : '';

  const client = axios.create({
    baseURL: `${API_ROOT}${normalizedPath}`,
    headers: { 'Content-Type': 'application/json' },
  });

  client.interceptors.response.use(
    (response) => response,
    (error) => {
      handleAuthExpiredResponse(error);
      return Promise.reject(error);
    },
  );

  return client;
}

function getResponseMessage(error) {
  return typeof error?.response?.data === 'string' ? error.response.data : '';
}

function handleAuthExpiredResponse(error) {
  if (typeof window === 'undefined') {
    return;
  }

  const status = error?.response?.status;
  const message = getResponseMessage(error);
  const hasStoredToken = Boolean(window.sessionStorage.getItem('token'));
  const requestUrl = `${error?.config?.baseURL || ''}${error?.config?.url || ''}`;
  const securityForbidden = status === 403 && !message;
  if (requestUrl.includes('/api/auth/') || !hasStoredToken || (status !== 401 && !securityForbidden)) {
    return;
  }

  window.sessionStorage.removeItem('token');
  window.sessionStorage.removeItem('user');
  window.dispatchEvent(new Event('incteam:user-session-changed'));
  window.dispatchEvent(new CustomEvent('incteam:auth-expired', {
    detail: { message: message || SESSION_EXPIRED_MESSAGE },
  }));
}
