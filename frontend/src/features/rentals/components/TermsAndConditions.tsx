/**
 * TermsAndConditions - T&C display with acceptance checkbox
 */

/// Default T&C text (would come from settings API in production)
const DEFAULT_TNC_TEXT = `By signing below, I acknowledge that the bicycle is rented for use only by the designated customer and may not be used by any other person.

In case of loss, theft, or damage to the bicycle or its key, the hostel will charge the customer the full cost of the missing or damaged items. The replacement cost is 250 DKK for a lost key and 1,250 DKK for a lost bicycle.

The hostel is not liable for any claims related to bodily injury sustained by the customer or any third party, nor for loss or damage to personal belongings. It is the responsibility of the customer and their group to comply with all Danish traffic laws and regulations while using the bicycle.`;

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

