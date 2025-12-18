/**
 * RentalErrorScreen - Error state when bikes become unavailable
 */

import type { UnavailableBike } from '../api/rentalApi'

export interface RentalErrorScreenProps {
  unavailableBikes: UnavailableBike[]
  onRetry: () => void
}

function getReasonText(reason: string): string {
  switch (reason) {
    case 'ALREADY_RENTED':
      return 'Already rented'
    case 'OUT_OF_ORDER':
      return 'Out of order'
    default:
      return 'Not found'
  }
}

export function RentalErrorScreen({ unavailableBikes, onRetry }: RentalErrorScreenProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-red-50 to-orange-50 p-4">
      <div className="w-full max-w-lg rounded-3xl bg-white p-8 text-center shadow-xl">
        {/* Error Icon */}
        <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-red-100">
          <svg className="h-10 w-10 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
        </div>

        <h1 className="mb-2 text-2xl font-bold text-slate-900">Some Bikes Unavailable</h1>
        <p className="mb-6 text-slate-600">
          One or more bikes became unavailable. Please return to staff mode to reassign bikes.
        </p>

        {/* Unavailable Bikes List */}
        <div className="mb-8 rounded-2xl bg-red-50 p-4 text-left">
          <h3 className="mb-3 font-semibold text-red-800">Unavailable Bikes:</h3>
          <ul className="space-y-2">
            {unavailableBikes.map(bike => (
              <li key={bike.bikeNumber} className="flex items-center gap-2 text-sm">
                <span className="font-medium text-red-900">#{bike.bikeNumber}</span>
                <span className="text-red-600">({getReasonText(bike.reason)})</span>
              </li>
            ))}
          </ul>
        </div>

        <button
          onClick={onRetry}
          className="w-full rounded-xl bg-slate-800 py-4 font-semibold text-white transition-colors hover:bg-slate-900"
        >
          Return to Staff Mode
        </button>
      </div>
    </div>
  )
}

