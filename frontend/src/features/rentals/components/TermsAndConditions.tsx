/**
 * TermsAndConditions - T&C display with acceptance checkbox
 */

// Default T&C text (would come from settings API in production)
const DEFAULT_TNC_TEXT = `By signing below, I acknowledge that I have received the bicycle(s) listed above in good condition. I agree to return them by the specified due date and time. I accept responsibility for any damage to or loss of the bicycle(s) during the rental period. I understand that late returns may incur additional charges.

The rental fee is non-refundable. In case of damage or loss, I agree to pay the full replacement cost of the bicycle(s). I confirm that I am at least 18 years of age and have read and understood these terms and conditions.

I release the hotel and its staff from any liability for injuries sustained while using the bicycles. I agree to follow all traffic laws and safety guidelines while operating the bicycles.`

export interface TermsAndConditionsProps {
  accepted: boolean
  onAcceptChange: (accepted: boolean) => void
  error?: string
}

export function TermsAndConditions({ accepted, onAcceptChange, error }: TermsAndConditionsProps) {
  return (
    <section className="mb-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="mb-4 text-lg font-semibold text-slate-800">Terms & Conditions</h2>

      <div className="mb-4 h-48 overflow-y-auto rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm leading-relaxed text-slate-700">
        {DEFAULT_TNC_TEXT.split('\n\n').map((paragraph, i) => (
          <p key={i} className="mb-3 last:mb-0">
            {paragraph}
          </p>
        ))}
      </div>

      <label className="flex cursor-pointer items-start gap-3">
        <input
          type="checkbox"
          checked={accepted}
          onChange={e => onAcceptChange(e.target.checked)}
          className="mt-0.5 h-5 w-5 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
        />
        <span className="text-sm text-slate-700">
          I have read and agree to the Terms & Conditions
          <span className="text-red-500"> *</span>
        </span>
      </label>
      {error && <p className="mt-2 text-sm text-red-500">{error}</p>}
    </section>
  )
}

