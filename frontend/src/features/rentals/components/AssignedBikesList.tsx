/**
 * AssignedBikesList - displays bikes assigned to a new rental with remove buttons
 */

import type { AssignedBike } from '../types'

export interface AssignedBikesListProps {
  bikes: AssignedBike[]
  onRemove: (bikeId: number) => void
  disabled?: boolean
}

export function AssignedBikesList({
  bikes,
  onRemove,
  disabled = false,
}: AssignedBikesListProps) {
  if (bikes.length === 0) {
    return (
      <div className="text-center py-8 bg-slate-50 border-2 border-dashed border-slate-200 rounded-xl">
        <div className="text-4xl mb-3">🚲</div>
        <p className="text-slate-500 font-medium">No bikes assigned yet</p>
        <p className="text-slate-400 text-sm mt-1">
          Enter a bike number above to add bikes to this rental
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-2">
      {bikes.map((bike, index) => (
        <div
          key={bike.bikeId}
          className="flex items-center justify-between p-4 bg-white border border-slate-200 rounded-xl hover:border-slate-300 transition-colors group"
          style={{ animationDelay: `${index * 50}ms` }}
        >
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 bg-emerald-100 rounded-lg flex items-center justify-center text-emerald-600 font-bold text-lg">
              {index + 1}
            </div>
            <div>
              <div className="font-semibold text-slate-900 text-lg">
                {bike.bikeNumber}
              </div>
              {bike.bikeType && (
                <div className="text-sm text-slate-500">{bike.bikeType}</div>
              )}
            </div>
          </div>

          <button
            type="button"
            onClick={() => onRemove(bike.bikeId)}
            disabled={disabled}
            className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            aria-label={`Remove bike ${bike.bikeNumber}`}
          >
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
              />
            </svg>
          </button>
        </div>
      ))}
    </div>
  )
}

