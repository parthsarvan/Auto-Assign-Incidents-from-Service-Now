import axios from 'axios';

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

  return axios.create({
    baseURL: `${API_ROOT}${normalizedPath}`,
    headers: { 'Content-Type': 'application/json' },
  });
}
