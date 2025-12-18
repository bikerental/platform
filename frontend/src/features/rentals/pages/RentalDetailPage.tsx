/**
 * RentalDetailPage - Displays detailed rental information
 * Includes rental info, bikes list, signature preview, and contract link
 */

import { useParams, useNavigate } from 'react-router-dom'
import { Badge } from '@/components/ui/Badge'
import { useRentalDetail } from '../hooks/useRentalDetail'
import { RentalBikesList } from '../components/RentalBikesList'
import { getContractUrl } from '../api/rentalApi'
import { getToken } from '@/lib/api'
import type { RentalStatus } from '../types'

/**
 * Format an ISO date string for display
 */
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

/**
 * Get badge variant based on rental status
 */
function getStatusBadgeVariant(status: RentalStatus): 'success' | 'info' | 'warning' | 'error' {
  switch (status) {
    case 'ACTIVE':
      return 'info'
    case 'OVERDUE':
      return 'error'
    case 'CLOSED':
      return 'success'
    default:
      return 'info'
  }
}

/**
 * Get time remaining or overdue text
 */
function getTimeStatus(dueAt: string, status: RentalStatus): { text: string; isUrgent: boolean } {
  const now = new Date()
  const dueDate = new Date(dueAt)
  const diffMs = dueDate.getTime() - now.getTime()
  const diffMins = Math.round(diffMs / (1000 * 60))
  const diffHours = Math.round(diffMs / (1000 * 60 * 60))
  const diffDays = Math.round(diffMs / (1000 * 60 * 60 * 24))

  if (status === 'CLOSED') {
    return { text: 'Completed', isUrgent: false }
  }

  if (status === 'OVERDUE' || diffMins <= 0) {
    const overdueMins = Math.abs(diffMins)
    if (overdueMins < 60) {
      return { text: `${overdueMins}m overdue`, isUrgent: true }
    }
    const overdueHours = Math.abs(diffHours)
    if (overdueHours < 24) {
      return { text: `${overdueHours}h overdue`, isUrgent: true }
    }
    return { text: `${Math.abs(diffDays)}d overdue`, isUrgent: true }
  }

  if (diffMins < 60) {
    return { text: `Due in ${diffMins}m`, isUrgent: diffMins < 30 }
  }
  if (diffHours < 24) {
    return { text: `Due in ${diffHours}h`, isUrgent: diffHours < 2 }
  }
  return { text: `Due in ${diffDays}d`, isUrgent: false }
}

export function RentalDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const rentalId = id ? parseInt(id, 10) : null
  
  const { rental, signatureUrl, isLoading, error, refresh } = useRentalDetail(rentalId)

  /**
   * Open contract in new tab (needs auth header, so we use a form-based approach)
   */
  const handleViewContract = () => {
    if (!rentalId) return
    const contractUrl = getContractUrl(rentalId)
    const token = getToken()
    
    // Create a form to POST with auth token, or open in new window for HTML
    // Since our endpoint returns HTML and accepts GET with auth, we'll open directly
    // For authenticated access, we need to handle this differently
    const authWindow = window.open('', '_blank')
    if (authWindow) {
      fetch(contractUrl, {
        headers: {
          Authorization: `Bearer ${token || ''}`,
        },
      })
        .then(res => res.text())
        .then(html => {
          authWindow.document.write(html)
          authWindow.document.close()
        })
        .catch(() => {
          authWindow.close()
          alert('Failed to load contract')
        })
    }
  }

  if (isLoading) {
    return (
      <div className="flex min-h-[400px] items-center justify-center">
        <div className="flex items-center gap-3">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-sky-500 border-t-transparent" />
          <span className="text-slate-600">Loading rental details...</span>
        </div>
      </div>
    )
  }

  if (error || !rental) {
    return (
      <div className="space-y-6">
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-900"
        >
          <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
          </svg>
          Back
        </button>
        
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-8 text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-rose-100">
            <svg className="h-8 w-8 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-slate-900">
            {error || 'Rental not found'}
          </h3>
          <p className="mt-2 text-slate-600">
            The rental you're looking for doesn't exist or you don't have access to it.
          </p>
          <button
            onClick={refresh}
            className="mt-4 inline-flex items-center gap-2 rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-700"
          >
            Try Again
          </button>
        </div>
      </div>
    )
  }

  const timeStatus = getTimeStatus(rental.dueAt, rental.status)
  const bikesOut = rental.items.filter(item => item.status === 'RENTED').length
  const bikesTotal = rental.items.length

  return (
    <div className="space-y-6">
      {/* Back Button & Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-4">
          <button
            onClick={() => navigate(-1)}
            className="mt-1 inline-flex items-center justify-center rounded-lg border border-slate-300 bg-white p-2 text-slate-600 transition-colors hover:bg-slate-50 hover:text-slate-900"
            aria-label="Go back"
          >
            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">
                Rental #{rental.rentalId}
              </h1>
              <Badge variant={getStatusBadgeVariant(rental.status)}>
                {rental.status}
              </Badge>
            </div>
            <p className="mt-1 text-sm text-slate-600">
              Room {rental.roomNumber}
              {rental.bedNumber && ` • Bed ${rental.bedNumber}`}
            </p>
          </div>
        </div>

        {/* Actions */}
        <div className="flex gap-3">
          <button
            onClick={handleViewContract}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm transition-colors hover:bg-slate-50"
          >
            <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            View Contract
          </button>
        </div>
      </div>

      {/* Info Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Start Time */}
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Started
          </div>
          <div className="mt-1 text-lg font-semibold text-slate-900">
            {formatDateTime(rental.startAt)}
          </div>
        </div>

        {/* Due Time */}
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Due
          </div>
          <div className="mt-1 text-lg font-semibold text-slate-900">
            {formatDateTime(rental.dueAt)}
          </div>
          <div className={`mt-1 text-sm font-medium ${timeStatus.isUrgent ? 'text-rose-600' : 'text-slate-500'}`}>
            {timeStatus.text}
          </div>
        </div>

        {/* Bikes Summary */}
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            Bikes
          </div>
          <div className="mt-1 text-lg font-semibold text-slate-900">
            {bikesOut} / {bikesTotal} <span className="text-sm font-normal text-slate-500">out</span>
          </div>
        </div>

        {/* T&C Version */}
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
            T&C Version
          </div>
          <div className="mt-1 text-lg font-semibold text-slate-900">
            v{rental.tncVersion}
          </div>
        </div>
      </div>

      {/* Bikes List */}
      <div className="rounded-xl border border-slate-200 bg-white">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Bikes</h2>
        </div>
        <div className="p-6">
          <RentalBikesList items={rental.items} onRefresh={refresh} />
        </div>
      </div>

      {/* Signature Section */}
      <div className="rounded-xl border border-slate-200 bg-white">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Guest Signature</h2>
        </div>
        <div className="p-6">
          {signatureUrl ? (
            <div className="inline-block rounded-lg border border-slate-200 bg-slate-50 p-4">
              <img
                src={signatureUrl}
                alt="Guest signature"
                className="max-h-32 max-w-xs"
              />
            </div>
          ) : (
            <div className="flex items-center gap-2 text-slate-500">
              <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              <span>Signature not available</span>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

