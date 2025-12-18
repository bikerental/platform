/**
 * Hook for managing toast notifications
 */

import { useState, useCallback } from 'react'
import type { ToastType } from '@/components/ui/Toast'

export interface ToastState {
  message: string
  type: ToastType
}

export interface UseToastResult {
  toast: ToastState | null
  showToast: (message: string, type: ToastType) => void
  hideToast: () => void
}

export function useToast(): UseToastResult {
  const [toast, setToast] = useState<ToastState | null>(null)

  const showToast = useCallback((message: string, type: ToastType) => {
    setToast({ message, type })
  }, [])

  const hideToast = useCallback(() => {
    setToast(null)
  }, [])

  return { toast, showToast, hideToast }
}

