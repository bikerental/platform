/**
 * BikesActionBar - Action bar with selection count and return buttons
 */

export interface BikesActionBarProps {
  selectedCount: number
  rentedCount: number
  isLoading: boolean
  onReturnSelected: () => void
  onReturnAll: () => void
}

export function BikesActionBar({
  selectedCount,
  rentedCount,
  isLoading,
  onReturnSelected,
  onReturnAll,
}: BikesActionBarProps) {
  if (rentedCount === 0) return null

  return (
    <div className="flex flex-wrap items-center gap-3 rounded-lg bg-slate-50 p-3">
      <span className="text-sm text-slate-600">
        {selectedCount > 0 ? `${selectedCount} selected` : `${rentedCount} bike(s) out`}
      </span>
      <div className="ml-auto flex gap-2">
        {selectedCount > 0 && (
          <button
            onClick={onReturnSelected}
            disabled={isLoading}
            className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-emerald-700 disabled:opacity-50"
          >
            Return Selected ({selectedCount})
          </button>
        )}
        <button
          onClick={onReturnAll}
          disabled={isLoading || rentedCount === 0}
          className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 shadow-sm transition-colors hover:bg-slate-50 disabled:opacity-50"
        >
          Return All ({rentedCount})
        </button>
      </div>
    </div>
  )
}

