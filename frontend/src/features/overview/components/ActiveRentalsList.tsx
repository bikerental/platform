/**
 * ActiveRentalsList - displays active/overdue rentals in a table
 */

import { useNavigate } from 'react-router-dom'
import { Badge } from '@/components/ui/Badge'
import type { ActiveRentalSummary } from '../types'

export interface ActiveRentalsListProps {
  rentals: ActiveRentalSummary[]
  isLoading: boolean
}

/**
 * Format a date string for display
 */
function formatDueDate(isoString: string): string {
  const date = new Date(isoString)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

/**
 * Get time relative to now (e.g., "2h overdue", "in 3h")
 */
function getTimeRelative(isoString: string, status: 'ACTIVE' | 'OVERDUE' | 'CLOSED'): string {
  const now = new Date()
  const dueDate = new Date(isoString)
  const diffMs = dueDate.getTime() - now.getTime()
  const diffMins = Math.round(diffMs / (1000 * 60))
  const diffHours = Math.round(diffMs / (1000 * 60 * 60))

  if (status === 'OVERDUE') {
    if (Math.abs(diffMins) < 60) {
      return `${Math.abs(diffMins)}m overdue`
    }
    return `${Math.abs(diffHours)}h overdue`
  }

  if (diffMins <= 0) {
    return 'Due now'
  }
  if (diffMins < 60) {
    return `in ${diffMins}m`
  }
  return `in ${diffHours}h`
}

export function ActiveRentalsList({ rentals, isLoading }: ActiveRentalsListProps) {
  const navigate = useNavigate()

  if (isLoading) {
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-8">
        <div className="flex items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-sky-500 border-t-transparent" />
          <span className="ml-3 text-slate-600">Loading rentals...</span>
        </div>
      </div>
    )
  }

  if (rentals.length === 0) {
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-8 text-center">
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-emerald-100">
          <svg className="h-8 w-8 text-emerald-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
          </svg>
        </div>
        <h3 className="text-lg font-semibold text-slate-900">All Clear!</h3>
        <p className="mt-1 text-slate-600">No active or overdue rentals at the moment.</p>
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200">
        <thead className="bg-slate-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
              Room / Bed
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
              Bikes
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
              Due
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
              Status
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rentals.map((rental) => (
            <tr
              key={rental.rentalId}
              onClick={() => navigate(`/rentals/${rental.rentalId}`)}
              className="cursor-pointer transition-colors hover:bg-slate-50"
            >
              <td className="whitespace-nowrap px-4 py-3">
                <div className="text-sm font-medium text-slate-900">
                  {rental.roomNumber}
                </div>
                {rental.bedNumber && (
                  <div className="text-xs text-slate-500">
                    Bed {rental.bedNumber}
                  </div>
                )}
              </td>
              <td className="px-4 py-3">
                <div className="flex flex-wrap gap-1">
                  {rental.bikeNumbers.map((bikeNumber) => (
                    <span
                      key={bikeNumber}
                      className="inline-flex items-center rounded bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-700"
                    >
                      {bikeNumber}
                    </span>
                  ))}
                </div>
              </td>
              <td className="whitespace-nowrap px-4 py-3">
                <div className="text-sm text-slate-900">
                  {formatDueDate(rental.dueAt)}
                </div>
                <div className={`text-xs ${rental.status === 'OVERDUE' ? 'font-medium text-rose-600' : 'text-slate-500'}`}>
                  {getTimeRelative(rental.dueAt, rental.status)}
                </div>
              </td>
              <td className="whitespace-nowrap px-4 py-3">
                <Badge variant={rental.status === 'OVERDUE' ? 'error' : 'info'}>
                  {rental.status}
                </Badge>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

