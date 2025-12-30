/**
 * BikesPage - orchestrates bike inventory page
 */

import { useState } from 'react'
import { useBikes } from '../hooks/useBikes'
import { BikesTable } from '../components/BikesTable'
import { MarkOooDialog } from '../components/MarkOooDialog'
import { exportOooBikesExcel } from '../api/bikeApi'
import { useToast } from '@/lib/hooks/useToast'
import { Toast } from '@/components/ui/Toast'
import type { Bike, BikeStatus } from '../types'

export function BikesPage() {
  const {
    bikes,
    isLoading,
    error,
    statusFilter,
    searchQuery,
    setStatusFilter,
    setSearchQuery,
    handleMarkOoo,
    handleMarkAvailable,
  } = useBikes()

  const [selectedBike, setSelectedBike] = useState<Bike | null>(null)
  const [showOooDialog, setShowOooDialog] = useState(false)
  const [isExporting, setIsExporting] = useState(false)
  const { toast, showToast, hideToast } = useToast()

  const openOooDialog = (bike: Bike) => {
    setSelectedBike(bike)
    setShowOooDialog(true)
  }

  const closeOooDialog = () => {
    setShowOooDialog(false)
    setSelectedBike(null)
  }

  const handleConfirmOoo = async (note: string) => {
    if (!selectedBike) return
    await handleMarkOoo(selectedBike, note)
    closeOooDialog()
  }

  const handleExportOoo = async () => {
    setIsExporting(true)
    try {
      await exportOooBikesExcel()
      showToast('OOO bikes exported successfully', 'success')
    } catch (err) {
      showToast(
        err instanceof Error ? err.message : 'Failed to export OOO bikes',
        'error'
      )
    } finally {
      setIsExporting(false)
    }
  }

  // Show export button when viewing OOO bikes
  const showExportButton = statusFilter === 'OOO'

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Bike Inventory</h1>
        {showExportButton && (
          <button
            onClick={handleExportOoo}
            disabled={isExporting}
            className="inline-flex items-center gap-2 px-4 py-2 bg-slate-700 text-white rounded-lg hover:bg-slate-800 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {isExporting ? (
              <>
                <svg
                  className="animate-spin h-4 w-4"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                Exporting...
              </>
            ) : (
              <>
                <svg
                  className="h-4 w-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
                Export OOO to Excel
              </>
            )}
          </button>
        )}
      </div>

      {/* Error Message */}
      {error && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-red-700 text-sm">{error}</p>
        </div>
      )}

      {/* Filters */}
      <div className="bg-white rounded-lg border border-slate-200 p-4 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Status Filter */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              Status Filter
            </label>
            <select
              value={statusFilter}
              onChange={(e) =>
                setStatusFilter(e.target.value as BikeStatus | 'ALL')
              }
              className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            >
              <option value="ALL">All Statuses</option>
              <option value="AVAILABLE">Available</option>
              <option value="RENTED">Rented</option>
              <option value="OOO">Out of Order</option>
            </select>
          </div>

          {/* Search */}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-2">
              Search by Bike Number
            </label>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Enter bike number..."
              className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
            />
          </div>
        </div>
      </div>

      {/* Bike Table */}
      <BikesTable
        bikes={bikes}
        isLoading={isLoading}
        onMarkOoo={openOooDialog}
        onMarkAvailable={handleMarkAvailable}
      />

      {/* Mark OOO Dialog */}
      <MarkOooDialog
        bike={selectedBike}
        isOpen={showOooDialog}
        onClose={closeOooDialog}
        onConfirm={handleConfirmOoo}
      />

      {/* Toast notifications */}
      {toast && (
        <Toast message={toast.message} type={toast.type} onClose={hideToast} />
      )}
    </div>
  )
}

