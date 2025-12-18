/**
 * Hook for fetching rental detail
 */

import { useState, useEffect, useCallback, useRef } from 'react'
import { getRentalDetail, fetchSignatureBlob } from '../api/rentalApi'
import type { RentalDetail } from '../types'

export interface UseRentalDetailResult {
  rental: RentalDetail | null
  signatureUrl: string | null
  isLoading: boolean
  error: string | null
  refresh: () => void
}

export function useRentalDetail(rentalId: number | null): UseRentalDetailResult {
  const [rental, setRental] = useState<RentalDetail | null>(null)
  const [signatureUrl, setSignatureUrl] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const signatureUrlRef = useRef<string | null>(null)

  const fetchData = useCallback(async () => {
    if (rentalId === null) return

    setIsLoading(true)
    setError(null)

    try {
      const rentalData = await getRentalDetail(rentalId)
      setRental(rentalData)

      // Fetch signature blob for display
      try {
        // Revoke previous blob URL if it exists
        if (signatureUrlRef.current) {
          URL.revokeObjectURL(signatureUrlRef.current)
        }
        
        const blobUrl = await fetchSignatureBlob(rentalId)
        signatureUrlRef.current = blobUrl
        setSignatureUrl(blobUrl)
      } catch {
        // Signature fetch failed, continue without it
        if (signatureUrlRef.current) {
          URL.revokeObjectURL(signatureUrlRef.current)
          signatureUrlRef.current = null
        }
        setSignatureUrl(null)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load rental')
      setRental(null)
    } finally {
      setIsLoading(false)
    }
  }, [rentalId])

  useEffect(() => {
    fetchData()

    // Cleanup blob URL on unmount
    return () => {
      if (signatureUrlRef.current) {
        URL.revokeObjectURL(signatureUrlRef.current)
      }
    }
  }, [fetchData])

  return {
    rental,
    signatureUrl,
    isLoading,
    error,
    refresh: fetchData,
  }
}

