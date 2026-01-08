import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import type { ReactNode } from 'react'
import { 
  login as authLogin, 
  logout as authLogout, 
  isAuthenticated as checkAuth,
  getHotelName,
} from './api/authApi'
import type { LoginRequest, AuthState, AuthContextType } from './types'

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  // Initialize state directly from localStorage (synchronous, avoids useEffect)
  const [state, setState] = useState<AuthState>(() => {
    const authenticated = checkAuth()
    const hotelName = getHotelName()
    return {
      isAuthenticated: authenticated,
      hotelName: authenticated ? hotelName : null,
      isLoading: false,
    }
  })

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await authLogin(credentials)
    setState({
      isAuthenticated: true,
      hotelName: response.hotelName,
      isLoading: false,
    })
  }, [])

  const logout = useCallback(() => {
    authLogout()
    setState({
      isAuthenticated: false,
      hotelName: null,
      isLoading: false,
    })
  }, [])

  // Proactively check token validity and logout when expired
  useEffect(() => {
    if (!state.isAuthenticated) return

    const checkTokenValidity = () => {
      if (!checkAuth()) {
        logout()
      }
    }

    const interval = setInterval(checkTokenValidity, 10000)
    return () => clearInterval(interval)
  }, [state.isAuthenticated, logout])

  return (
    <AuthContext.Provider value={{ ...state, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextType {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
