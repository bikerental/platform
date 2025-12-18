/**
 * ReturnConfirmContent - Content for return confirmation dialogs
 */

import type { RentalItem } from '../types'

export interface ReturnConfirmContentProps {
  type: 'single' | 'selected' | 'all'
  item?: RentalItem
  items?: RentalItem[]
}

export function ReturnConfirmContent({ type, item, items }: ReturnConfirmContentProps) {
  if (type === 'single' && item) {
    return (
      <p className="text-slate-600">
        Are you sure you want to return bike{' '}
        <span className="font-mono font-semibold">{item.bikeNumber}</span>?
      </p>
    )
  }

  if (type === 'selected' && items) {
    return (
      <div className="space-y-3">
        <p className="text-slate-600">
          Are you sure you want to return these {items.length} bike(s)?
        </p>
        <BikeList items={items} />
      </div>
    )
  }

  if (type === 'all' && items) {
    return (
      <div className="space-y-3">
        <p className="text-slate-600">
          Are you sure you want to return all {items.length} remaining bike(s)?
          This will close the rental.
        </p>
        <BikeList items={items} scrollable />
      </div>
    )
  }

  return null
}

function BikeList({ items, scrollable }: { items: RentalItem[]; scrollable?: boolean }) {
  return (
    <ul className={`rounded-lg bg-slate-50 p-3 ${scrollable ? 'max-h-48 overflow-y-auto' : ''}`}>
      {items.map(item => (
        <li key={item.rentalItemId} className="flex items-center gap-2 py-1">
          <span className="font-mono text-sm font-semibold">{item.bikeNumber}</span>
          {item.bikeType && <span className="text-sm text-slate-500">({item.bikeType})</span>}
        </li>
      ))}
    </ul>
  )
}

