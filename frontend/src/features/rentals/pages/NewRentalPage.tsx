/**
 * NewRentalPage - Staff mode for creating new rentals
 * Manages bike assignment locally before handing device to guest.
 */

import { useNavigate } from 'react-router-dom'
import { useNewRental } from '../hooks/useNewRental'
import { BikeInput } from '../components/BikeInput'
import { AssignedBikesList } from '../components/AssignedBikesList'

export function NewRentalPage() {
  const navigate = useNavigate()
  const {
    assignedBikes,
    error,
    isLoading,
    addBike,
    removeBike,
    clearAll,
    clearError,
  } = useNewRental()

  const handleContinue = () => {
    // Navigate to guest mode with bike data
    // Using state to pass assigned bikes to guest page
    navigate('/rentals/new/guest', {
      state: { assignedBikes },
    })
  }

  const handleCancel = () => {
    navigate('/')
  }

  return (
    <div className="max-w-2xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-slate-900 mb-2">New Rental</h1>
        <p className="text-slate-600">
          Add bikes to this rental, then hand the device to the guest to complete.
        </p>
      </div>

      {/* Main Content Card */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        {/* Add Bike Section */}
        <div className="p-6 border-b border-slate-100 bg-gradient-to-br from-slate-50 to-white">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">
            Add Bikes
          </h2>
          <BikeInput
            onAdd={addBike}
            error={error}
            isLoading={isLoading}
            onClearError={clearError}
          />
        </div>

        {/* Assigned Bikes Section */}
        <div className="p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-slate-800">
              Assigned Bikes
              {assignedBikes.length > 0 && (
                <span className="ml-2 inline-flex items-center justify-center px-2.5 py-0.5 rounded-full text-sm font-medium bg-emerald-100 text-emerald-800">
                  {assignedBikes.length}
                </span>
              )}
            </h2>
            {assignedBikes.length > 0 && (
              <button
                type="button"
                onClick={clearAll}
                className="text-sm text-slate-500 hover:text-red-600 transition-colors"
              >
                Clear all
              </button>
            )}
          </div>

          <AssignedBikesList
            bikes={assignedBikes}
            onRemove={removeBike}
            disabled={isLoading}
          />
        </div>
      </div>

      {/* Action Buttons */}
      <div className="mt-8 flex items-center justify-between">
        <button
          type="button"
          onClick={handleCancel}
          className="px-6 py-3 text-slate-600 font-medium hover:text-slate-900 hover:bg-slate-100 rounded-xl transition-colors"
        >
          Cancel
        </button>

        <button
          type="button"
          onClick={handleContinue}
          disabled={assignedBikes.length === 0}
          className="px-8 py-3 bg-emerald-600 text-white font-semibold rounded-xl hover:bg-emerald-700 focus:ring-2 focus:ring-emerald-500 focus:ring-offset-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-emerald-600 flex items-center gap-2"
        >
          <span>Continue & Hand to Guest</span>
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M13 7l5 5m0 0l-5 5m5-5H6"
            />
          </svg>
        </button>
      </div>

      {/* Help Text */}
      {assignedBikes.length > 0 && (
        <div className="mt-6 p-4 bg-blue-50 border border-blue-100 rounded-xl">
          <div className="flex gap-3">
            <div className="flex-shrink-0 text-blue-500">
              <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path
                  fillRule="evenodd"
                  d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                  clipRule="evenodd"
                />
              </svg>
            </div>
            <div className="text-sm text-blue-800">
              <p className="font-medium">Ready to continue?</p>
              <p className="mt-1 text-blue-600">
                Click "Continue & Hand to Guest" to open the guest form where the guest
                will enter their details and sign the rental agreement.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

