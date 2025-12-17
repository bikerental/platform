/**
 * GuestModePage - Guest mode for completing rental
 * Displays assigned bikes, collects guest details, T&C acceptance, and signature.
 * No staff navigation visible in this mode.
 */

import { useState, useRef, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import SignatureCanvas from 'react-signature-canvas'
import { SignaturePad } from '../components/SignaturePad'
import { createRentalWithDetails, type UnavailableBike } from '../api/rentalApi'
import type { AssignedBike, Rental } from '../types'

// Default T&C text (would come from settings API in production)
const DEFAULT_TNC_TEXT = `By signing below, I acknowledge that I have received the bicycle(s) listed above in good condition. I agree to return them by the specified due date and time. I accept responsibility for any damage to or loss of the bicycle(s) during the rental period. I understand that late returns may incur additional charges.

The rental fee is non-refundable. In case of damage or loss, I agree to pay the full replacement cost of the bicycle(s). I confirm that I am at least 18 years of age and have read and understood these terms and conditions.

I release the hotel and its staff from any liability for injuries sustained while using the bicycles. I agree to follow all traffic laws and safety guidelines while operating the bicycles.`

const TNC_VERSION = '1.0'

type PageState = 'form' | 'success' | 'error'

export function GuestModePage() {
  const location = useLocation()
  const navigate = useNavigate()
  const signatureRef = useRef<SignatureCanvas>(null)

  // Get assigned bikes from navigation state
  const assignedBikes: AssignedBike[] = location.state?.assignedBikes || []

  // Form state
  const [returnDate, setReturnDate] = useState('')
  const [returnTime, setReturnTime] = useState('')
  const [roomNumber, setRoomNumber] = useState('')
  const [bedNumber, setBedNumber] = useState('')
  const [hasSignature, setHasSignature] = useState(false)
  const [tncAccepted, setTncAccepted] = useState(false)

  // Page state
  const [pageState, setPageState] = useState<PageState>('form')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [createdRental, setCreatedRental] = useState<Rental | null>(null)
  const [unavailableBikes, setUnavailableBikes] = useState<UnavailableBike[]>([])

  // Validation
  const [errors, setErrors] = useState<Record<string, string>>({})

  // Redirect if no bikes assigned
  useEffect(() => {
    if (assignedBikes.length === 0) {
      navigate('/rentals/new', { replace: true })
    }
  }, [assignedBikes.length, navigate])

  // Set default return date/time (tomorrow, same time)
  useEffect(() => {
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    setReturnDate(tomorrow.toISOString().split('T')[0])
    
    const now = new Date()
    const hours = now.getHours().toString().padStart(2, '0')
    const minutes = now.getMinutes().toString().padStart(2, '0')
    setReturnTime(`${hours}:${minutes}`)
  }, [])

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!returnDate) {
      newErrors.returnDate = 'Return date is required'
    }

    if (!returnTime) {
      newErrors.returnTime = 'Return time is required'
    }

    // Check if return date/time is in the future
    if (returnDate && returnTime) {
      const returnDateTime = new Date(`${returnDate}T${returnTime}`)
      if (returnDateTime <= new Date()) {
        newErrors.returnDate = 'Return date/time must be in the future'
      }
    }

    if (!roomNumber.trim()) {
      newErrors.roomNumber = 'Room number is required'
    }

    if (!hasSignature) {
      newErrors.signature = 'Signature is required'
    }

    if (!tncAccepted) {
      newErrors.tnc = 'You must accept the terms and conditions'
    }

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async () => {
    if (!validateForm()) return

    setIsSubmitting(true)
    setErrors({})

    try {
      // Get signature as base64 PNG
      const signatureBase64 = signatureRef.current?.toDataURL('image/png') || ''

      // Build return datetime
      const returnDateTime = new Date(`${returnDate}T${returnTime}`).toISOString()

      const result = await createRentalWithDetails({
        bikeNumbers: assignedBikes.map((b) => b.bikeNumber),
        roomNumber: roomNumber.trim(),
        bedNumber: bedNumber.trim() || undefined,
        returnDateTime,
        tncVersion: TNC_VERSION,
        signatureBase64Png: signatureBase64,
      })

      if (result.success) {
        setCreatedRental(result.rental)
        setPageState('success')
      } else {
        setUnavailableBikes(result.unavailableBikes)
        setPageState('error')
      }
    } catch (error) {
      setErrors({
        submit: error instanceof Error ? error.message : 'Failed to create rental',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleBackToStaff = () => {
    navigate('/rentals/new', { state: { assignedBikes } })
  }

  const handleDone = () => {
    if (createdRental) {
      // Navigate to rental detail page when implemented, for now go home
      navigate('/')
    } else {
      navigate('/')
    }
  }

  const handleRetryWithNewBikes = () => {
    navigate('/rentals/new')
  }

  // Render success screen
  if (pageState === 'success' && createdRental) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-emerald-50 to-teal-50 flex items-center justify-center p-4">
        <div className="max-w-lg w-full bg-white rounded-3xl shadow-xl p-8 text-center">
          {/* Success Icon */}
          <div className="w-20 h-20 bg-emerald-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg
              className="w-10 h-10 text-emerald-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>

          <h1 className="text-3xl font-bold text-slate-900 mb-2">Rental Created!</h1>
          <p className="text-slate-600 mb-8">
            Your rental has been successfully recorded. Please return the bikes by the due date.
          </p>

          {/* Rental Summary */}
          <div className="bg-slate-50 rounded-2xl p-6 mb-8 text-left">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-slate-500">Rental ID</span>
                <p className="font-semibold text-slate-900">#{createdRental.rentalId}</p>
              </div>
              <div>
                <span className="text-slate-500">Room</span>
                <p className="font-semibold text-slate-900">
                  {createdRental.roomNumber}
                  {createdRental.bedNumber && ` / Bed ${createdRental.bedNumber}`}
                </p>
              </div>
              <div>
                <span className="text-slate-500">Due By</span>
                <p className="font-semibold text-slate-900">
                  {new Date(createdRental.dueAt).toLocaleString()}
                </p>
              </div>
              <div>
                <span className="text-slate-500">Bikes</span>
                <p className="font-semibold text-slate-900">
                  {createdRental.items.length} bike(s)
                </p>
              </div>
            </div>

            {/* Bike List */}
            <div className="mt-4 pt-4 border-t border-slate-200">
              <span className="text-slate-500 text-sm">Bikes Rented:</span>
              <div className="flex flex-wrap gap-2 mt-2">
                {createdRental.items.map((item) => (
                  <span
                    key={item.rentalItemId}
                    className="px-3 py-1 bg-emerald-100 text-emerald-800 rounded-full text-sm font-medium"
                  >
                    #{item.bikeNumber}
                  </span>
                ))}
              </div>
            </div>
          </div>

          <button
            onClick={handleDone}
            className="w-full py-4 bg-emerald-600 text-white font-semibold rounded-xl hover:bg-emerald-700 transition-colors text-lg"
          >
            Done
          </button>
        </div>
      </div>
    )
  }

  // Render error screen (bikes unavailable)
  if (pageState === 'error' && unavailableBikes.length > 0) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-red-50 to-orange-50 flex items-center justify-center p-4">
        <div className="max-w-lg w-full bg-white rounded-3xl shadow-xl p-8 text-center">
          {/* Error Icon */}
          <div className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-6">
            <svg
              className="w-10 h-10 text-red-600"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
          </div>

          <h1 className="text-2xl font-bold text-slate-900 mb-2">Some Bikes Unavailable</h1>
          <p className="text-slate-600 mb-6">
            One or more bikes became unavailable. Please return to staff mode to reassign bikes.
          </p>

          {/* Unavailable Bikes List */}
          <div className="bg-red-50 rounded-2xl p-4 mb-8 text-left">
            <h3 className="font-semibold text-red-800 mb-3">Unavailable Bikes:</h3>
            <ul className="space-y-2">
              {unavailableBikes.map((bike) => (
                <li key={bike.bikeNumber} className="flex items-center gap-2 text-sm">
                  <span className="font-medium text-red-900">#{bike.bikeNumber}</span>
                  <span className="text-red-600">
                    ({bike.reason === 'ALREADY_RENTED'
                      ? 'Already rented'
                      : bike.reason === 'OUT_OF_ORDER'
                      ? 'Out of order'
                      : 'Not found'})
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <button
            onClick={handleRetryWithNewBikes}
            className="w-full py-4 bg-slate-800 text-white font-semibold rounded-xl hover:bg-slate-900 transition-colors"
          >
            Return to Staff Mode
          </button>
        </div>
      </div>
    )
  }

  // Main form view
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50">
      {/* Minimal header for guest mode */}
      <header className="bg-white/80 backdrop-blur-sm border-b border-slate-200 sticky top-0 z-10">
        <div className="max-w-2xl mx-auto px-4 py-4 flex items-center justify-between">
          <h1 className="text-xl font-bold text-slate-900">🚲 Complete Your Rental</h1>
          <button
            onClick={handleBackToStaff}
            className="text-sm text-slate-500 hover:text-slate-700"
          >
            ← Back to Staff
          </button>
        </div>
      </header>

      <main className="max-w-2xl mx-auto px-4 py-8">
        {/* Assigned Bikes Display */}
        <section className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Bikes You're Renting</h2>
          <div className="flex flex-wrap gap-2">
            {assignedBikes.map((bike) => (
              <div
                key={bike.bikeId}
                className="flex items-center gap-2 px-4 py-2 bg-emerald-50 border border-emerald-200 rounded-xl"
              >
                <span className="text-2xl">🚲</span>
                <div>
                  <span className="font-bold text-emerald-800">#{bike.bikeNumber}</span>
                  {bike.bikeType && (
                    <span className="text-emerald-600 text-sm ml-2">{bike.bikeType}</span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Form Fields */}
        <section className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Rental Details</h2>

          <div className="space-y-5">
            {/* Return Date & Time */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  Return Date <span className="text-red-500">*</span>
                </label>
                <input
                  type="date"
                  value={returnDate}
                  onChange={(e) => setReturnDate(e.target.value)}
                  min={new Date().toISOString().split('T')[0]}
                  className={`w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-lg ${
                    errors.returnDate ? 'border-red-500' : 'border-slate-300'
                  }`}
                />
                {errors.returnDate && (
                  <p className="text-red-500 text-sm mt-1">{errors.returnDate}</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  Return Time <span className="text-red-500">*</span>
                </label>
                <input
                  type="time"
                  value={returnTime}
                  onChange={(e) => setReturnTime(e.target.value)}
                  className={`w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-lg ${
                    errors.returnTime ? 'border-red-500' : 'border-slate-300'
                  }`}
                />
                {errors.returnTime && (
                  <p className="text-red-500 text-sm mt-1">{errors.returnTime}</p>
                )}
              </div>
            </div>

            {/* Room & Bed */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  Room Number <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={roomNumber}
                  onChange={(e) => setRoomNumber(e.target.value)}
                  placeholder="e.g. 204"
                  className={`w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-lg ${
                    errors.roomNumber ? 'border-red-500' : 'border-slate-300'
                  }`}
                />
                {errors.roomNumber && (
                  <p className="text-red-500 text-sm mt-1">{errors.roomNumber}</p>
                )}
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-2">
                  Bed Number <span className="text-slate-400">(optional)</span>
                </label>
                <input
                  type="text"
                  value={bedNumber}
                  onChange={(e) => setBedNumber(e.target.value)}
                  placeholder="e.g. A"
                  className="w-full px-4 py-3 border border-slate-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-lg"
                />
              </div>
            </div>
          </div>
        </section>

        {/* Terms & Conditions */}
        <section className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Terms & Conditions</h2>
          
          <div className="h-48 overflow-y-auto bg-slate-50 rounded-xl p-4 border border-slate-200 text-sm text-slate-700 leading-relaxed mb-4">
            {DEFAULT_TNC_TEXT.split('\n\n').map((paragraph, i) => (
              <p key={i} className="mb-3 last:mb-0">
                {paragraph}
              </p>
            ))}
          </div>

          <label className="flex items-start gap-3 cursor-pointer">
            <input
              type="checkbox"
              checked={tncAccepted}
              onChange={(e) => setTncAccepted(e.target.checked)}
              className="w-5 h-5 mt-0.5 text-blue-600 border-slate-300 rounded focus:ring-blue-500"
            />
            <span className="text-sm text-slate-700">
              I have read and agree to the Terms & Conditions
              <span className="text-red-500"> *</span>
            </span>
          </label>
          {errors.tnc && <p className="text-red-500 text-sm mt-2">{errors.tnc}</p>}
        </section>

        {/* Signature */}
        <section className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6 mb-6">
          <SignaturePad signatureRef={signatureRef} onSignatureChange={setHasSignature} />
          {errors.signature && <p className="text-red-500 text-sm mt-2">{errors.signature}</p>}
        </section>

        {/* Submit Error */}
        {errors.submit && (
          <div className="bg-red-50 border border-red-200 rounded-xl p-4 mb-6">
            <p className="text-red-700">{errors.submit}</p>
          </div>
        )}

        {/* Submit Button */}
        <button
          onClick={handleSubmit}
          disabled={isSubmitting}
          className="w-full py-4 bg-gradient-to-r from-blue-600 to-indigo-600 text-white font-bold rounded-xl hover:from-blue-700 hover:to-indigo-700 transition-all disabled:opacity-50 disabled:cursor-not-allowed text-lg shadow-lg shadow-blue-500/25"
        >
          {isSubmitting ? (
            <span className="flex items-center justify-center gap-2">
              <svg className="animate-spin w-5 h-5" fill="none" viewBox="0 0 24 24">
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
              Creating Rental...
            </span>
          ) : (
            'Confirm Rental'
          )}
        </button>

        {/* Footer Note */}
        <p className="text-center text-slate-500 text-sm mt-6">
          By confirming, you agree to the rental terms above.
        </p>
      </main>
    </div>
  )
}

