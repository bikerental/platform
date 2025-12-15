import { Outlet, Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../features/auth/AuthContext'

/**
 * Main application layout with header, navigation, and content area.
 * Used for all authenticated pages.
 */
export function AppLayout() {
  const { hotelName, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Header */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            {/* Logo / Title */}
            <Link to="/" className="flex items-center gap-3">
              <span className="text-xl font-bold text-slate-900">🚲 Bike Rental</span>
            </Link>

            {/* Hotel Name & Logout */}
            <div className="flex items-center gap-4">
              <span className="text-slate-600 font-medium">{hotelName}</span>
              <button
                onClick={handleLogout}
                className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Navigation */}
      <nav className="bg-white border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex gap-1 py-2">
            <NavLink to="/">Home</NavLink>
            <NavLink to="/bikes">Bikes</NavLink>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Outlet />
      </main>
    </div>
  )
}

function NavLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <Link
      to={to}
      className="px-4 py-2 text-sm font-medium text-slate-600 hover:text-slate-900 hover:bg-slate-100 rounded-lg transition-colors"
    >
      {children}
    </Link>
  )
}
