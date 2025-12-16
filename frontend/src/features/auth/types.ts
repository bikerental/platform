/**
 * Auth feature type definitions
 */

export interface LoginRequest {
  hotelCode: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  hotelName: string
}

export interface AuthState {
  isAuthenticated: boolean
  hotelName: string | null
  isLoading: boolean
}

export interface AuthContextType extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>
  logout: () => void
}

