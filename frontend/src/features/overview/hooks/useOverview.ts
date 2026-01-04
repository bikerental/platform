/**
 * useOverview hook - manages overview data fetching and filtering
 */

import { useState, useEffect, useCallback, useMemo } from 'react'
import { getOverview } from '../api/overviewApi'
import type { OverviewData, ActiveRentalSummary } from '../types'

const DEBOUNCE_DELAY = 300
const POLL_INTERVAL = 30000 // Refresh every 30 seconds

export interface UseOverviewReturn {
  data: OverviewData | null
  filteredRentals: ActiveRentalSummary[]
  isLoading: boolean
  error: string | null
  searchQuery: string
  setSearchQuery: (query: string) => void
  refresh: () => void
}

export function useOverview(): UseOverviewReturn {
  const [data, setData] = useState<OverviewData | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')

  // Debounce search query
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, DEBOUNCE_DELAY)
    return () => clearTimeout(timer)
  }, [searchQuery])

  // Fetch overview data
  const loadOverview = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const overviewData = await getOverview()
      setData(overviewData)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load overview')
    } finally {
      setIsLoading(false)
    }
  }, [])

  // Initial load
  useEffect(() => {
    loadOverview()
  }, [loadOverview])

  // Polling for updates
  useEffect(() => {
    const interval = setInterval(() => {
      loadOverview()
    }, POLL_INTERVAL)
    return () => clearInterval(interval)
  }, [loadOverview])

  // Filter rentals client-side by search query
  // Matches: bike numbers, room number, bed number
  const filteredRentals = useMemo(() => {
    if (!data?.activeRentals) return []

    const query = debouncedQuery.trim().toLowerCase()
    if (!query) return data.activeRentals

    return data.activeRentals.filter((rental) => {
      const roomMatch = rental.roomNumber.toLowerCase().includes(query)
      const bedMatch = rental.bedNumber?.toLowerCase().includes(query) ?? false
      const bikeMatch = rental.bikeNumbers.some((bikeNumber) =>
        bikeNumber.toLowerCase().includes(query)
      )

      return roomMatch || bedMatch || bikeMatch
    })
  }, [data?.activeRentals, debouncedQuery])

  return {
    data,
    filteredRentals,
    isLoading,
    error,
    searchQuery,
    setSearchQuery,
    refresh: loadOverview,
  }
}

