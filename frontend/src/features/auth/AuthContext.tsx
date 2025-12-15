import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import type { ReactNode } from 'react'
import { 
  login as authLogin, 
  logout as authLogout, 
  isAuthenticated as checkAuth,
  getHotelName,
} from '../../services/authService'
import type { LoginRequest } from '../../services/authService'

interface AuthState {
  isAuthenticated: boolean
  hotelName: string | null
  isLoading: boolean
}

interface AuthContextType extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    isAuthenticated: false,
    hotelName: null,
    isLoading: true,
  })

  // Initialize auth state from localStorage
  useEffect(() => {
    const authenticated = checkAuth()
    const hotelName = getHotelName()
    setState({
      isAuthenticated: authenticated,
      hotelName: authenticated ? hotelName : null,
      isLoading: false,
    })
  }, [])

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

  return (
    <AuthContext.Provider value={{ ...state, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
