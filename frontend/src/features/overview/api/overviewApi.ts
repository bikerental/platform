/**
 * Overview API layer
 * Handles all overview-related API calls.
 */

import { apiGet } from '@/lib/api'
import type { OverviewData } from '../types'

/**
 * Fetch overview data for the current hotel
 */
export async function getOverview(): Promise<OverviewData> {
  return apiGet<OverviewData>('/overview')
}

/**
 * Overview API object
 */
export const overviewApi = {
  getOverview,
}

