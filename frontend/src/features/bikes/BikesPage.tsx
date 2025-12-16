import { useState, useEffect } from 'react'
import { bikeService, type Bike } from '../../services/bikeService'

export function BikesPage() {
  const [bikes, setBikes] = useState<Bike[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [statusFilter, setStatusFilter] = useState<'AVAILABLE' | 'RENTED' | 'OOO' | 'ALL'>('ALL')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedBike, setSelectedBike] = useState<Bike | null>(null)
  const [showOooDialog, setShowOooDialog] = useState(false)
  const [oooNote, setOooNote] = useState('')

  useEffect(() => {
    loadBikes()
  }, [statusFilter, searchQuery])

  const loadBikes = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const params: { status?: Bike['status']; q?: string } = {}
      if (statusFilter !== 'ALL') {
        params.status = statusFilter
      }
      if (searchQuery.trim()) {
        params.q = searchQuery.trim()
      }
      const data = await bikeService.listBikes(params)
      setBikes(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load bikes')
    } finally {
      setIsLoading(false)
    }
  }

  const handleMarkOoo = async (bike: Bike) => {
    if (!oooNote.trim()) {
      return
    }
    try {
      await bikeService.markOoo(bike.bikeId, oooNote.trim())
      setShowOooDialog(false)
      setOooNote('')
      setSelectedBike(null)
      await loadBikes()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to mark bike as OOO')
    }
  }

  const handleMarkAvailable = async (bike: Bike) => {
    try {
      await bikeService.markAvailable(bike.bikeId)
      await loadBikes()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to mark bike as available')
    }
  }

  const openOooDialog = (bike: Bike) => {
    setSelectedBike(bike)
    setOooNote('')
    setShowOooDialog(true)
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
              onChange={(e) => setStatusFilter(e.target.value as typeof statusFilter)}
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

      {/* Bike List */}
      {isLoading ? (
        <div className="text-center py-12">
          <p className="text-slate-600">Loading bikes...</p>
        </div>
      ) : bikes.length === 0 ? (
        <div className="text-center py-12 bg-white rounded-lg border border-slate-200">
          <p className="text-slate-600">No bikes found</p>
        </div>
      ) : (
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
                      <div className="text-sm font-medium text-slate-900">{bike.bikeNumber}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-slate-600">{bike.bikeType || '—'}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <StatusBadge status={bike.status} />
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
                            onClick={() => openOooDialog(bike)}
                            className="text-orange-600 hover:text-orange-900 px-3 py-1 rounded hover:bg-orange-50 transition-colors"
                          >
                            Mark OOO
                          </button>
                        )}
                        {bike.status === 'OOO' && (
                          <button
                            onClick={() => handleMarkAvailable(bike)}
                            className="text-green-600 hover:text-green-900 px-3 py-1 rounded hover:bg-green-50 transition-colors"
                          >
                            Mark Available
                          </button>
                        )}
                        {bike.status === 'RENTED' && (
                          <span className="text-slate-400 text-xs">No actions available</span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Mark OOO Dialog */}
      {showOooDialog && selectedBike && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full p-6">
            <h2 className="text-xl font-bold text-slate-900 mb-4">
              Mark Bike {selectedBike.bikeNumber} as Out of Order
            </h2>
            <div className="mb-4">
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Reason / Note <span className="text-red-500">*</span>
              </label>
              <textarea
                value={oooNote}
                onChange={(e) => setOooNote(e.target.value)}
                placeholder="Enter reason for marking bike as OOO..."
                rows={4}
                className="w-full px-4 py-2 rounded-lg border border-slate-300 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                autoFocus
              />
            </div>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => {
                  setShowOooDialog(false)
                  setOooNote('')
                  setSelectedBike(null)
                }}
                className="px-4 py-2 text-sm font-medium text-slate-700 bg-white border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => handleMarkOoo(selectedBike)}
                disabled={!oooNote.trim()}
                className="px-4 py-2 text-sm font-medium text-white bg-orange-600 rounded-lg hover:bg-orange-700 disabled:bg-orange-300 disabled:cursor-not-allowed transition-colors"
              >
                Mark OOO
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function StatusBadge({ status }: { status: Bike['status'] }) {
  const styles = {
    AVAILABLE: 'bg-green-100 text-green-800',
    RENTED: 'bg-blue-100 text-blue-800',
    OOO: 'bg-orange-100 text-orange-800',
  }

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${styles[status]}`}>
      {status}
    </span>
  )
}

