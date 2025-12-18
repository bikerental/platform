/**
 * GuestRentalForm - Form section for guest rental details
 */

import type { AssignedBike } from '../types'

export interface GuestRentalFormProps {
  assignedBikes: AssignedBike[]
  returnDate: string
  returnTime: string
  roomNumber: string
  bedNumber: string
  errors: Record<string, string>
  onReturnDateChange: (value: string) => void
  onReturnTimeChange: (value: string) => void
  onRoomNumberChange: (value: string) => void
  onBedNumberChange: (value: string) => void
}

export function GuestRentalForm({
  assignedBikes,
  returnDate,
  returnTime,
  roomNumber,
  bedNumber,
  errors,
  onReturnDateChange,
  onReturnTimeChange,
  onRoomNumberChange,
  onBedNumberChange,
}: GuestRentalFormProps) {
  return (
    <>
      {/* Assigned Bikes Display */}
      <section className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-semibold text-slate-800">Bikes You're Renting</h2>
        <div className="flex flex-wrap gap-2">
          {assignedBikes.map(bike => (
            <div
              key={bike.bikeId}
              className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2"
            >
              <span className="text-2xl">🚲</span>
              <div>
                <span className="font-bold text-emerald-800">#{bike.bikeNumber}</span>
                {bike.bikeType && <span className="ml-2 text-sm text-emerald-600">{bike.bikeType}</span>}
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Form Fields */}
      <section className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-lg font-semibold text-slate-800">Rental Details</h2>

        <div className="space-y-5">
          {/* Return Date & Time */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-2 block text-sm font-semibold text-slate-700">
                Return Date <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                value={returnDate}
                onChange={e => onReturnDateChange(e.target.value)}
                min={new Date().toISOString().split('T')[0]}
                className={`w-full rounded-xl border px-4 py-3 text-lg focus:border-blue-500 focus:ring-2 focus:ring-blue-500 ${
                  errors.returnDate ? 'border-red-500' : 'border-slate-300'
                }`}
              />
              {errors.returnDate && <p className="mt-1 text-sm text-red-500">{errors.returnDate}</p>}
            </div>
            <div>
              <label className="mb-2 block text-sm font-semibold text-slate-700">
                Return Time <span className="text-red-500">*</span>
              </label>
              <input
                type="time"
                value={returnTime}
                onChange={e => onReturnTimeChange(e.target.value)}
                className={`w-full rounded-xl border px-4 py-3 text-lg focus:border-blue-500 focus:ring-2 focus:ring-blue-500 ${
                  errors.returnTime ? 'border-red-500' : 'border-slate-300'
                }`}
              />
              {errors.returnTime && <p className="mt-1 text-sm text-red-500">{errors.returnTime}</p>}
            </div>
          </div>

          {/* Room & Bed */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="mb-2 block text-sm font-semibold text-slate-700">
                Room Number <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={roomNumber}
                onChange={e => onRoomNumberChange(e.target.value)}
                placeholder="e.g. 204"
                className={`w-full rounded-xl border px-4 py-3 text-lg focus:border-blue-500 focus:ring-2 focus:ring-blue-500 ${
                  errors.roomNumber ? 'border-red-500' : 'border-slate-300'
                }`}
              />
              {errors.roomNumber && <p className="mt-1 text-sm text-red-500">{errors.roomNumber}</p>}
            </div>
            <div>
              <label className="mb-2 block text-sm font-semibold text-slate-700">
                Bed Number <span className="text-slate-400">(optional)</span>
              </label>
              <input
                type="text"
                value={bedNumber}
                onChange={e => onBedNumberChange(e.target.value)}
                placeholder="e.g. A"
                className="w-full rounded-xl border border-slate-300 px-4 py-3 text-lg focus:border-blue-500 focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
        </div>
      </section>
    </>
  )
}

