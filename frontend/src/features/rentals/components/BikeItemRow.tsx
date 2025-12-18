/**
 * BikeItemRow - Single bike row in the rental bikes table
 */

import { Badge } from '@/components/ui/Badge'
import type { RentalItem, RentalItemStatus } from '../types'

export interface BikeItemRowProps {
  item: RentalItem
  isSelected: boolean
  showCheckbox: boolean
  showActions: boolean
  isLoading: boolean
  onToggleSelect: (itemId: number) => void
  onReturn: (item: RentalItem) => void
  onMarkLost: (item: RentalItem) => void
}

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

function formatReturnedAt(isoString: string): string {
  const date = new Date(isoString)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

export function BikeItemRow({
  item,
  isSelected,
  showCheckbox,
  showActions,
  isLoading,
  onToggleSelect,
  onReturn,
  onMarkLost,
}: BikeItemRowProps) {
  return (
    <tr className="hover:bg-slate-50">
      {/* Checkbox */}
      {showCheckbox && (
        <td className="w-12 px-4 py-4">
          {item.status === 'RENTED' && (
            <input
              type="checkbox"
              checked={isSelected}
              onChange={() => onToggleSelect(item.rentalItemId)}
              className="h-4 w-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500"
            />
          )}
        </td>
      )}

      {/* Bike Number */}
      <td className="whitespace-nowrap px-4 py-4">
        <span className="font-mono text-sm font-semibold text-slate-900">
          {item.bikeNumber}
        </span>
      </td>

      {/* Bike Type */}
      <td className="whitespace-nowrap px-4 py-4">
        <span className="text-sm text-slate-600">{item.bikeType || '-'}</span>
      </td>

      {/* Status Badge */}
      <td className="whitespace-nowrap px-4 py-4">
        <Badge variant={getItemStatusBadgeVariant(item.status)}>{item.status}</Badge>
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
              {item.lostReason && <p className="mt-0.5 text-slate-600">{item.lostReason}</p>}
            </div>
          </div>
        )}

        {item.status === 'RENTED' && <span className="text-sm text-slate-500">Currently out</span>}
      </td>

      {/* Actions */}
      {showActions && (
        <td className="whitespace-nowrap px-4 py-4 text-right">
          {item.status === 'RENTED' ? (
            <div className="flex justify-end gap-2">
              <button
                onClick={() => onReturn(item)}
                disabled={isLoading}
                className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition-colors hover:bg-emerald-700 disabled:opacity-50"
              >
                <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                </svg>
                Return
              </button>
              <button
                onClick={() => onMarkLost(item)}
                disabled={isLoading}
                className="inline-flex items-center gap-1.5 rounded-lg border border-rose-300 bg-white px-3 py-1.5 text-xs font-semibold text-rose-700 shadow-sm transition-colors hover:bg-rose-50 disabled:opacity-50"
              >
                <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                Mark Lost
              </button>
            </div>
          ) : (
            <span className="text-sm text-slate-400">—</span>
          )}
        </td>
      )}
    </tr>
  )
}

