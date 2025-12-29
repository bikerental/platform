/**
 * Bikes API layer
 * Handles all bike-related API calls.
 */

import { apiGet, apiPatch, apiUrl, getToken } from '@/lib/api'
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
 * Export OOO bikes as CSV file download.
 * Triggers browser download with file named "ooo-bikes-YYYY-MM-DD.csv".
 * @throws Error if export fails
 */
export async function exportOooBikesCsv(): Promise<void> {
  const token = getToken()
  
  const response = await fetch(apiUrl('/maintenance/ooo/export'), {
    method: 'GET',
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  })

  if (!response.ok) {
    if (response.status === 401) {
      window.location.href = '/login'
      throw new Error('Session expired. Please login again.')
    }
    throw new Error('Failed to export OOO bikes')
  }

  // Extract filename from Content-Disposition header or use default
  const contentDisposition = response.headers.get('Content-Disposition')
  let filename = 'ooo-bikes.csv'
  if (contentDisposition) {
    const match = contentDisposition.match(/filename="?([^";\n]+)"?/)
    if (match && match[1]) {
      filename = match[1]
    }
  }

  // Create blob and trigger download
  const blob = await response.blob()
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * Bike API object (alternative export style for backward compatibility)
 */
export const bikeApi = {
  listBikes,
  getBikeByNumber,
  markOoo,
  markAvailable,
  exportOooBikesCsv,
}

