/**
 * RentalBikesList - Displays bikes in a rental with return functionality
 * Orchestrates multi-select, return operations, and undo (always available for returned bikes)
 */

import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Toast } from '@/components/ui/Toast'
import { useToast } from '@/lib/hooks/useToast'
import { returnBike, undoReturnBike, returnSelected, returnAll } from '../api/rentalApi'
import { BikesActionBar } from './BikesActionBar'
import { BikesTable } from './BikesTable'
import { ReturnConfirmContent } from './ReturnConfirmContent'
import type { RentalItem, RentalStatus } from '../types'

export interface RentalBikesListProps {
  rentalId: number
  items: RentalItem[]
  rentalStatus: RentalStatus
  onRefresh: () => void
}

type ConfirmDialogState = {
  type: 'single' | 'selected' | 'all'
  item?: RentalItem
  items?: RentalItem[]
} | null

export function RentalBikesList({ rentalId, items, rentalStatus, onRefresh }: RentalBikesListProps) {
  const [selectedItems, setSelectedItems] = useState<Set<number>>(new Set())
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [confirmDialog, setConfirmDialog] = useState<ConfirmDialogState>(null)

  const { toast, showToast, hideToast } = useToast()

  const rentedItems = items.filter(item => item.status === 'RENTED')
  const isClosed = rentalStatus === 'CLOSED'
  const showCheckbox = !isClosed && rentedItems.length > 0
  const selectedRentedItems = items.filter(i => selectedItems.has(i.rentalItemId) && i.status === 'RENTED')

  const handleReturnSingle = async (item: RentalItem) => {
    setConfirmDialog(null)
    setIsLoading(true)
    setError(null)
    try {
      await returnBike(rentalId, item.rentalItemId)
      showToast(`Bike ${item.bikeNumber} returned`, 'success')
      onRefresh()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to return bike')
      showToast('Failed to return bike', 'error')
    } finally {
      setIsLoading(false)
    }
  }

  /**
   * Undo a bike return - always available for any returned bike while rental is active
   */
  const handleUndoReturn = async (item: RentalItem) => {
    setIsLoading(true)
    setError(null)
    try {
      await undoReturnBike(rentalId, item.rentalItemId)
      showToast(`Return of bike ${item.bikeNumber} undone`, 'success')
      onRefresh()
    } catch {
      showToast('Failed to undo return', 'error')
    } finally {
      setIsLoading(false)
    }
  }

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

  const toggleSelection = (itemId: number) => {
    const newSelection = new Set(selectedItems)
    newSelection.has(itemId) ? newSelection.delete(itemId) : newSelection.add(itemId)
    setSelectedItems(newSelection)
  }

  const toggleAll = () => {
    setSelectedItems(selectedItems.size === rentedItems.length ? new Set() : new Set(rentedItems.map(i => i.rentalItemId)))
  }

  const handleMarkLost = (item: RentalItem) => {
    alert(`Mark lost functionality coming in Phase 10 for bike ${item.bikeNumber}`)
  }

  const handleConfirm = () => {
    if (confirmDialog?.type === 'single' && confirmDialog.item) handleReturnSingle(confirmDialog.item)
    else if (confirmDialog?.type === 'selected') handleReturnSelected()
    else if (confirmDialog?.type === 'all') handleReturnAll()
  }

  if (items.length === 0) {
    return <div className="py-8 text-center text-slate-500">No bikes in this rental</div>
  }

  return (
    <div className="space-y-4">
      {error && <div className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{error}</div>}

      {!isClosed && (
        <BikesActionBar
          selectedCount={selectedItems.size}
          rentedCount={rentedItems.length}
          isLoading={isLoading}
          onReturnSelected={() => setConfirmDialog({ type: 'selected', items: selectedRentedItems })}
          onReturnAll={() => setConfirmDialog({ type: 'all', items: rentedItems })}
        />
      )}

      <BikesTable
        items={items}
        selectedItems={selectedItems}
        rentedItems={rentedItems}
        showCheckbox={showCheckbox}
        showActions={!isClosed}
        isLoading={isLoading}
        onToggleAll={toggleAll}
        onToggleSelect={toggleSelection}
        onReturn={item => setConfirmDialog({ type: 'single', item })}
        onMarkLost={handleMarkLost}
        onUndoReturn={handleUndoReturn}
      />

      <Modal
        isOpen={confirmDialog !== null}
        onClose={() => setConfirmDialog(null)}
        title={confirmDialog?.type === 'single' ? 'Return Bike' : confirmDialog?.type === 'selected' ? 'Return Selected Bikes' : 'Return All Bikes'}
        footer={
          <>
            <button onClick={() => setConfirmDialog(null)} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Cancel</button>
            <button onClick={handleConfirm} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700">Confirm Return</button>
          </>
        }
      >
        {confirmDialog && <ReturnConfirmContent type={confirmDialog.type} item={confirmDialog.item} items={confirmDialog.items} />}
      </Modal>

      {toast && <Toast message={toast.message} type={toast.type} onClose={hideToast} />}
    </div>
  )
}
