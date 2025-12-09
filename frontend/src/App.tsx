import { Routes, Route } from 'react-router-dom'

// Placeholder pages - will be implemented in later phases
function HomePage() {
  return (
    <div className="min-h-screen bg-slate-50 p-8">
      <h1 className="text-3xl font-bold text-slate-900">Bike Rental System</h1>
      <p className="mt-2 text-slate-600">Welcome to the home page. Routes configured.</p>
    </div>
  )
}

function LoginPage() {
  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center">
      <div className="bg-white p-8 rounded-lg shadow-md">
        <h1 className="text-2xl font-bold text-slate-900">Login</h1>
        <p className="mt-2 text-slate-600">Login page placeholder.</p>
      </div>
    </div>
  )
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<HomePage />} />
    </Routes>
  )
}

export default App
