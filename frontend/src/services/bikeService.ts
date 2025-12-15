import { apiGet, apiPatch } from './api'

export interface Bike {
  bikeId: number
  bikeNumber: string
  bikeType: string | null
  status: 'AVAILABLE' | 'RENTED' | 'OOO'
  oooNote: string | null
  oooSince: string | null
}

export interface BikeListParams {
  status?: 'AVAILABLE' | 'RENTED' | 'OOO'
  q?: string
}

export interface MarkOooRequest {
  note: string
}

/**
 * Bike service for API calls
 */
export const bikeService = {
  /**
   * List bikes with optional filters
   */
  async listBikes(params?: BikeListParams): Promise<Bike[]> {
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
  },

  /**
   * Get a bike by bike number
   */
  async getBikeByNumber(bikeNumber: string): Promise<Bike> {
    return apiGet<Bike>(`/bikes/by-number/${encodeURIComponent(bikeNumber)}`)
  },

  /**
   * Mark a bike as Out of Order (OOO)
   */
  async markOoo(bikeId: number, note: string): Promise<Bike> {
    return apiPatch<Bike>(`/bikes/${bikeId}/ooo`, { note })
  },

  /**
   * Mark a bike as available
   */
  async markAvailable(bikeId: number): Promise<Bike> {
    return apiPatch<Bike>(`/bikes/${bikeId}/available`, {})
  },
}

