import { apiUrl } from '../config/api'
import { getToken, logout } from './authService'

export interface ApiError {
  error: string
  message: string
  details?: Record<string, unknown>
  timestamp: string
}

/**
 * Make an authenticated API request.
 * Automatically attaches Authorization header if token exists.
 * Handles 401 by clearing token and redirecting to login.
 */
export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken()
  
  // Create Headers instance from existing headers (handles Headers, Record, or array)
  const headers = new Headers(options.headers)
  
  // Only set default Content-Type if:
  // 1. Caller didn't specify one
  // 2. Body is not FormData, Blob, or URLSearchParams (which set their own content types)
  if (!headers.has('Content-Type')) {
    const body = options.body
    if (!(body instanceof FormData) && !(body instanceof Blob) && !(body instanceof URLSearchParams)) {
      headers.set('Content-Type', 'application/json')
    }
  }
  
  // Add Authorization header if token exists
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(apiUrl(path), {
    ...options,
    headers,
  })

  // Handle 401 - unauthorized
  if (response.status === 401) {
    logout()
    window.location.href = '/login'
    throw new Error('Session expired. Please login again.')
  }

  // Handle other errors
  if (!response.ok) {
    const errorData: ApiError = await response.json().catch(() => ({
      error: 'UNKNOWN_ERROR',
      message: 'An unexpected error occurred',
      timestamp: new Date().toISOString(),
    }))
    throw new Error(errorData.message)
  }

  // Handle empty responses (204 No Content)
  if (response.status === 204) {
    return {} as T
  }

  return response.json()
}

/**
 * GET request helper
 */
export function apiGet<T>(path: string): Promise<T> {
  return apiFetch<T>(path, { method: 'GET' })
}

/**
 * POST request helper
 */
export function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return apiFetch<T>(path, {
    method: 'POST',
    body: body ? JSON.stringify(body) : undefined,
  })
}

/**
 * PATCH request helper
 */
export function apiPatch<T>(path: string, body?: unknown): Promise<T> {
  return apiFetch<T>(path, {
    method: 'PATCH',
    body: body ? JSON.stringify(body) : undefined,
  })
}

/**
 * DELETE request helper
 */
export function apiDelete<T>(path: string): Promise<T> {
  return apiFetch<T>(path, { method: 'DELETE' })
}
