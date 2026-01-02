/**
 * Hook for managing new rental state (staff mode)
 * Handles bike assignment with local state - no backend persistence until finalization.
 */

import { useState, useCallback } from 'react'
import { getBikeByNumber } from '@/features/bikes'
import type { AssignedBike, BikeValidationError } from '../types'
import { toAssignedBike } from '../types'

interface UseNewRentalReturn {
  /** List of bikes assigned to the new rental */
  assignedBikes: AssignedBike[]
  /** Current error (if any) */
  error: BikeValidationError | null
  /** Whether a bike lookup is in progress */
  isLoading: boolean
  /** Add a bike by its number (validates via API) */
  addBike: (bikeNumber: string) => Promise<boolean>
  /** Remove a bike from the assigned list */
  removeBike: (bikeId: number) => void
  /** Clear all assigned bikes */
  clearAll: () => void
  /** Clear the current error */
  clearError: () => void
}

export function useNewRental(): UseNewRentalReturn {
  const [assignedBikes, setAssignedBikes] = useState<AssignedBike[]>([])
  const [error, setError] = useState<BikeValidationError | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const clearError = useCallback(() => {
    setError(null)
  }, [])

  const addBike = useCallback(
    async (bikeNumber: string): Promise<boolean> => {
      const trimmedNumber = bikeNumber.trim()
      if (!trimmedNumber) return false

      // Check for duplicate in local list
      const isDuplicate = assignedBikes.some(
        (b) => b.bikeNumber.toLowerCase() === trimmedNumber.toLowerCase()
      )
      if (isDuplicate) {
        setError({ type: 'DUPLICATE', bikeNumber: trimmedNumber })
        return false
      }

      setIsLoading(true)
      setError(null)

      try {
        const bike = await getBikeByNumber(trimmedNumber)

        // Check if bike is available
        if (bike.status !== 'AVAILABLE') {
          setError({
            type: 'NOT_AVAILABLE',
            bikeNumber: bike.bikeNumber,
            status: bike.status,
          })
          return false
        }

        // Add to assigned list
        setAssignedBikes((prev) => [...prev, toAssignedBike(bike)])
        return true
      } catch {
        // API error - bike not found
        setError({ type: 'NOT_FOUND', bikeNumber: trimmedNumber })
        return false
      } finally {
        setIsLoading(false)
      }
    },
    [assignedBikes]
  )

  const removeBike = useCallback((bikeId: number) => {
    setAssignedBikes((prev) => prev.filter((b) => b.bikeId !== bikeId))
    setError(null)
  }, [])

  const clearAll = useCallback(() => {
    setAssignedBikes([])
    setError(null)
  }, [])

  return {
    assignedBikes,
    error,
    isLoading,
    addBike,
    removeBike,
    clearAll,
    clearError,
  }
}

