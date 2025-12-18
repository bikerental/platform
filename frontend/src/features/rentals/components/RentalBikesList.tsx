/**
 * RentalBikesList - Displays bikes in a rental with status-specific UI
 * Shows return/mark lost buttons for rented items, timestamps for returned, reason for lost
 */

import { Badge } from '@/components/ui/Badge'
import type { RentalItem, RentalItemStatus } from '../types'

export interface RentalBikesListProps {
  items: RentalItem[]
  onRefresh: () => void
}

/**
 * Get badge variant based on item status
 */
function getItemStatusBadgeVariant(status: RentalItemStatus): 'success' | 'info' | 'warning' | 'error' {
  switch (status) {
    case 'RENTED':
      return 'info'
    case 'RETURNED':
      return 'success'
    case 'LOST':
      return 'error'
    default:
      return 'info'
  }
}

/**
 * Format a timestamp for display
 */
function formatReturnedAt(isoString: string): string {
  const date = new Date(isoString)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

export function RentalBikesList({ items, onRefresh: _onRefresh }: RentalBikesListProps) {
  // Placeholder handlers - these would connect to actual API endpoints when implemented
  // _onRefresh will be used when return/mark lost APIs are implemented
  const handleReturn = (item: RentalItem) => {
    // TODO: Implement return bike API call (Phase 9) - then call _onRefresh()
    console.log('Return bike:', item.bikeNumber)
    alert(`Return bike functionality coming soon for bike ${item.bikeNumber}`)
  }

  const handleMarkLost = (item: RentalItem) => {
    // TODO: Implement mark lost API call (Phase 9) - then call _onRefresh()
    console.log('Mark lost:', item.bikeNumber)
    alert(`Mark lost functionality coming soon for bike ${item.bikeNumber}`)
  }

  if (items.length === 0) {
    return (
      <div className="py-8 text-center text-slate-500">
        No bikes in this rental
      </div>
    )
  }

  return (
    <div className="overflow-hidden rounded-lg border border-slate-200">
      <table className="min-w-full divide-y divide-slate-200">
        <thead className="bg-slate-50">
          <tr>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
              Bike
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
              Type
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
              Status
            </th>
            <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-500">
              Details
            </th>
            <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-slate-500">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {items.map((item) => (
            <tr key={item.rentalItemId} className="hover:bg-slate-50">
              {/* Bike Number */}
              <td className="whitespace-nowrap px-4 py-4">
                <span className="font-mono text-sm font-semibold text-slate-900">
                  {item.bikeNumber}
                </span>
              </td>

              {/* Bike Type */}
              <td className="whitespace-nowrap px-4 py-4">
                <span className="text-sm text-slate-600">
                  {item.bikeType || '-'}
                </span>
              </td>

              {/* Status Badge */}
              <td className="whitespace-nowrap px-4 py-4">
                <Badge variant={getItemStatusBadgeVariant(item.status)}>
                  {item.status}
                </Badge>
              </td>

              {/* Status Details */}
              <td className="px-4 py-4">
                {item.status === 'RETURNED' && item.returnedAt && (
                  <div className="flex items-center gap-2 text-sm text-slate-600">
                    <svg className="h-4 w-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                    </svg>
                    <span>Returned {formatReturnedAt(item.returnedAt)}</span>
                  </div>
                )}
                
                {item.status === 'LOST' && (
                  <div className="flex items-start gap-2 text-sm">
                    <svg className="mt-0.5 h-4 w-4 flex-shrink-0 text-rose-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                    <div>
                      <span className="font-medium text-rose-700">Marked Lost</span>
                      {item.lostReason && (
                        <p className="mt-0.5 text-slate-600">{item.lostReason}</p>
                      )}
                    </div>
                  </div>
                )}
                
                {item.status === 'RENTED' && (
                  <span className="text-sm text-slate-500">Currently out</span>
                )}
              </td>

              {/* Actions */}
              <td className="whitespace-nowrap px-4 py-4 text-right">
                {item.status === 'RENTED' && (
                  <div className="flex justify-end gap-2">
                    <button
                      onClick={() => handleReturn(item)}
                      className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition-colors hover:bg-emerald-700"
                    >
                      <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                      </svg>
                      Return
                    </button>
                    <button
                      onClick={() => handleMarkLost(item)}
                      className="inline-flex items-center gap-1.5 rounded-lg border border-rose-300 bg-white px-3 py-1.5 text-xs font-semibold text-rose-700 shadow-sm transition-colors hover:bg-rose-50"
                    >
                      <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                      </svg>
                      Mark Lost
                    </button>
                  </div>
                )}
                
                {item.status !== 'RENTED' && (
                  <span className="text-sm text-slate-400">—</span>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

