/**
 * Bikes API layer
 * Handles all bike-related API calls.
 */

import { apiGet, apiPatch } from '@/lib/api'
import type { Bike, BikeListParams } from '../types'

/**
 * List bikes with optional filters
 */
export async function listBikes(params?: BikeListParams): Promise<Bike[]> {
  const searchParams = new URLSearchParams()
  if (params?.status) {
    searchParams.append('status', params.status)
  }
  if (params?.q) {
    searchParams.append('q', params.q)
  }
  
  const queryString = searchParams.toString()
  const path = queryString ? `/bikes?${queryString}` : '/bikes'
  
  return apiGet<Bike[]>(path)
}

/**
 * Get a bike by bike number
 */
export async function getBikeByNumber(bikeNumber: string): Promise<Bike> {
  return apiGet<Bike>(`/bikes/by-number/${encodeURIComponent(bikeNumber)}`)
}

/**
 * Mark a bike as Out of Order (OOO)
 */
export async function markOoo(bikeId: number, note: string): Promise<Bike> {
  return apiPatch<Bike>(`/bikes/${bikeId}/ooo`, { note })
}

/**
 * Mark a bike as available
 */
export async function markAvailable(bikeId: number): Promise<Bike> {
  return apiPatch<Bike>(`/bikes/${bikeId}/available`, {})
}

/**
 * Bike API object (alternative export style for backward compatibility)
 */
export const bikeApi = {
  listBikes,
  getBikeByNumber,
  markOoo,
  markAvailable,
}

