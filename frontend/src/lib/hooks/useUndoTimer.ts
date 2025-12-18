/**
 * Hook for managing undo operations with a countdown timer
 */

import { useState, useEffect, useCallback } from 'react'

export interface UndoState<T> {
  data: T
  expiresAt: number
}

export interface UseUndoTimerResult<T> {
  undoState: UndoState<T> | null
  timeLeft: number
  startUndo: (data: T, durationMs?: number) => void
  clearUndo: () => void
  isActive: boolean
}

const DEFAULT_UNDO_DURATION_MS = 30000 // 30 seconds

export function useUndoTimer<T>(): UseUndoTimerResult<T> {
  const [undoState, setUndoState] = useState<UndoState<T> | null>(null)
  const [timeLeft, setTimeLeft] = useState(0)

  // Timer effect
  useEffect(() => {
    if (!undoState) {
      setTimeLeft(0)
      return
    }

    const interval = setInterval(() => {
      const remaining = Math.max(0, undoState.expiresAt - Date.now())
      setTimeLeft(remaining)

      if (remaining <= 0) {
        setUndoState(null)
      }
    }, 100)

    return () => clearInterval(interval)
  }, [undoState])

  const startUndo = useCallback((data: T, durationMs = DEFAULT_UNDO_DURATION_MS) => {
    setUndoState({
      data,
      expiresAt: Date.now() + durationMs,
    })
  }, [])

  const clearUndo = useCallback(() => {
    setUndoState(null)
  }, [])

  return {
    undoState,
    timeLeft,
    startUndo,
    clearUndo,
    isActive: undoState !== null && timeLeft > 0,
  }
}

