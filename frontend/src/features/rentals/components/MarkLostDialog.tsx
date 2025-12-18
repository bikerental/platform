/**
 * MarkLostDialog - Confirmation dialog for marking a bike as lost
 * Includes optional reason text input
 */

import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import type { RentalItem } from '../types'

export interface MarkLostDialogProps {
  isOpen: boolean
  item: RentalItem | null
  isLoading: boolean
  onClose: () => void
  onConfirm: (item: RentalItem, reason: string) => void
}

export function MarkLostDialog({
  isOpen,
  item,
  isLoading,
  onClose,
  onConfirm,
}: MarkLostDialogProps) {
  const [reason, setReason] = useState('')

  const handleConfirm = () => {
    if (item) {
      onConfirm(item, reason.trim())
      setReason('')
    }
  }

  const handleClose = () => {
    setReason('')
    onClose()
  }

  if (!item) return null

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title="Mark Bike as Lost"
      footer={
        <>
          <button
            onClick={handleClose}
            disabled={isLoading}
            className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            Cancel
          </button>
          <button
            onClick={handleConfirm}
            disabled={isLoading}
            className="inline-flex items-center gap-2 rounded-lg bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-700 disabled:opacity-50"
          >
            {isLoading ? (
              <>
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                Marking Lost...
              </>
            ) : (
              'Mark as Lost'
            )}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        {/* Warning message */}
        <div className="flex items-start gap-3 rounded-lg border border-rose-200 bg-rose-50 p-4">
          <svg
            className="mt-0.5 h-5 w-5 flex-shrink-0 text-rose-500"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
          <div className="text-sm">
            <p className="font-medium text-rose-800">
              This action cannot be undone
            </p>
            <p className="mt-1 text-rose-700">
              Marking a bike as lost will set it as out of order and close the rental item.
            </p>
          </div>
        </div>

        {/* Bike info */}
        <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-slate-200">
              <svg
                className="h-5 w-5 text-slate-600"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 110-4m0 4v2m0-6V4"
                />
              </svg>
            </div>
            <div>
              <p className="font-mono text-sm font-semibold text-slate-900">
                {item.bikeNumber}
              </p>
              <p className="text-xs text-slate-600">
                {item.bikeType || 'Standard bike'}
              </p>
            </div>
          </div>
        </div>

        {/* Reason input */}
        <div>
          <label
            htmlFor="lost-reason"
            className="block text-sm font-medium text-slate-700"
          >
            Reason (optional)
          </label>
          <textarea
            id="lost-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="e.g., Guest left without returning, bike was stolen..."
            rows={3}
            className="mt-1.5 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm placeholder:text-slate-400 focus:border-rose-500 focus:outline-none focus:ring-1 focus:ring-rose-500"
          />
        </div>
      </div>
    </Modal>
  )
}

