/**
 * RentalSuccessScreen - Success state after rental creation
 */

import type { Rental } from '../types'

export interface RentalSuccessScreenProps {
  rental: Rental
  onDone: () => void
}

export function RentalSuccessScreen({ rental, onDone }: RentalSuccessScreenProps) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-emerald-50 to-teal-50 p-4">
      <div className="w-full max-w-lg rounded-3xl bg-white p-8 text-center shadow-xl">
        {/* Success Icon */}
        <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-emerald-100">
          <svg className="h-10 w-10 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>

        <h1 className="mb-2 text-3xl font-bold text-slate-900">Rental Created!</h1>
        <p className="mb-8 text-slate-600">
          Your rental has been successfully recorded. Please return the bikes by the due date.
        </p>

        {/* Rental Summary */}
        <div className="mb-8 rounded-2xl bg-slate-50 p-6 text-left">
          <div className="grid grid-cols-3 gap-4 text-sm">
            <div>
              <span className="text-slate-500">Room</span>
              <p className="font-semibold text-slate-900">
                {rental.roomNumber}
                {rental.bedNumber && ` / Bed ${rental.bedNumber}`}
              </p>
            </div>
            <div>
              <span className="text-slate-500">Due By</span>
              <p className="font-semibold text-slate-900">{new Date(rental.dueAt).toLocaleString()}</p>
            </div>
            <div>
              <span className="text-slate-500">Bikes</span>
              <p className="font-semibold text-slate-900">{rental.items.length} bike(s)</p>
            </div>
          </div>

          {/* Bike List */}
          <div className="mt-4 border-t border-slate-200 pt-4">
            <span className="text-sm text-slate-500">Bikes Rented:</span>
            <div className="mt-2 flex flex-wrap gap-2">
              {rental.items.map(item => (
                <span
                  key={item.rentalItemId}
                  className="rounded-full bg-emerald-100 px-3 py-1 text-sm font-medium text-emerald-800"
                >
                  #{item.bikeNumber}
                </span>
              ))}
            </div>
          </div>
        </div>

        <button
          onClick={onDone}
          className="w-full rounded-xl bg-emerald-600 py-4 text-lg font-semibold text-white transition-colors hover:bg-emerald-700"
        >
          Done
        </button>
      </div>
    </div>
  )
}

