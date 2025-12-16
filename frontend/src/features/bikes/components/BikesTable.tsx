/**
 * BikesTable component - displays bike inventory in a table format
 */

import { Badge } from '@/components/ui/Badge'
import type { Bike, BikeStatus } from '../types'

export interface BikesTableProps {
  bikes: Bike[]
  isLoading: boolean
  onMarkOoo: (bike: Bike) => void
  onMarkAvailable: (bike: Bike) => void
}

const statusVariantMap: Record<BikeStatus, 'success' | 'info' | 'warning'> = {
  AVAILABLE: 'success',
  RENTED: 'info',
  OOO: 'warning',
}

export function BikesTable({
  bikes,
  isLoading,
  onMarkOoo,
  onMarkAvailable,
}: BikesTableProps) {
  if (isLoading) {
    return (
      <div className="text-center py-12">
        <p className="text-slate-600">Loading bikes...</p>
      </div>
    )
  }

  if (bikes.length === 0) {
    return (
      <div className="text-center py-12 bg-white rounded-lg border border-slate-200">
        <p className="text-slate-600">No bikes found</p>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg border border-slate-200 overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-slate-50 border-b border-slate-200">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                Bike Number
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                Type
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                OOO Note
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-slate-500 uppercase tracking-wider">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-slate-200">
            {bikes.map((bike) => (
              <tr key={bike.bikeId} className="hover:bg-slate-50">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm font-medium text-slate-900">
                    {bike.bikeNumber}
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="text-sm text-slate-600">
                    {bike.bikeType || '—'}
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <Badge variant={statusVariantMap[bike.status]}>
                    {bike.status}
                  </Badge>
                </td>
                <td className="px-6 py-4">
                  <div className="text-sm text-slate-600">
                    {bike.oooNote || '—'}
                    {bike.oooSince && (
                      <div className="text-xs text-slate-400 mt-1">
                        Since {new Date(bike.oooSince).toLocaleDateString()}
                      </div>
                    )}
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <div className="flex gap-2">
                    {bike.status === 'AVAILABLE' && (
                      <button
                        onClick={() => onMarkOoo(bike)}
                        className="text-orange-600 hover:text-orange-900 px-3 py-1 rounded hover:bg-orange-50 transition-colors"
                      >
                        Mark OOO
                      </button>
                    )}
                    {bike.status === 'OOO' && (
                      <button
                        onClick={() => onMarkAvailable(bike)}
                        className="text-green-600 hover:text-green-900 px-3 py-1 rounded hover:bg-green-50 transition-colors"
                      >
                        Mark Available
                      </button>
                    )}
                    {bike.status === 'RENTED' && (
                      <span className="text-slate-400 text-xs">
                        No actions available
                      </span>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

