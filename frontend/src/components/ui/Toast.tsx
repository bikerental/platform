/**
 * Toast notification component for displaying temporary messages
 */

import { useEffect } from 'react'

export type ToastType = 'success' | 'error' | 'info'

export interface ToastProps {
  message: string
  type: ToastType
  onClose: () => void
  duration?: number
}

const typeStyles: Record<ToastType, string> = {
  success: 'bg-emerald-600 text-white',
  error: 'bg-rose-600 text-white',
  info: 'bg-sky-600 text-white',
}

export function Toast({ message, type, onClose, duration = 4000 }: ToastProps) {
  useEffect(() => {
    const timer = setTimeout(onClose, duration)
    return () => clearTimeout(timer)
  }, [onClose, duration])

  return (
    <div
      className={`fixed bottom-4 right-4 z-50 rounded-lg px-4 py-3 shadow-lg ${typeStyles[type]}`}
      role="alert"
    >
      {message}
    </div>
  )
}

