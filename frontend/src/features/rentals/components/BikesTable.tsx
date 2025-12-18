/**
 * BikesTable - Table displaying rental bikes with selection and actions
 */

import { BikeItemRow } from './BikeItemRow'
import type { RentalItem } from '../types'

export interface BikesTableProps {
  items: RentalItem[]
  selectedItems: Set<number>
  rentedItems: RentalItem[]
  showCheckbox: boolean
  showActions: boolean
  isLoading: boolean
  onToggleAll: () => void
  onToggleSelect: (itemId: number) => void
  onReturn: (item: RentalItem) => void
  onMarkLost: (item: RentalItem) => void
}

export function BikesTable({
  items,
  selectedItems,
  rentedItems,
  showCheckbox,
  showActions,
  isLoading,
  onToggleAll,
  onToggleSelect,
  onReturn,
  onMarkLost,
}: BikesTableProps) {
  return (
    <div className="overflow-hidden rounded-lg border border-slate-200">
      <table className="min-w-full divide-y divide-slate-200">
        <thead className="bg-slate-50">
          <tr>
            {showCheckbox && (
              <th className="w-12 px-4 py-3">
                <input
                  type="checkbox"
                  checked={selectedItems.size === rentedItems.length && rentedItems.length > 0}
                  onChange={onToggleAll}
                  className="h-4 w-4 rounded border-slate-300 text-sky-600 focus:ring-sky-500"
                />
              </th>
            )}
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
            {showActions && (
              <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-slate-500">
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100 bg-white">
          {items.map(item => (
            <BikeItemRow
              key={item.rentalItemId}
              item={item}
              isSelected={selectedItems.has(item.rentalItemId)}
              showCheckbox={showCheckbox}
              showActions={showActions}
              isLoading={isLoading}
              onToggleSelect={onToggleSelect}
              onReturn={onReturn}
              onMarkLost={onMarkLost}
            />
          ))}
        </tbody>
      </table>
    </div>
  )
}

