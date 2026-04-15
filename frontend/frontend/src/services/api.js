import axios from 'axios';

const rawBaseUrl = process.env.REACT_APP_API_BASE_URL || '';
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
