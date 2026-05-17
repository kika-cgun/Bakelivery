/**
 * Base fetch client with automatic JWT injection.
 * Token is read from localStorage on every request (no stale closures).
 */

const BASE = '/api';

function getToken(): string | null {
  return localStorage.getItem('bakelivery_token');
}

interface RequestOptions extends RequestInit {
  skipAuth?: boolean;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { skipAuth = false, ...fetchOptions } = options;

  const headers = new Headers(fetchOptions.headers);
  headers.set('Content-Type', 'application/json');

  if (!skipAuth) {
    const token = getToken();
    if (token) headers.set('Authorization', `Bearer ${token}`);
  }

  const res = await fetch(`${BASE}${path}`, { ...fetchOptions, headers });

  if (res.status === 401) {
    localStorage.removeItem('bakelivery_token');
    localStorage.removeItem('bakelivery_role');
    window.dispatchEvent(new Event('bakelivery:unauthorized'));
    throw new Error('Unauthorized');
  }

  if (!res.ok) {
    const body = await res.text().catch(() => '');
    throw new Error(body || `HTTP ${res.status}`);
  }

  // 204 No Content
  if (res.status === 204) return undefined as T;

  return res.json() as Promise<T>;
}

export const api = {
  get:    <T>(path: string, opts?: RequestOptions) =>
    request<T>(path, { method: 'GET', ...opts }),
  post:   <T>(path: string, body: unknown, opts?: RequestOptions) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body), ...opts }),
  put:    <T>(path: string, body: unknown, opts?: RequestOptions) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body), ...opts }),
  patch:  <T>(path: string, body: unknown, opts?: RequestOptions) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(body), ...opts }),
  delete: <T>(path: string, opts?: RequestOptions) =>
    request<T>(path, { method: 'DELETE', ...opts }),
};
