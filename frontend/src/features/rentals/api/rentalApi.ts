/**
 * Rental API service
 * Handles all rental-related API calls
 */

import { apiGet, apiPost, apiUrl, getToken } from '@/lib/api'
import type { CreateRentalRequest, Rental, RentalDetail } from '../types'

/**
 * Error response for unavailable bikes (409 Conflict)
 */
export interface UnavailableBike {
  bikeNumber: string
  reason: 'NOT_FOUND' | 'ALREADY_RENTED' | 'OUT_OF_ORDER'
}

export interface BikeUnavailableError {
  error: string
  message: string
  details: {
    unavailableBikes: UnavailableBike[]
  }
  timestamp: string
}

/**
 * Create a new rental
 * @throws Error with message for general errors
 * @throws BikeUnavailableError (as Error with details) for 409 conflicts
 */
export async function createRental(request: CreateRentalRequest): Promise<Rental> {
  return apiPost<Rental>('/rentals', request)
}

/**
 * Get rental details by ID
 */
export async function getRentalDetail(rentalId: number): Promise<RentalDetail> {
  return apiGet<RentalDetail>(`/rentals/${rentalId}`)
}

/**
 * Get the URL for the rental signature image
 */
export function getSignatureUrl(rentalId: number): string {
  return apiUrl(`/rentals/${rentalId}/signature`)
}

/**
 * Get the URL for the rental contract HTML
 */
export function getContractUrl(rentalId: number): string {
  return apiUrl(`/rentals/${rentalId}/contract`)
}

/**
 * Fetch signature image as blob URL for display
 * (needed for authenticated image requests)
 */
export async function fetchSignatureBlob(rentalId: number): Promise<string> {
  const token = getToken()
  const response = await fetch(apiUrl(`/rentals/${rentalId}/signature`), {
    headers: {
      Authorization: `Bearer ${token || ''}`,
    },
  })
  
  if (response.status === 401) {
    localStorage.removeItem('auth_token')
    window.location.href = '/login'
    throw new Error('Unauthorized')
  }
  
  if (!response.ok) {
    throw new Error('Failed to fetch signature')
  }
  
  const blob = await response.blob()
  return URL.createObjectURL(blob)
}

/**
 * Create rental with enhanced error handling for 409 responses
 * Returns either the rental or detailed unavailable bike info
 */
export async function createRentalWithDetails(
  request: CreateRentalRequest
): Promise<{ success: true; rental: Rental } | { success: false; unavailableBikes: UnavailableBike[] }> {
  try {
    const response = await fetch(
      `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'}/rentals`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('auth_token') || ''}`,
        },
        body: JSON.stringify(request),
      }
    )

    if (response.ok) {
      const rental = await response.json()
      return { success: true, rental }
    }

    if (response.status === 409) {
      const errorData: BikeUnavailableError = await response.json()
      return {
        success: false,
        unavailableBikes: errorData.details?.unavailableBikes || [],
      }
    }

    if (response.status === 401) {
      localStorage.removeItem('auth_token')
      window.location.href = '/login'
      throw new Error('Unauthorized')
    }

    // Handle other errors
    const errorData = await response.json().catch(() => ({ message: 'An error occurred' }))
    throw new Error(errorData.message)
  } catch (error) {
    if (error instanceof Error) {
      throw error
    }
    throw new Error('Failed to create rental')
  }
}
