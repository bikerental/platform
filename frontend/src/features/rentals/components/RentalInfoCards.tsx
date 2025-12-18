/**
 * RentalInfoCards - Displays rental summary info in card grid
 */

import type { RentalStatus } from '../types'

export interface RentalInfoCardsProps {
  startAt: string
  dueAt: string
  status: RentalStatus
  bikesOut: number
  bikesTotal: number
  tncVersion: string
}

function formatDateTime(isoString: string): string {
  const date = new Date(isoString)
  return date.toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

function getTimeStatus(dueAt: string, status: RentalStatus): { text: string; isUrgent: boolean } {
  const now = new Date()
  const dueDate = new Date(dueAt)
  const diffMs = dueDate.getTime() - now.getTime()
  const diffMins = Math.round(diffMs / (1000 * 60))
  const diffHours = Math.round(diffMs / (1000 * 60 * 60))
  const diffDays = Math.round(diffMs / (1000 * 60 * 60 * 24))

  if (status === 'CLOSED') return { text: 'Completed', isUrgent: false }

  if (status === 'OVERDUE' || diffMins <= 0) {
    const overdueMins = Math.abs(diffMins)
    if (overdueMins < 60) return { text: `${overdueMins}m overdue`, isUrgent: true }
    const overdueHours = Math.abs(diffHours)
    if (overdueHours < 24) return { text: `${overdueHours}h overdue`, isUrgent: true }
    return { text: `${Math.abs(diffDays)}d overdue`, isUrgent: true }
  }

  if (diffMins < 60) return { text: `Due in ${diffMins}m`, isUrgent: diffMins < 30 }
  if (diffHours < 24) return { text: `Due in ${diffHours}h`, isUrgent: diffHours < 2 }
  return { text: `Due in ${diffDays}d`, isUrgent: false }
}

export function RentalInfoCards({ startAt, dueAt, status, bikesOut, bikesTotal, tncVersion }: RentalInfoCardsProps) {
  const timeStatus = getTimeStatus(dueAt, status)

  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">Started</div>
        <div className="mt-1 text-lg font-semibold text-slate-900">{formatDateTime(startAt)}</div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">Due</div>
        <div className="mt-1 text-lg font-semibold text-slate-900">{formatDateTime(dueAt)}</div>
        <div className={`mt-1 text-sm font-medium ${timeStatus.isUrgent ? 'text-rose-600' : 'text-slate-500'}`}>
          {timeStatus.text}
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">Bikes</div>
        <div className="mt-1 text-lg font-semibold text-slate-900">
          {bikesOut} / {bikesTotal} <span className="text-sm font-normal text-slate-500">out</span>
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4">
        <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">T&C Version</div>
        <div className="mt-1 text-lg font-semibold text-slate-900">v{tncVersion}</div>
      </div>
    </div>
  )
}

