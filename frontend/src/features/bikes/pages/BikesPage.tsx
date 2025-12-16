/**
 * BikesPage - orchestrates bike inventory page
 */

import { useState } from 'react'
import { useBikes } from '../hooks/useBikes'
import { BikesTable } from '../components/BikesTable'
import { MarkOooDialog } from '../components/MarkOooDialog'
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

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-slate-900">Bike Inventory</h1>
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
    </div>
  )
}

