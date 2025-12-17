/**
 * SignaturePad component for capturing guest signatures
 */

import { useRef, useEffect } from 'react'
import SignatureCanvas from 'react-signature-canvas'

interface SignaturePadProps {
  onSignatureChange: (hasSignature: boolean) => void
  signatureRef: React.RefObject<SignatureCanvas | null>
}

export function SignaturePad({ onSignatureChange, signatureRef }: SignaturePadProps) {
  const containerRef = useRef<HTMLDivElement>(null)

  // Resize canvas to fill container
  useEffect(() => {
    const resizeCanvas = () => {
      if (containerRef.current && signatureRef.current) {
        const canvas = signatureRef.current.getCanvas()
        const container = containerRef.current
        
        // Get the current signature data before resizing
        const wasEmpty = signatureRef.current.isEmpty()
        
        // Set canvas size to match container
        canvas.width = container.clientWidth
        canvas.height = container.clientHeight
        
        // Clear after resize (canvas data is lost)
        if (!wasEmpty) {
          onSignatureChange(false)
        }
      }
    }

    resizeCanvas()
    window.addEventListener('resize', resizeCanvas)
    return () => window.removeEventListener('resize', resizeCanvas)
  }, [signatureRef, onSignatureChange])

  const handleEnd = () => {
    if (signatureRef.current) {
      onSignatureChange(!signatureRef.current.isEmpty())
    }
  }

  const handleClear = () => {
    if (signatureRef.current) {
      signatureRef.current.clear()
      onSignatureChange(false)
    }
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <label className="block text-sm font-semibold text-slate-700">
          Signature <span className="text-red-500">*</span>
        </label>
        <button
          type="button"
          onClick={handleClear}
          className="text-sm text-slate-500 hover:text-red-600 transition-colors flex items-center gap-1"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
            />
          </svg>
          Clear
        </button>
      </div>
      
      <div
        ref={containerRef}
        className="border-2 border-dashed border-slate-300 rounded-xl bg-white h-48 relative overflow-hidden"
      >
        <SignatureCanvas
          ref={signatureRef}
          penColor="black"
          canvasProps={{
            className: 'absolute inset-0 w-full h-full touch-none',
          }}
          onEnd={handleEnd}
        />
        
        {/* Placeholder text */}
        <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
          <span className="text-slate-400 text-sm">Sign here</span>
        </div>
      </div>
      
      <p className="text-xs text-slate-500">
        Use your finger or stylus to sign above
      </p>
    </div>
  )
}

