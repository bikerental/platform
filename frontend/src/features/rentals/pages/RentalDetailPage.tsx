/**
 * RentalDetailPage - Displays detailed rental information
 */

import { useParams, useNavigate } from 'react-router-dom'
import { Badge } from '@/components/ui/Badge'
import { useRentalDetail } from '../hooks/useRentalDetail'
import { RentalBikesList } from '../components/RentalBikesList'
import { RentalInfoCards } from '../components/RentalInfoCards'
import { SignaturePreview } from '../components/SignaturePreview'
import { getContractUrl } from '../api/rentalApi'
import { getToken } from '@/lib/api'
import type { RentalStatus } from '../types'

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

export function RentalDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const rentalId = id ? parseInt(id, 10) : null
  const { rental, signatureUrl, isLoading, error, refresh } = useRentalDetail(rentalId)

  const handleViewContract = () => {
    if (!rentalId) return
    const contractUrl = getContractUrl(rentalId)
    const token = getToken()

    const authWindow = window.open('', '_blank')
    if (authWindow) {
      fetch(contractUrl, { headers: { Authorization: `Bearer ${token || ''}` } })
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
        <BackButton onClick={() => navigate(-1)} />
        <div className="rounded-xl border border-rose-200 bg-rose-50 p-8 text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-rose-100">
            <svg className="h-8 w-8 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h3 className="text-lg font-semibold text-slate-900">{error || 'Rental not found'}</h3>
          <p className="mt-2 text-slate-600">The rental you're looking for doesn't exist or you don't have access to it.</p>
          <button onClick={refresh} className="mt-4 inline-flex items-center gap-2 rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-700">
            Try Again
          </button>
        </div>
      </div>
    )
  }

  const bikesOut = rental.items.filter(item => item.status === 'RENTED').length

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-start gap-4">
          <BackButton onClick={() => navigate(-1)} />
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold tracking-tight text-slate-900">Rental #{rental.rentalId}</h1>
              <Badge variant={getStatusBadgeVariant(rental.status)}>{rental.status}</Badge>
            </div>
            <p className="mt-1 text-sm text-slate-600">
              Room {rental.roomNumber}
              {rental.bedNumber && ` • Bed ${rental.bedNumber}`}
            </p>
          </div>
        </div>

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

      <RentalInfoCards
        startAt={rental.startAt}
        dueAt={rental.dueAt}
        status={rental.status}
        bikesOut={bikesOut}
        bikesTotal={rental.items.length}
        tncVersion={rental.tncVersion}
      />

      {/* Bikes List */}
      <div className="rounded-xl border border-slate-200 bg-white">
        <div className="border-b border-slate-200 px-6 py-4">
          <h2 className="text-lg font-semibold text-slate-900">Bikes</h2>
        </div>
        <div className="p-6">
          <RentalBikesList rentalId={rental.rentalId} items={rental.items} rentalStatus={rental.status} onRefresh={refresh} />
        </div>
      </div>

      <SignaturePreview signatureUrl={signatureUrl} />
    </div>
  )
}

function BackButton({ onClick }: { onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className="mt-1 inline-flex items-center justify-center rounded-lg border border-slate-300 bg-white p-2 text-slate-600 transition-colors hover:bg-slate-50 hover:text-slate-900"
      aria-label="Go back"
    >
      <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
      </svg>
    </button>
  )
}
