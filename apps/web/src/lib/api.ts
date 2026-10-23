import type { Page, Title } from '@/types/catalog';
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); this.name = 'ApiError'; }
}
export async function apiFetch<T>(path: string, token?: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers
    },
    cache: 'no-store'
  });
  if (!response.ok) {
    let detail = '';
    try {
      const body = await response.json() as {detail?:string;message?:string;title?:string};
      detail = body.detail ?? body.message ?? body.title ?? '';
    } catch { /* Server may return an empty or plain-text error. */ }
    throw new ApiError(response.status, `Request failed (${response.status})${detail ? ': ' + detail.slice(0,250) : ''}`);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}
export async function getCatalog(signal?: AbortSignal): Promise<Title[]> {
  const page = await apiFetch<Page<Title>>('/api/v1/catalog?size=48', undefined, { signal });
  return page.content;
}
