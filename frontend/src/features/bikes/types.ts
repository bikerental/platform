/**
 * Bikes feature type definitions
 */

export type BikeStatus = 'AVAILABLE' | 'RENTED' | 'OOO'

export interface Bike {
  bikeId: number
  bikeNumber: string
  bikeType: string | null
  status: BikeStatus
  oooNote: string | null
  oooSince: string | null
}

export interface BikeListParams {
  status?: BikeStatus
  q?: string
}

export interface MarkOooRequest {
  note: string
}

