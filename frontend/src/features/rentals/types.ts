/**
 * Rentals feature type definitions
 */

import type { Bike } from '@/features/bikes'

/**
 * A bike that has been assigned to a new rental (staff mode state)
 */
export interface AssignedBike {
  bikeId: number
  bikeNumber: string
  bikeType: string | null
}

/**
 * Convert a Bike to AssignedBike
 */
export function toAssignedBike(bike: Bike): AssignedBike {
  return {
    bikeId: bike.bikeId,
    bikeNumber: bike.bikeNumber,
    bikeType: bike.bikeType,
  }
}

/**
 * Rental status
 */
export type RentalStatus = 'ACTIVE' | 'OVERDUE' | 'CLOSED'

/**
 * Rental item status
 */
export type RentalItemStatus = 'RENTED' | 'RETURNED' | 'LOST'

/**
 * Rental item in a rental
 */
export interface RentalItem {
  rentalItemId: number
  bikeId: number
  bikeNumber: string
  bikeType: string | null
  status: RentalItemStatus
  returnedAt: string | null
  lostReason: string | null
}

/**
 * Full rental details (from GET /api/rentals/:id)
 */
export interface RentalDetail {
  rentalId: number
  status: RentalStatus
  startAt: string
  dueAt: string
  returnAt: string | null
  roomNumber: string
  bedNumber: string | null
  tncVersion: string
  signatureId: number
  items: RentalItem[]
}

/**
 * Rental response from create (subset of RentalDetail)
 */
export interface Rental {
  rentalId: number
  status: RentalStatus
  startAt: string
  dueAt: string
  returnAt: string | null
  roomNumber: string
  bedNumber: string | null
  items: RentalItem[]
}

/**
 * Request body for creating a rental
 */
export interface CreateRentalRequest {
  bikeNumbers: string[]
  roomNumber: string
  bedNumber?: string
  returnDateTime: string
  tncVersion: string
  signatureBase64Png: string
}

/**
 * Validation error for bike assignment
 */
export type BikeValidationError =
  | { type: 'NOT_FOUND'; bikeNumber: string }
  | { type: 'NOT_AVAILABLE'; bikeNumber: string; status: string }
  | { type: 'DUPLICATE'; bikeNumber: string }

/**
 * Response from returning a single bike
 */
export interface ReturnBikeResponse {
  rentalItemId: number
  bikeId: number
  bikeNumber: string
  itemStatus: RentalItemStatus
  returnedAt: string | null
  rentalStatus: RentalStatus
  rentalClosed: boolean
}

/**
 * Response from returning multiple bikes
 */
export interface ReturnAllResponse {
  rentalId: number
  rentalStatus: RentalStatus
  returnAt: string | null
  returnedCount: number
  returnedItems: ReturnBikeResponse[]
}

/**
 * Response from marking a bike as lost
 */
export interface MarkLostResponse {
  rentalItemId: number
  bikeId: number
  bikeNumber: string
  itemStatus: RentalItemStatus
  lostReason: string | null
  rentalStatus: RentalStatus
  rentalClosed: boolean
}

