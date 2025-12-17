/**
 * OverviewStats - displays summary stat cards for bikes and rentals
 */

interface StatCardProps {
  label: string
  value: number | string
  variant: 'success' | 'info' | 'warning' | 'error'
  icon?: React.ReactNode
}

const variantStyles = {
  success: {
    bg: 'bg-emerald-50',
    text: 'text-emerald-700',
    border: 'border-emerald-200',
    accent: 'bg-emerald-500',
  },
  info: {
    bg: 'bg-sky-50',
    text: 'text-sky-700',
    border: 'border-sky-200',
    accent: 'bg-sky-500',
  },
  warning: {
    bg: 'bg-amber-50',
    text: 'text-amber-700',
    border: 'border-amber-200',
    accent: 'bg-amber-500',
  },
  error: {
    bg: 'bg-rose-50',
    text: 'text-rose-700',
    border: 'border-rose-200',
    accent: 'bg-rose-500',
  },
}

function StatCard({ label, value, variant, icon }: StatCardProps) {
  const styles = variantStyles[variant]

  return (
    <div
      className={`relative overflow-hidden rounded-xl border ${styles.border} ${styles.bg} p-5 transition-all hover:shadow-md`}
    >
      <div className={`absolute top-0 left-0 h-1 w-full ${styles.accent}`} />
      <div className="flex items-start justify-between">
        <div>
          <p className={`text-sm font-medium ${styles.text} opacity-80`}>{label}</p>
          <p className={`mt-2 text-3xl font-bold tracking-tight ${styles.text}`}>
            {value}
          </p>
        </div>
        {icon && (
          <div className={`${styles.text} opacity-60`}>{icon}</div>
        )}
      </div>
    </div>
  )
}

// Icons as simple SVG components
function BikeIcon() {
  return (
    <svg className="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <circle cx="5.5" cy="17.5" r="3.5" strokeWidth="1.5" />
      <circle cx="18.5" cy="17.5" r="3.5" strokeWidth="1.5" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M15 6h2l3 8M5.5 17.5l3-7h7l2.5 7M12 6v4" />
    </svg>
  )
}

function ClockIcon() {
  return (
    <svg className="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <circle cx="12" cy="12" r="9" strokeWidth="1.5" />
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 7v5l3 3" />
    </svg>
  )
}

function AlertIcon() {
  return (
    <svg className="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M12 9v2m0 4h.01M5.07 19h13.86c1.54 0 2.5-1.67 1.73-3L13.73 4c-.77-1.33-2.69-1.33-3.46 0L3.34 16c-.77 1.33.19 3 1.73 3z" />
    </svg>
  )
}

function WrenchIcon() {
  return (
    <svg className="h-8 w-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37 1 .608 2.296.07 2.572-1.065z" />
      <circle cx="12" cy="12" r="3" strokeWidth="1.5" />
    </svg>
  )
}

export interface OverviewStatsProps {
  bikesAvailable: number
  bikesRented: number
  bikesOoo: number
  rentalsActive: number
  rentalsOverdue: number
}

export function OverviewStats({
  bikesAvailable,
  bikesRented,
  bikesOoo,
  rentalsActive,
  rentalsOverdue,
}: OverviewStatsProps) {
  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
      <StatCard
        label="Available Bikes"
        value={bikesAvailable}
        variant="success"
        icon={<BikeIcon />}
      />
      <StatCard
        label="Rented Out"
        value={bikesRented}
        variant="info"
        icon={<BikeIcon />}
      />
      <StatCard
        label="Out of Order"
        value={bikesOoo}
        variant="warning"
        icon={<WrenchIcon />}
      />
      <StatCard
        label="Active Rentals"
        value={rentalsActive}
        variant="info"
        icon={<ClockIcon />}
      />
      <StatCard
        label="Overdue"
        value={rentalsOverdue}
        variant="error"
        icon={<AlertIcon />}
      />
    </div>
  )
}

