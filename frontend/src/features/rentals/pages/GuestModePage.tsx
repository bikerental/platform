/**
 * GuestModePage - Guest mode for completing rental
 * Displays assigned bikes, collects guest details, T&C acceptance, and signature.
 */

import { useState, useRef, useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import SignatureCanvas from 'react-signature-canvas'
import { SignaturePad } from '../components/SignaturePad'
import { GuestRentalForm } from '../components/GuestRentalForm'
import { TermsAndConditions } from '../components/TermsAndConditions'
import { RentalSuccessScreen } from '../components/RentalSuccessScreen'
import { RentalErrorScreen } from '../components/RentalErrorScreen'
import { createRentalWithDetails, type UnavailableBike } from '../api/rentalApi'
import type { AssignedBike, Rental } from '../types'

const TNC_VERSION = '1.0'

type PageState = 'form' | 'success' | 'error'

export function GuestModePage() {
  const location = useLocation()
  const navigate = useNavigate()
  const signatureRef = useRef<SignatureCanvas>(null)

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
    setReturnTime(`${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}`)
  }, [])

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {}

    if (!returnDate) newErrors.returnDate = 'Return date is required'
    if (!returnTime) newErrors.returnTime = 'Return time is required'

    if (returnDate && returnTime) {
      const returnDateTime = new Date(`${returnDate}T${returnTime}`)
      if (returnDateTime <= new Date()) {
        newErrors.returnDate = 'Return date/time must be in the future'
      }
    }

    if (!roomNumber.trim()) newErrors.roomNumber = 'Room number is required'
    if (!hasSignature) newErrors.signature = 'Signature is required'
    if (!tncAccepted) newErrors.tnc = 'You must accept the terms and conditions'

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleSubmit = async () => {
    if (!validateForm()) return

    setIsSubmitting(true)
    setErrors({})

    try {
      const signatureBase64 = signatureRef.current?.toDataURL('image/png') || ''
      const returnDateTime = new Date(`${returnDate}T${returnTime}`).toISOString()

      const result = await createRentalWithDetails({
        bikeNumbers: assignedBikes.map(b => b.bikeNumber),
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
      setErrors({ submit: error instanceof Error ? error.message : 'Failed to create rental' })
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleDone = () => navigate('/')
  const handleRetry = () => navigate('/rentals/new')
  const handleBackToStaff = () => navigate('/rentals/new', { state: { assignedBikes } })

  // Success screen
  if (pageState === 'success' && createdRental) {
    return <RentalSuccessScreen rental={createdRental} onDone={handleDone} />
  }

  // Error screen
  if (pageState === 'error' && unavailableBikes.length > 0) {
    return <RentalErrorScreen unavailableBikes={unavailableBikes} onRetry={handleRetry} />
  }

  // Main form view
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50">
      {/* Header */}
      <header className="sticky top-0 z-10 border-b border-slate-200 bg-white/80 backdrop-blur-sm">
        <div className="mx-auto flex max-w-2xl items-center justify-between px-4 py-4">
          <h1 className="text-xl font-bold text-slate-900">🚲 Complete Your Rental</h1>
          <button onClick={handleBackToStaff} className="text-sm text-slate-500 hover:text-slate-700">
            ← Back to Staff
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-2xl px-4 py-8">
        <GuestRentalForm
          assignedBikes={assignedBikes}
          returnDate={returnDate}
          returnTime={returnTime}
          roomNumber={roomNumber}
          bedNumber={bedNumber}
          errors={errors}
          onReturnDateChange={setReturnDate}
          onReturnTimeChange={setReturnTime}
          onRoomNumberChange={setRoomNumber}
          onBedNumberChange={setBedNumber}
        />

        <TermsAndConditions accepted={tncAccepted} onAcceptChange={setTncAccepted} error={errors.tnc} />

        {/* Signature */}
        <section className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <SignaturePad signatureRef={signatureRef} onSignatureChange={setHasSignature} />
          {errors.signature && <p className="mt-2 text-sm text-red-500">{errors.signature}</p>}
        </section>

        {/* Submit Error */}
        {errors.submit && (
          <div className="mb-6 rounded-xl border border-red-200 bg-red-50 p-4">
            <p className="text-red-700">{errors.submit}</p>
          </div>
        )}

        {/* Submit Button */}
        <button
          onClick={handleSubmit}
          disabled={isSubmitting}
          className="w-full rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 py-4 text-lg font-bold text-white shadow-lg shadow-blue-500/25 transition-all hover:from-blue-700 hover:to-indigo-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {isSubmitting ? (
            <span className="flex items-center justify-center gap-2">
              <svg className="h-5 w-5 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
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

        <p className="mt-6 text-center text-sm text-slate-500">
          By confirming, you agree to the rental terms above.
        </p>
      </main>
    </div>
  )
}
