/**
 * Auth API layer
 * Handles authentication API calls and token/session storage.
 */

import { apiUrl, setToken, clearToken, getToken } from '@/lib/api'
import type { LoginRequest, LoginResponse } from '../types'

const HOTEL_NAME_KEY = 'hotel_name'

interface ErrorResponse {
  error: string
  message: string
  details?: Record<string, unknown>
  timestamp: string
}

/**
 * Login with hotel credentials
 */
export async function login(credentials: LoginRequest): Promise<LoginResponse> {
  const response = await fetch(apiUrl('/auth/login'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  })

  if (!response.ok) {
    let message = 'Login failed'
    try {
      const error: ErrorResponse = await response.json()
      message = error.message || message
    } catch {
      // Non-JSON response (HTML/text/empty) — keep default message
    }
    throw new Error(message)
  }

  const data: LoginResponse = await response.json()
  
  // Store token and hotel name
  setToken(data.accessToken)
  localStorage.setItem(HOTEL_NAME_KEY, data.hotelName)
  
  return data
}

/**
 * Logout - clear stored auth data
 */
export function logout(): void {
  clearToken()
  localStorage.removeItem(HOTEL_NAME_KEY)
}

/**
 * Get stored hotel name
 */
export function getHotelName(): string | null {
  return localStorage.getItem(HOTEL_NAME_KEY)
}

/**
 * Check if user is authenticated (has token)
 */
export function isAuthenticated(): boolean {
  return getToken() !== null
}

