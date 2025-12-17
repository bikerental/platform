/**
 * Overview feature type definitions
 */

import type { RentalStatus } from '@/features/rentals'

/**
 * Summary of an active/overdue rental for the overview
 */
export interface ActiveRentalSummary {
  rentalId: number
  roomNumber: string
  bedNumber: string | null
  dueAt: string
  status: RentalStatus
  bikesOut: number
  bikesTotal: number
}

/**
 * Overview response containing bike/rental counts and active rentals
 */
export interface OverviewData {
  bikesAvailable: number
  bikesRented: number
  bikesOoo: number
  rentalsActive: number
  rentalsOverdue: number
  activeRentals: ActiveRentalSummary[]
}

/**
 * Search filter for active rentals
 */
export interface OverviewSearchParams {
  q?: string
}

