/**
 * UndoBanner - Shows undo option with countdown after a bike return
 */

export interface UndoBannerProps {
  bikeNumber: string
  timeLeftMs: number
  isLoading: boolean
  onUndo: () => void
}

export function UndoBanner({ bikeNumber, timeLeftMs, isLoading, onUndo }: UndoBannerProps) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-emerald-200 bg-emerald-50 p-3">
      <span className="text-sm text-emerald-800">Bike {bikeNumber} returned</span>
      <div className="flex items-center gap-3">
        <span className="text-xs text-emerald-600">{Math.ceil(timeLeftMs / 1000)}s</span>
        <button
          onClick={onUndo}
          disabled={isLoading}
          className="text-sm font-semibold text-emerald-700 underline hover:no-underline"
        >
          Undo
        </button>
      </div>
    </div>
  )
}

