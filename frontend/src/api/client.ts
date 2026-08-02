import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL,
  withCredentials: true,
});

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(^|; )' + name + '=([^;]*)'));
  return match ? decodeURIComponent(match[2]) : null;
}

apiClient.interceptors.request.use((config) => {
  const method = (config.method ?? 'get').toLowerCase();
  if (method !== 'get' && method !== 'head' && method !== 'options') {
    const token = readCookie('XSRF-TOKEN');
    if (token) {
      config.headers = config.headers ?? {};
      config.headers['X-XSRF-TOKEN'] = token;
    }
  }
  return config;
});

const AUTH_CHECK_PATHS = ['/auth/me', '/auth/login'];

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const url: string = error.config?.url ?? '';
      const isAuthCheck = AUTH_CHECK_PATHS.some((path) => url.includes(path));
      if (!isAuthCheck && window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);
