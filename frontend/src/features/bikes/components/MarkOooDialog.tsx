/**
 * MarkOooDialog component - modal form for marking a bike as Out of Order
 */

import { useState, useEffect } from 'react'
import { Modal } from '@/components/ui/Modal'
import type { Bike } from '../types'

export interface MarkOooDialogProps {
  bike: Bike | null
  isOpen: boolean
  onClose: () => void
  onConfirm: (note: string) => Promise<void>
}

export function MarkOooDialog({
  bike,
  isOpen,
  onClose,
  onConfirm,
}: MarkOooDialogProps) {
  const [note, setNote] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  // Reset note when dialog opens/closes
  useEffect(() => {
    if (!isOpen) {
      setNote('')
    }
  }, [isOpen])

  const handleSubmit = async () => {
    if (!note.trim()) return

    setIsSubmitting(true)
    try {
      await onConfirm(note.trim())
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!bike) return null

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={`Mark Bike ${bike.bikeNumber} as Out of Order`}
      footer={
        <>
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={!note.trim() || isSubmitting}
            className="px-4 py-2 text-sm font-medium text-white bg-orange-600 rounded-lg hover:bg-orange-700 disabled:bg-orange-300 disabled:cursor-not-allowed transition-colors"
          >
            {isSubmitting ? 'Submitting...' : 'Mark OOO'}
          </button>
        </>
      }
    >
      <div>
        <label className="block text-sm font-medium text-slate-700 mb-2">
          Reason / Note <span className="text-red-500">*</span>
        </label>
        <textarea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="Enter reason for marking bike as OOO..."
          rows={4}
          className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
          autoFocus
        />
      </div>
    </Modal>
  )
}

