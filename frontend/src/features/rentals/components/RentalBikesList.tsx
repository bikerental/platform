/**
 * RentalBikesList - Displays bikes in a rental with status-specific UI
 * Shows return/mark lost buttons for rented items, timestamps for returned, reason for lost
 * Includes multi-select, return-all, and undo functionality
 */

import { useState, useEffect, useCallback } from 'react'
import { Badge } from '@/components/ui/Badge'
import { Modal } from '@/components/ui/Modal'
import { returnBike, undoReturnBike, returnSelected, returnAll } from '../api/rentalApi'
import type { RentalItem, RentalItemStatus, RentalStatus } from '../types'

export interface RentalBikesListProps {
  rentalId: number
  items: RentalItem[]
  rentalStatus: RentalStatus
  onRefresh: () => void
}

interface UndoState {
  rentalItemId: number
  bikeNumber: string
  expiresAt: number
}

/**
 * Get badge variant based on item status
 */
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

/**
 * Format a timestamp for display
 */
function formatReturnedAt(isoString: string): string {
  const date = new Date(isoString)
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

const UNDO_WINDOW_MS = 30000 // 30 seconds

export function RentalBikesList({ rentalId, items, rentalStatus, onRefresh }: RentalBikesListProps) {
  const [selectedItems, setSelectedItems] = useState<Set<number>>(new Set())
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  // Confirmation dialog state
  const [confirmDialog, setConfirmDialog] = useState<{
    type: 'single' | 'selected' | 'all'
    item?: RentalItem
    items?: RentalItem[]
  } | null>(null)
  
  // Undo state
  const [undoState, setUndoState] = useState<UndoState | null>(null)
  const [undoTimeLeft, setUndoTimeLeft] = useState(0)

  // Toast notification state
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null)

  const rentedItems = items.filter(item => item.status === 'RENTED')
  const isClosed = rentalStatus === 'CLOSED'

  // Undo timer effect
  useEffect(() => {
    if (!undoState) return

    const interval = setInterval(() => {
      const timeLeft = Math.max(0, undoState.expiresAt - Date.now())
      setUndoTimeLeft(timeLeft)
      
      if (timeLeft <= 0) {
        setUndoState(null)
      }
    }, 100)

    return () => clearInterval(interval)
  }, [undoState])

  // Auto-hide toast
  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(null), 4000)
    return () => clearTimeout(timer)
  }, [toast])

  const showToast = useCallback((message: string, type: 'success' | 'error') => {
    setToast({ message, type })
  }, [])

  // Single bike return
  const handleReturnSingle = async (item: RentalItem) => {
    setConfirmDialog(null)
    setIsLoading(true)
    setError(null)
    
    try {
      await returnBike(rentalId, item.rentalItemId)
      
      // Set up undo state
      setUndoState({
        rentalItemId: item.rentalItemId,
        bikeNumber: item.bikeNumber,
        expiresAt: Date.now() + UNDO_WINDOW_MS,
      })
      
      showToast(`Bike ${item.bikeNumber} returned`, 'success')
      onRefresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to return bike')
      showToast('Failed to return bike', 'error')
    } finally {
      setIsLoading(false)
    }
  }

  // Undo return
  const handleUndo = async () => {
    if (!undoState) return
    
    setIsLoading(true)
    try {
      await undoReturnBike(rentalId, undoState.rentalItemId)
      showToast(`Return of ${undoState.bikeNumber} undone`, 'success')
      setUndoState(null)
      onRefresh()
    } catch (err) {
      showToast('Failed to undo return', 'error')
    } finally {
      setIsLoading(false)
    }
  }

  // Return selected bikes
  const handleReturnSelected = async () => {
    setConfirmDialog(null)
    setIsLoading(true)
    setError(null)
    
    try {
      const result = await returnSelected(rentalId, Array.from(selectedItems))
      setSelectedItems(new Set())
      showToast(`${result.returnedCount} bike(s) returned`, 'success')
      onRefresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to return bikes')
      showToast('Failed to return bikes', 'error')
    } finally {
      setIsLoading(false)
    }
  }

  // Return all bikes
  const handleReturnAll = async () => {
    setConfirmDialog(null)
    setIsLoading(true)
    setError(null)
    
    try {
      const result = await returnAll(rentalId)
      showToast(`All ${result.returnedCount} bike(s) returned. Rental closed.`, 'success')
      onRefresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to return bikes')
      showToast('Failed to return bikes', 'error')
    } finally {
      setIsLoading(false)
    }
  }

  // Toggle selection
  const toggleSelection = (itemId: number) => {
    const newSelection = new Set(selectedItems)
    if (newSelection.has(itemId)) {
      newSelection.delete(itemId)
    } else {
      newSelection.add(itemId)
    }
    setSelectedItems(newSelection)
  }

  // Toggle all selection
  const toggleAll = () => {
    if (selectedItems.size === rentedItems.length) {
      setSelectedItems(new Set())
    } else {
      setSelectedItems(new Set(rentedItems.map(i => i.rentalItemId)))
    }
  }

  // Mark lost handler (placeholder - Phase 10)
  const handleMarkLost = (item: RentalItem) => {
    console.log('Mark lost:', item.bikeNumber)
    alert(`Mark lost functionality coming in Phase 10 for bike ${item.bikeNumber}`)
  }

  if (items.length === 0) {
    return (
      <div className="py-8 text-center text-slate-500">
        No bikes in this rental
      </div>
    )
  }

  const selectedRentedItems = items.filter(
    i => selectedItems.has(i.rentalItemId) && i.status === 'RENTED'
  )

  return (
    <div className="space-y-4">
      {/* Error message */}
      {error && (
        <div className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">
          {error}
        </div>
      )}

      {/* Action bar */}
      {!isClosed && rentedItems.length > 0 && (
        <div className="flex flex-wrap items-center gap-3 rounded-lg bg-slate-50 p-3">
          <span className="text-sm text-slate-600">
            {selectedItems.size > 0 
              ? `${selectedItems.size} selected` 
              : `${rentedItems.length} bike(s) out`}
          </span>
          
          <div className="ml-auto flex gap-2">
            {selectedItems.size > 0 && (
              <button
                onClick={() => setConfirmDialog({ 
                  type: 'selected', 
                  items: selectedRentedItems 
                })}
                disabled={isLoading}
                className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-emerald-700 disabled:opacity-50"
              >
                Return Selected ({selectedItems.size})
              </button>
            )}
            
            <button
              onClick={() => setConfirmDialog({ type: 'all', items: rentedItems })}
              disabled={isLoading || rentedItems.length === 0}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 shadow-sm transition-colors hover:bg-slate-50 disabled:opacity-50"
            >
              Return All ({rentedItems.length})
            </button>
          </div>
        </div>
      )}

      {/* Undo toast */}
      {undoState && undoTimeLeft > 0 && (
        <div className="flex items-center justify-between rounded-lg border border-emerald-200 bg-emerald-50 p-3">
          <span className="text-sm text-emerald-800">
            Bike {undoState.bikeNumber} returned
          </span>
          <div className="flex items-center gap-3">
            <span className="text-xs text-emerald-600">
              {Math.ceil(undoTimeLeft / 1000)}s
            </span>
            <button
              onClick={handleUndo}
              disabled={isLoading}
              className="text-sm font-semibold text-emerald-700 underline hover:no-underline"
            >
              Undo
            </button>
          </div>
        </div>
      )}

      {/* Toast notification */}
      {toast && (
        <div className={`fixed bottom-4 right-4 z-50 rounded-lg px-4 py-3 shadow-lg ${
          toast.type === 'success' 
            ? 'bg-emerald-600 text-white' 
            : 'bg-rose-600 text-white'
        }`}>
          {toast.message}
        </div>
      )}

      {/* Bikes table */}
      <div className="overflow-hidden rounded-lg border border-slate-200">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>
              {/* Checkbox column */}
              {!isClosed && rentedItems.length > 0 && (
                <th className="w-12 px-4 py-3">
                  <input
                    type="checkbox"
                    checked={selectedItems.size === rentedItems.length && rentedItems.length > 0}
                    onChange={toggleAll}
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
              {!isClosed && (
                <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-slate-500">
                  Actions
                </th>
              )}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 bg-white">
            {items.map((item) => (
              <tr key={item.rentalItemId} className="hover:bg-slate-50">
                {/* Checkbox */}
                {!isClosed && rentedItems.length > 0 && (
                  <td className="w-12 px-4 py-4">
                    {item.status === 'RENTED' && (
                      <input
                        type="checkbox"
                        checked={selectedItems.has(item.rentalItemId)}
                        onChange={() => toggleSelection(item.rentalItemId)}
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
                  <span className="text-sm text-slate-600">
                    {item.bikeType || '-'}
                  </span>
                </td>

                {/* Status Badge */}
                <td className="whitespace-nowrap px-4 py-4">
                  <Badge variant={getItemStatusBadgeVariant(item.status)}>
                    {item.status}
                  </Badge>
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
                        {item.lostReason && (
                          <p className="mt-0.5 text-slate-600">{item.lostReason}</p>
                        )}
                      </div>
                    </div>
                  )}
                  
                  {item.status === 'RENTED' && (
                    <span className="text-sm text-slate-500">Currently out</span>
                  )}
                </td>

                {/* Actions */}
                {!isClosed && (
                  <td className="whitespace-nowrap px-4 py-4 text-right">
                    {item.status === 'RENTED' && (
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => setConfirmDialog({ type: 'single', item })}
                          disabled={isLoading}
                          className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition-colors hover:bg-emerald-700 disabled:opacity-50"
                        >
                          <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                          </svg>
                          Return
                        </button>
                        <button
                          onClick={() => handleMarkLost(item)}
                          disabled={isLoading}
                          className="inline-flex items-center gap-1.5 rounded-lg border border-rose-300 bg-white px-3 py-1.5 text-xs font-semibold text-rose-700 shadow-sm transition-colors hover:bg-rose-50 disabled:opacity-50"
                        >
                          <svg className="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                          </svg>
                          Mark Lost
                        </button>
                      </div>
                    )}
                    
                    {item.status !== 'RENTED' && (
                      <span className="text-sm text-slate-400">—</span>
                    )}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Confirmation Dialog */}
      <Modal
        isOpen={confirmDialog !== null}
        onClose={() => setConfirmDialog(null)}
        title={
          confirmDialog?.type === 'single'
            ? 'Return Bike'
            : confirmDialog?.type === 'selected'
            ? 'Return Selected Bikes'
            : 'Return All Bikes'
        }
        footer={
          <>
            <button
              onClick={() => setConfirmDialog(null)}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              onClick={() => {
                if (confirmDialog?.type === 'single' && confirmDialog.item) {
                  handleReturnSingle(confirmDialog.item)
                } else if (confirmDialog?.type === 'selected') {
                  handleReturnSelected()
                } else if (confirmDialog?.type === 'all') {
                  handleReturnAll()
                }
              }}
              className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
            >
              Confirm Return
            </button>
          </>
        }
      >
        {confirmDialog?.type === 'single' && confirmDialog.item && (
          <p className="text-slate-600">
            Are you sure you want to return bike{' '}
            <span className="font-mono font-semibold">{confirmDialog.item.bikeNumber}</span>?
          </p>
        )}
        
        {confirmDialog?.type === 'selected' && confirmDialog.items && (
          <div className="space-y-3">
            <p className="text-slate-600">
              Are you sure you want to return these {confirmDialog.items.length} bike(s)?
            </p>
            <ul className="rounded-lg bg-slate-50 p-3">
              {confirmDialog.items.map(item => (
                <li key={item.rentalItemId} className="flex items-center gap-2 py-1">
                  <span className="font-mono text-sm font-semibold">{item.bikeNumber}</span>
                  {item.bikeType && (
                    <span className="text-sm text-slate-500">({item.bikeType})</span>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}
        
        {confirmDialog?.type === 'all' && confirmDialog.items && (
          <div className="space-y-3">
            <p className="text-slate-600">
              Are you sure you want to return all {confirmDialog.items.length} remaining bike(s)?
              This will close the rental.
            </p>
            <ul className="max-h-48 overflow-y-auto rounded-lg bg-slate-50 p-3">
              {confirmDialog.items.map(item => (
                <li key={item.rentalItemId} className="flex items-center gap-2 py-1">
                  <span className="font-mono text-sm font-semibold">{item.bikeNumber}</span>
                  {item.bikeType && (
                    <span className="text-sm text-slate-500">({item.bikeType})</span>
                  )}
                </li>
              ))}
            </ul>
          </div>
        )}
      </Modal>
    </div>
  )
}
