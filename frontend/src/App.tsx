import { Routes, Route } from 'react-router-dom'
import { AuthProvider, ProtectedRoute, LoginPage } from './features/auth'
import { BikesPage } from './features/bikes'
import { NewRentalPage, GuestModePage, RentalDetailPage } from './features/rentals'
import { HomePage } from './features/overview'
import { AppLayout } from './components/layout/AppLayout'

function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public route */}
        <Route path="/login" element={<LoginPage />} />
        
        {/* Guest mode - protected but without staff navigation */}
        <Route
          path="/rentals/new/guest"
          element={
            <ProtectedRoute>
              <GuestModePage />
            </ProtectedRoute>
          }
        />
        
        {/* Protected routes with layout */}
        <Route element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }>
          <Route path="/" element={<HomePage />} />
          <Route path="/bikes" element={<BikesPage />} />
          <Route path="/rentals/new" element={<NewRentalPage />} />
          <Route path="/rentals/:id" element={<RentalDetailPage />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}

export default App
