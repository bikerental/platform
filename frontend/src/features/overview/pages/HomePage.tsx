/**
 * HomePage - Dashboard/Overview page
 * Displays bike/rental statistics and active rentals list
 */

import { Link } from 'react-router-dom'
import { useOverview } from '../hooks/useOverview'
import { OverviewStats } from '../components/OverviewStats'
import { ActiveRentalsList } from '../components/ActiveRentalsList'

export function HomePage() {
  const {
    data,
    filteredRentals,
    isLoading,
    error,
    searchQuery,
    setSearchQuery,
    refresh,
  } = useOverview()

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">Dashboard</h1>
          <p className="mt-1 text-sm text-slate-600">
            Overview of bike inventory and active rentals
          </p>
        </div>
        <Link
          to="/rentals/new"
          className="inline-flex items-center justify-center gap-2 rounded-lg bg-sky-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-sky-700 focus:outline-none focus:ring-2 focus:ring-sky-500 focus:ring-offset-2"
        >
          <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
          </svg>
          New Rental
        </Link>
      </div>

      {/* Error Message */}
      {error && (
        <div className="rounded-lg border border-rose-200 bg-rose-50 p-4">
          <div className="flex items-center gap-3">
            <svg className="h-5 w-5 text-rose-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <p className="text-sm font-medium text-rose-700">{error}</p>
            <button
              onClick={refresh}
              className="ml-auto text-sm font-medium text-rose-700 underline hover:no-underline"
            >
              Retry
            </button>
          </div>
        </div>
      )}

      {/* Stats Cards */}
      {isLoading && !data ? (
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="h-24 animate-pulse rounded-xl bg-slate-100" />
          ))}
        </div>
      ) : data ? (
        <OverviewStats
          bikesAvailable={data.bikesAvailable}
          bikesRented={data.bikesRented}
          bikesOoo={data.bikesOoo}
          rentalsActive={data.rentalsActive}
          rentalsOverdue={data.rentalsOverdue}
        />
      ) : null}

      {/* Active Rentals Section */}
      <div className="space-y-4">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <h2 className="text-lg font-semibold text-slate-900">Active Rentals</h2>
          
          {/* Search Input */}
          <div className="relative">
            <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
              <svg className="h-4 w-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by ID, room, or bed..."
              className="block w-full rounded-lg border border-slate-300 bg-white py-2 pl-10 pr-4 text-sm text-slate-900 placeholder-slate-400 focus:border-sky-500 focus:outline-none focus:ring-1 focus:ring-sky-500 sm:w-64"
            />
          </div>
        </div>

        <ActiveRentalsList
          rentals={filteredRentals}
          isLoading={isLoading && !data}
        />
      </div>
    </div>
  )
}

