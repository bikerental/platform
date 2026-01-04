/**
 * BikeInput - input field for adding bikes to a rental
 */

import { useState, useRef, useEffect } from 'react'
import type { BikeValidationError } from '../types'

export interface BikeInputProps {
  onAdd: (bikeNumber: string) => Promise<boolean>
  error: BikeValidationError | null
  isLoading: boolean
  onClearError: () => void
}

function getErrorMessage(error: BikeValidationError): string {
  switch (error.type) {
    case 'NOT_FOUND':
      return `Bike "${error.bikeNumber}" not found`
    case 'NOT_AVAILABLE':
      return `Bike "${error.bikeNumber}" is ${error.status.toLowerCase()} and cannot be rented`
    case 'DUPLICATE':
      return `Bike "${error.bikeNumber}" is already in the list`
  }
}

export function BikeInput({
  onAdd,
  error,
  isLoading,
  onClearError,
}: BikeInputProps) {
  const [value, setValue] = useState('')
  const inputRef = useRef<HTMLInputElement>(null)

  // Focus input on mount
  useEffect(() => {
    inputRef.current?.focus()
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!value.trim() || isLoading) return

    const success = await onAdd(value)
    if (success) {
      setValue('')
      inputRef.current?.focus()
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setValue(e.target.value)
    if (error) {
      onClearError()
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div className="flex gap-3">
        <div className="flex-1 relative">
          <input
            ref={inputRef}
            type="text"
            value={value}
            onChange={handleChange}
            placeholder="Enter bike number (e.g., 1)"
            disabled={isLoading}
            className={`w-full px-4 py-3 text-lg rounded-xl border-2 focus:ring-2 focus:ring-offset-1 outline-none transition-all
              ${
                error
                  ? 'border-red-300 focus:border-red-500 focus:ring-red-200'
                  : 'border-slate-200 focus:border-emerald-500 focus:ring-emerald-200'
              }
              disabled:bg-slate-50 disabled:cursor-not-allowed
            `}
            aria-invalid={!!error}
            aria-describedby={error ? 'bike-input-error' : undefined}
          />
          {isLoading && (
            <div className="absolute right-4 top-1/2 -translate-y-1/2">
              <div className="w-5 h-5 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin" />
            </div>
          )}
        </div>
        <button
          type="submit"
          disabled={!value.trim() || isLoading}
          className="px-6 py-3 bg-emerald-600 text-white font-semibold rounded-xl hover:bg-emerald-700 focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-emerald-600"
        >
          Add Bike
        </button>
      </div>

      {error && (
        <div
          id="bike-input-error"
          className="flex items-center gap-2 text-red-600 text-sm font-medium"
          role="alert"
        >
          <svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
            <path
              fillRule="evenodd"
              d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z"
              clipRule="evenodd"
            />
          </svg>
          <span>{getErrorMessage(error)}</span>
        </div>
      )}
    </form>
  )
}

