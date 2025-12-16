/**
 * useBikes hook - manages bike data fetching, filtering, and actions
 */

import { useState, useEffect, useCallback } from 'react'
import { listBikes, markOoo, markAvailable } from '../api/bikeApi'
import type { Bike, BikeStatus } from '../types'

export type StatusFilter = BikeStatus | 'ALL'

export interface UseBikesReturn {
  bikes: Bike[]
  isLoading: boolean
  error: string | null
  statusFilter: StatusFilter
  searchQuery: string
  setStatusFilter: (status: StatusFilter) => void
  setSearchQuery: (query: string) => void
  handleMarkOoo: (bike: Bike, note: string) => Promise<void>
  handleMarkAvailable: (bike: Bike) => Promise<void>
  clearError: () => void
  refresh: () => void
}

const DEBOUNCE_DELAY = 300

export function useBikes(): UseBikesReturn {
  const [bikes, setBikes] = useState<Bike[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL')
  const [searchQuery, setSearchQuery] = useState('')
  const [debouncedQuery, setDebouncedQuery] = useState('')

  // Debounce search query
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchQuery)
    }, DEBOUNCE_DELAY)
    return () => clearTimeout(timer)
  }, [searchQuery])

  // Fetch bikes when filters change
  const loadBikes = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const params: { status?: BikeStatus; q?: string } = {}
      if (statusFilter !== 'ALL') {
        params.status = statusFilter
      }
      if (debouncedQuery.trim()) {
        params.q = debouncedQuery.trim()
      }
      const data = await listBikes(params)
      setBikes(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load bikes')
    } finally {
      setIsLoading(false)
    }
  }, [statusFilter, debouncedQuery])

  useEffect(() => {
    loadBikes()
  }, [loadBikes])

  const handleMarkOoo = useCallback(
    async (bike: Bike, note: string) => {
      try {
        await markOoo(bike.bikeId, note)
        await loadBikes()
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to mark bike as OOO')
        throw err // Re-throw so caller can handle
      }
    },
    [loadBikes]
  )

  const handleMarkAvailable = useCallback(
    async (bike: Bike) => {
      try {
        await markAvailable(bike.bikeId)
        await loadBikes()
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to mark bike as available')
      }
    },
    [loadBikes]
  )

  const clearError = useCallback(() => {
    setError(null)
  }, [])

  return {
    bikes,
    isLoading,
    error,
    statusFilter,
    searchQuery,
    setStatusFilter,
    setSearchQuery,
    handleMarkOoo,
    handleMarkAvailable,
    clearError,
    refresh: loadBikes,
  }
}

