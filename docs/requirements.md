# Requirements – Bike Rental System (v1.0)

## Scope
- Staff-only iPad/web app for managing bike rentals at a hotel/hostel.
- Covers login, home overview, starting rentals (single iPad), viewing rentals, partial/full returns, marking bikes lost, overdue handling, forgotten contracts.

## Architecture & Deployment
- Monorepo; backend Spring Boot (Java 21, Spring Web/Security/JPA, JWT, Maven), frontend React SPA (Tailwind, React Router), MySQL DB.
- Backend/Frontend build independently; frontend calls backend over HTTP via `API_BASE_URL`; JWT supplies hotel scope.
- Single deployment per environment: one backend API, static frontend assets deployed alongside.

## Actors
- Receptionist (staff): login, start rentals, assign bikes, hand to guest, handle returns/lost.
- Guest: limited guest mode to fill room/bed, return time, read T&C, sign.
- System: enforces business rules, prevents double-rent, derives statuses, supports partial returns/lost.

## Core Domain Invariants
- One open rental per bike: a bike can appear in at most one `RentalItem` with `status = RENTED`.
- OOO bikes cannot be rented; reject assignment when status is OOO.
- Rental status derives from rental items + time:
  - All items RETURNED/LOST → rental CLOSED.
  - Any RENTED and now > `due_at + grace` → rental OVERDUE.
  - Any RENTED and now ≤ `due_at + grace` → rental ACTIVE.
- Bike status derives from rentals + OOO:
  - Exists RENTED rental item → bike RENTED.
  - Not OOO and no RENTED item → bike AVAILABLE.
- Signature must reflect the final bike list and T&C version at signing time.

> See `db_schema.mdc` for technical constraint implementation details (I1-I5).

## Design for Forgiveness
- Per-bike actions (return, mark lost); contract-level return-all when applicable.
- Confirmation dialogs for critical actions with rental/bike context.
- Undo for single-bike returns (short-lived); optional for batches/return-all.
- Clear feedback after actions (status badges, toasts/banners).
- Overdue is informational; must not block returns or closure.

## Login & Authentication
- POST `/api/auth/login` with hotel code + password; returns JWT containing hotelId/hotelCode/exp and hotel name.
- Failed login shows generic error (no credential hinting).
- **Token lifetime:** 8–12 hours (covers one staff shift); forces daily re-authentication.
- **HTTPS:** Required in production; JWT must only travel over encrypted connections.
- **Future:** Individual staff accounts for audit trails (who performed which action). Out of scope for MVP.

> See `api_rules.mdc` for detailed auth handling (401 responses, hotel ID derivation from JWT).
> See `db_schema.mdc` for password hashing requirements.

## Home Screen
- Summary counts scoped to hotel: bikes AVAILABLE/RENTED/OOO; rentals ACTIVE/OVERDUE. Counts refresh after data changes.
- Active rentals list shows ACTIVE + OVERDUE, overdue highlighted and sorted first (e.g., overdue first then by `due_at` asc).
- Each rental row shows id/label, room/bed, due datetime, status badge, bikes out vs total; rows are clickable to Rental Detail.
- Simple search/filter by at least one of room number, bike number, or rental id.

## Starting a Rental (Single iPad)
> **Note:** No backend "draft" entity. Bike assignment is managed in frontend state; backend validates availability atomically at rental creation.

### Staff Mode – Assign Bikes
- "New rental" opens bike assignment screen (frontend state only, no backend call).
- Add bike by number: validate via API lookup; errors for not found, RENTED, or OOO; only AVAILABLE bikes allowed; no duplicates in local list.
- Display assigned bikes; allow removal before guest mode.
- Cannot proceed to guest mode without at least one bike.
- “Continue & hand to guest” switches to guest mode and hides staff navigation.

### Guest Mode – Details, T&C, Signature
- Show assigned bike numbers (read-only), inputs for return date/time, room number, optional bed number, full T&C text, signature pad, and Confirm button.
- No staff navigation in guest mode; no guest name/email/phone collected.
- Validation: return time in future; room_number non-empty; bed_number may be null; signature must be present.
- On confirm: require ≥1 bike, valid form, signature; show clear errors while preserving inputs on failure.

### Create Rental (Atomic)
- On confirm: call `POST /api/rentals` with all data (bike numbers, room/bed, return datetime, T&C version, signature).
- Backend validates all bikes still AVAILABLE (not RENTED/OOO); if any unavailable, return 409 with details.
- On success: store signature, compute start_at = now (UTC), due_at from return datetime.
- Create Rental (status ACTIVE) with room/bed, tnc_version, signature_id, start_at, due_at.
- For each assigned bike: create RentalItem with status RENTED; set bike status RENTED.
- All changes atomic in single transaction.
- After success: show “Rental created” confirmation; rental appears in Home Screen Active list.

## Rental Detail (Staff Mode)
- Shows rental id/label, room/bed (bed may be null), start_at, due_at, rental status badge.
- Displays contract artifacts: signature preview/link so staff can verify the guest’s signature after creation.
- Provides a “View contract” action to open the full contract (bike list, room/bed, times, T&C version, signature) so staff can review accountability.
- Lists bikes in rental with bike_number, type (if available), item status badge; for RENTED items: actions Return bike, Mark lost; RETURNED shows returned_at; LOST shows indicator + optional reason.
- If any RENTED items, show “Return all remaining bikes”; none of these actions when rental CLOSED.

## Returns
### Single-bike Return
- Available only for RENTED items; confirmation dialog naming bike/rental.
- On confirm: item → RETURNED, returned_at = now; bike → AVAILABLE (unless blocked by OOO rule); recalc rental status (CLOSED when all returned/lost; otherwise ACTIVE/OVERDUE by time).
- UI updates row; toast/banner e.g., “Bike #17 returned. [Undo]”.
- Undo (within window): item back to RENTED, clear returned_at, bike → RENTED, recalc rental status.

### Multi-bike Return
- Allow selecting multiple RENTED items; “Return selected bikes” enabled when ≥1 selected.
- Confirmation summarises count and bike numbers.
- On confirm: apply single-bike return rules to each; recalc rental after batch; show summary notification (e.g., “3 bikes returned.”).
- Undo optional; if omitted, confirmations act as safeguard.

### Return All Remaining Bikes (Close Rental)
- Visible when rental not CLOSED and ≥1 RENTED item.
- Confirmation lists remaining bikes; on confirm: set each RENTED item to RETURNED with returned_at = now; bikes → AVAILABLE (if not OOO); rental → CLOSED with return_at = now.
- After success: rental shows CLOSED; no further return/lost actions; show confirmation message. Undo optional.

## Marking Bikes as Lost
- Available for RENTED items; confirmation dialog with optional reason text.
- On confirm: item → LOST, store reason; bike not available for new rentals without intervention (e.g., OOO or dedicated lost handling); recalc rental status (CLOSED when all returned/lost, set return_at = now).
- Undo optional; dialog should signal action significance.

## Overdue & Forgotten Rentals
- Rental becomes OVERDUE when any item RENTED and now > `due_at + grace`.
- Overdue highlighted in counts, lists, and detail view.
- Overdue does not block returns, return-all, or closure; staff can clean up forgotten contracts after verifying bikes.
- Optional future logging of overdue duration; not required for MVP behaviour.

## Settings (Post-MVP Architecture)

The system architecture supports hotel-level configuration. MVP uses hardcoded defaults; settings UI and endpoints are post-MVP.

### Configurable Settings
- **Rental duration options:** Array of allowed durations in hours (e.g., `[24, 48, 72]`). Guest mode presents these as choices.
- **Grace period:** Minutes after `due_at` before rental becomes OVERDUE (default 0).
- **Terms & Conditions:** T&C text displayed in guest mode; version tracked per rental.

### MVP Behaviour
- Service layer reads from `HotelSettings` entity but falls back to hardcoded defaults if null/missing.
- No settings UI or endpoints in MVP.

### Post-MVP
- `/settings` route for hotel configuration (rental durations, grace, T&C editor).
- `GET /api/settings` and `PATCH /api/settings` endpoints.
- Bike creation UI (`POST /api/bikes`).

## Behaviour Summary for Implementers
- Rentals are multi-line contracts (Rental + RentalItems).
- Enforce no bike can be in more than one RENTED item at once.
- Derive rental status from item statuses plus `due_at + grace` (from settings, default 0).
- Support return flows: single, multi-select, all remaining.
- Apply confirmation + undo patterns; keep guest interaction minimal.
- At signing, lock T&C version and bike list with stored signature.

## React Routes (MVP)
- `/login` – hotel credential login; on success, store JWT and redirect to `/`.
- `/` – home overview (counts + active/overdue list); click rental to detail.
- `/rentals/new` – staff bike assignment (frontend state).
- `/rentals/new/guest` – guest mode form + T&C + signature; confirm → create rental → redirect to `/rentals/:id`.
- `/rentals/:id` – rental detail with per-bike actions, multi-return, return-all.
- `/bikes` – inventory view: search by bike number, filter by status, mark OOO/fixed (not if actively rented).
- `/maintenance` – optional; can be `/bikes?status=OOO`; export OOO to CSV.
