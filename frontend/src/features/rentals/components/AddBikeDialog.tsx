/**
 * AddBikeDialog - Dialog for adding a bike to an existing rental
 */

import { useState, useRef, useEffect } from 'react'
import { Modal } from '@/components/ui/Modal'
import { addBikeToRentalWithDetails, type UnavailableBike } from '../api/rentalApi'

export interface AddBikeDialogProps {
  isOpen: boolean
  rentalId: number
  onClose: () => void
  onSuccess: () => void
}

function getErrorMessage(unavailableBike: UnavailableBike): string {
  switch (unavailableBike.reason) {
    case 'NOT_FOUND':
      return `Bike "${unavailableBike.bikeNumber}" not found`
    case 'ALREADY_RENTED':
      return `Bike "${unavailableBike.bikeNumber}" is already rented`
    case 'OUT_OF_ORDER':
      return `Bike "${unavailableBike.bikeNumber}" is out of order`
  }
}

export function AddBikeDialog({ isOpen, rentalId, onClose, onSuccess }: AddBikeDialogProps) {
  const [bikeNumber, setBikeNumber] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  // Focus input and reset state when dialog opens
  useEffect(() => {
    if (isOpen) {
      setBikeNumber('')
      setError(null)
      setTimeout(() => inputRef.current?.focus(), 100)
    }
  }, [isOpen])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!bikeNumber.trim() || isLoading) return

    setIsLoading(true)
    setError(null)

    try {
      const result = await addBikeToRentalWithDetails(rentalId, bikeNumber.trim())
      
      if (result.success) {
        onSuccess()
        onClose()
      } else {
        // Show error for unavailable bike
        if (result.unavailableBikes.length > 0) {
          setError(getErrorMessage(result.unavailableBikes[0]))
        } else {
          setError('Bike is not available')
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to add bike')
    } finally {
      setIsLoading(false)
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setBikeNumber(e.target.value)
    if (error) setError(null)
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Add Bike to Rental"
      footer={
        <>
          <button
            onClick={onClose}
            disabled={isLoading}
            className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={!bikeNumber.trim() || isLoading}
            className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
          >
            {isLoading ? 'Adding...' : 'Add Bike'}
          </button>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        <p className="text-sm text-slate-600">
          Enter the bike number to add to this rental. The bike must be available (not rented or out of order).
        </p>

        <div className="space-y-2">
          <label htmlFor="bikeNumber" className="block text-sm font-medium text-slate-700">
            Bike Number
          </label>
          <div className="relative">
            <input
              ref={inputRef}
              id="bikeNumber"
              type="text"
              value={bikeNumber}
              onChange={handleChange}
              placeholder="e.g., 1"
              disabled={isLoading}
              className={`w-full rounded-lg border-2 px-4 py-2.5 text-sm outline-none transition-all
                ${error
                  ? 'border-rose-300 focus:border-rose-500 focus:ring-2 focus:ring-rose-200'
                  : 'border-slate-200 focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200'
                }
                disabled:bg-slate-50 disabled:cursor-not-allowed
              `}
              aria-invalid={!!error}
              aria-describedby={error ? 'add-bike-error' : undefined}
            />
            {isLoading && (
              <div className="absolute right-3 top-1/2 -translate-y-1/2">
                <div className="h-5 w-5 animate-spin rounded-full border-2 border-emerald-500 border-t-transparent" />
              </div>
            )}
          </div>
        </div>

        {error && (
          <div
            id="add-bike-error"
            className="flex items-center gap-2 rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700"
            role="alert"
          >
            <svg className="h-4 w-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path
                fillRule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
                clipRule="evenodd"
              />
            </svg>
            <span>{error}</span>
          </div>
        )}
      </form>
    </Modal>
  )
}

