/**
 * SignaturePreview - Displays signature image or placeholder
 */

export interface SignaturePreviewProps {
  signatureUrl: string | null
}

export function SignaturePreview({ signatureUrl }: SignaturePreviewProps) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-6 py-4">
        <h2 className="text-lg font-semibold text-slate-900">Guest Signature</h2>
      </div>
      <div className="p-6">
        {signatureUrl ? (
          <div className="inline-block rounded-lg border border-slate-200 bg-slate-50 p-4">
            <img src={signatureUrl} alt="Guest signature" className="max-h-32 max-w-xs" />
          </div>
        ) : (
          <div className="flex items-center gap-2 text-slate-500">
            <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
            <span>Signature not available</span>
          </div>
        )}
      </div>
    </div>
  )
}

