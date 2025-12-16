import { Routes, Route } from 'react-router-dom'
import { AuthProvider, ProtectedRoute, LoginPage } from './features/auth'
import { BikesPage } from './features/bikes'
import { AppLayout } from './components/layout/AppLayout'

// Placeholder pages - will be implemented in later phases
function HomePage() {
  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900 mb-4">Dashboard</h1>
      <p className="text-slate-600">Welcome to the bike rental system. Home overview coming soon.</p>
      
      {/* Placeholder counts */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-8">
        <StatCard label="Available Bikes" value="--" color="green" />
        <StatCard label="Rented Bikes" value="--" color="blue" />
        <StatCard label="Active Rentals" value="--" color="blue" />
        <StatCard label="Overdue" value="--" color="red" />
      </div>
    </div>
  )
}

function StatCard({ label, value, color }: { label: string; value: string; color: 'green' | 'blue' | 'red' }) {
  const colorClasses = {
    green: 'bg-green-50 text-green-700 border-green-200',
    blue: 'bg-blue-50 text-blue-700 border-blue-200',
    red: 'bg-red-50 text-red-700 border-red-200',
  }
  
  return (
    <div className={`p-4 rounded-lg border ${colorClasses[color]}`}>
      <div className="text-3xl font-bold">{value}</div>
      <div className="text-sm mt-1 opacity-80">{label}</div>
    </div>
  )
}

function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public route */}
        <Route path="/login" element={<LoginPage />} />
        
        {/* Protected routes with layout */}
        <Route element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }>
          <Route path="/" element={<HomePage />} />
          <Route path="/bikes" element={<BikesPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}

export default App
