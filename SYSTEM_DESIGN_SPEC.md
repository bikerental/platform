# Bike Rental System – System Design Spec (v1.0)

> Scope of v1.0:  
> This spec covers **core flows** for staff-only bike rentals:
> - Login
> - Home screen overview
> - Starting a rental (single iPad, staff + guest)
> - Viewing rentals
> - Partial returns and full returns
> - Marking bikes as lost
> - Overdue behaviour and “forgotten” contracts
>
> Other MVP features (out-of-order, maintenance, exports, GDPR retention, etc.) will be added in later sections/versions.

## 0. Architecture Overview

### 0.1 Repository layout (monorepo, split-friendly)

The project is organised as a monorepo to keep backend, frontend, and documentation aligned and to improve AI-assisted development across the stack.

```text
/steelhouse-bike
  /backend        # Spring Boot API
  /frontend       # React SPA
  /docs
    SYSTEM_DESIGN_SPEC.md
```

This structure is intentionally split-friendly:

* `/backend` and `/frontend` have independent build systems (`pom.xml`, `package.json`).
* The frontend communicates with the backend only over HTTP using `API_BASE_URL`.
* No shared compilation steps or cross-folder imports.

### 0.2 Backend

* Java 21
* Spring Boot
* Spring Web (REST)
* Spring Security (JWT)
* Spring Data JPA
* MySQL
* Lombok (restricted usage)
* Maven

### 0.3 Frontend

* React SPA
* Tailwind CSS
* React Router
* Minimal state via hooks (`useState`, `useEffect`)
* No global state libraries in v1

### 0.4 Deployment intent (MVP)

The MVP targets a single deployment per environment with:

* A single Spring Boot backend API.
* A separate React frontend build served as static assets (local dev) and deployed alongside the backend in production.

---

## 1. Context & Actors

### 1.1 System context

The system is a **staff-only web application** for managing bike rentals at a hostel/hotel. It is mainly used on an **iPad behind the reception**. The iPad is usually in staff possession and is only handed to the guest briefly to fill in rental details and sign the contract.

### 1.2 Actors

- **Receptionist (Staff)**  
  - Logs in.  
  - Starts new rentals.  
  - Assigns bikes (by typing the bike numbers printed on the keys).  
  - Hands the iPad to the guest for completing details and signing.  
  - Handles returns (partial and full).  
  - Marks bikes as lost if they cannot be returned.

- **Guest**  
  - Receives the iPad in a limited “guest mode”.  
  - Fills in room/bed and return time.  
  - Reads Terms & Conditions.  
  - Signs digitally to accept the contract.

- **System**  
  - Enforces business rules and state transitions.  
  - Guarantees that bikes cannot be double-rented.  
  - Calculates rental status (active/overdue/closed).  
  - Supports partial returns and contract closure.

---

## 2. Core Domain Model & States

This spec uses conceptual entities. The concrete implementation (e.g., DB tables) must preserve these relationships and invariants.

### 2.1 Bike

Represents a physical bike that can be rented.

- `bike_id` – internal identifier.
- `bike_number` – human-visible number printed on the bike/key (unique per hotel).
- `bike_type` – optional category (e.g., Adult, Kids).
- `status`:
  - `AVAILABLE`
  - `RENTED`
  - `OOO` (out of order / not rentable)

### 2.2 Rental (contract)

Represents one rental contract between the hotel and a guest.

- `rental_id` – internal identifier.
- `hotel_id` – which hotel/hostel this rental belongs to.
- `status`:
  - `ACTIVE` – at least one bike is currently out.
  - `OVERDUE` – at least one bike is out, and the rental is past due (including grace).
  - `CLOSED` – all bikes have been returned or marked lost.
- `start_at` – when the rental is finalized (after guest signs).
- `due_at` – expected return time, computed from `start_at` and the selected duration (e.g., 24/48/72 hours) and optional grace.
- `return_at` – when the **last** bike in this rental is returned or marked lost.
- `room_number`
- `bed_number`
- `tnc_version` – version identifier for the Terms & Conditions.
- `signature_id` – reference to a stored digital signature.

A rental can contain **one or more bikes**.

### 2.3 RentalItem (per-bike line item)

Represents a single bike within a rental.

- `rental_item_id`
- `rental_id`
- `bike_id`
- `status`:
  - `RENTED` – the bike is currently out with the guest.
  - `RETURNED` – the bike has been returned.
  - `LOST` – the bike or key cannot be returned.
- `returned_at` – timestamp when this specific bike was returned (if `RETURNED`).
- `lost_reason` – optional short note (e.g., “key lost”, “bike missing”).

### 2.4 Derived status rules (invariants)

The system must always enforce the following invariants:

**I1. One open rental per bike**  
A bike can be part of **at most one** `RentalItem` with `status = RENTED` at any time.  
No two active/overdue rentals may share the same `bike_id`.

**I2. OOO bikes cannot be rented**  
If `bike.status = OOO`, the bike cannot be assigned to new rentals.

**I3. Rental status is derived from RentalItems + time**

- If **all** `rental_items.status` are in `{RETURNED, LOST}`:  
  `rental.status = CLOSED`.
- Else if **at least one** `rental_item.status = RENTED` and current time > `due_at + grace`:  
  `rental.status = OVERDUE`.
- Else (there is at least one `RENTED` item, and now ≤ `due_at + grace`):  
  `rental.status = ACTIVE`.

**I4. Bike status is driven by participation in rentals + OOO**

- If there exists a `RentalItem` for that bike with `status = RENTED`:  
  `bike.status = RENTED`.
- Else, if bike is not OOO:  
  `bike.status = AVAILABLE`.
- If bike is explicitly put OOO (out of order), that state takes precedence, and the bike cannot be rented.

**I5. Signature must reflect the final bike list**  
At the time of signing, the guest must see the **actual list of bike numbers** they are renting, and the signature must be linked to that list and the active `tnc_version`.

---

## 3. Global Design Principles (Design for Forgiveness)

The UI and backend must support forgiving interactions, anticipating human errors and minimising their impact.

**D1. Per-bike actions**  
All bike-related actions (return, mark lost) are performed on individual `RentalItem`s, not on the rental as a whole, except when intentionally returning all remaining bikes.

**D2. Multiple return paths**

- Return a **single bike**.
- Return **selected bikes**.
- Return **all remaining bikes** (closing the rental).

**D3. Confirmation dialogs**  
Critical actions (return, mark lost, return all) must be confirmed with clear dialogs indicating:

- Which bike(s) are affected.
- Which rental is being changed.

**D4. Undo where feasible**  
After returning a bike (or a group of bikes), the UI should offer a short-lived **Undo** option to revert the change.

**D5. Clear feedback**  
After each action, the user shall receive clear feedback:

- Updated status badges.
- Toast or banner messages (e.g., “Bike #17 returned. [Undo]”).

**D6. No blocking on overdue**  
Being `OVERDUE` should not block staff from returning bikes or closing the rental; it is an informational state.

---

## 4. Login & Authentication (JWT)

### 4.1 Login

**Goal:** Authenticate staff for a specific hotel and issue a JWT token that scopes all access to that hotel.

**Behaviour:**
- Staff opens the application on the iPad.
- Enters hotel-specific credentials (e.g., hotel code + password).
- On success, the backend returns a JWT access token.
- The frontend stores the token and uses it on all subsequent API calls.
- Staff is redirected to the Home Screen (section 5).

**Requirements:**
- L1. The login screen must collect the minimum required credentials (e.g., `hotelCode` and `password`).
- L2. The backend must expose `POST /api/auth/login`.
- L3. On successful authentication, the backend must return an access token containing at least:
  - `hotelId`
  - `hotelCode`
  - `exp` (expiry)
- L4. All subsequent protected API calls must include:
  - `Authorization: Bearer <token>`
- L5. The system must never accept or trust `hotel_id` from client requests.  
  All scoping must be derived from the JWT context.
- L6. On failed login, the system must show a generic error without revealing which credential was wrong.
- L7. The UI must clearly indicate which hotel the user is logged in as (e.g., “SteelHouse Copenhagen”).

### 4.2 Token lifetime and logout

**Requirements:**
- L8. The access token must have a reasonable expiry suitable for staff usage.
- L9. When a token is expired or invalid, the backend must respond with HTTP 401.
- L10. The frontend must handle 401 by clearing the token and redirecting to `/login`.

---

## 5. Home Screen – Main Page After Login

After login, staff land on the **Home Screen**, which provides:

1. A summary header with key counts.
2. A list of all **active and overdue rentals**.

### 5.1 Summary Header (Counts)

**Goal:** Give instant overview of current load and potential issues.

**Behaviour:**

- At the top, the system shows aggregated counts for bikes and rentals.

**Requirements:**

- H1. The Home Screen must display counts (scoped to the current hotel) for:
  - Number of bikes with `status = AVAILABLE`.
  - Number of bikes with `status = RENTED`.
  - Number of bikes with `status = OOO`.
  - Number of rentals with `status = ACTIVE`.
  - Number of rentals with `status = OVERDUE`.
- H2. These counts must update whenever underlying data changes (e.g., after returns or OOO changes).

### 5.2 Active Rentals List (Main Content)

**Goal:** Show which rentals are currently out and which are overdue, and allow staff to jump into details.

**Behaviour:**

- Below the summary header, the main content is a list of rentals where `status ∈ {ACTIVE, OVERDUE}`.
- Overdue rentals are highlighted and shown first.

**Requirements:**

- H3. The Home Screen must list all rentals with `status = ACTIVE` or `status = OVERDUE`.
- H4. For each listed rental, show at least:
  - Rental id/label.
  - Room number and bed number.
  - Due date and time.
  - Status badge (`ACTIVE` or `OVERDUE`).
  - Count of bikes still out (e.g., “2/4 bikes out”).
- H5. The list must sort **OVERDUE** rentals first, then ACTIVE rentals (e.g., by `due_at` ascending).
- H6. Each rental in the list must be clickable to open the **Rental Detail** view.
- H7. The Home Screen must provide simple search/filter options for at least one of:
  - Room number.
  - Bike number.
  - Rental id.

---

## 6. Starting a Rental (Single iPad Flow)

All steps occur on **one iPad** behind the reception. The iPad has two logical modes:

- **Staff Mode** – full navigation, staff-only actions.
- **Guest Mode** – limited, kiosk-like screen for the guest to fill details and sign.

High-level flow:

1. Staff Mode: Create draft + assign bikes by typing bike numbers (from keys).
2. Guest Mode: Guest fills return date/time, room/bed, reads T&C, signs.
3. System finalizes the draft into an ACTIVE rental.

### 6.1 Staff Mode – Create Rental Draft & Assign Bikes

**Narrative:**

1. Staff taps **“New rental”** on the Home Screen (Staff Mode).
2. System creates a `RentalDraft` object (conceptual), tied to `hotel_id`.
3. Staff asks how many bikes the guest wants and takes that many keys.
4. For each key, staff types the **bike number** printed on the key into an input field.
5. System validates each bike number and adds it to the draft.
6. Staff checks the list of bikes and, when satisfied, taps **“Continue & hand to guest”** to switch to Guest Mode for this draft.

**Requirements:**

- R1. Tapping “New rental” must create a new `RentalDraft` with:
  - `hotel_id`
  - empty list of assigned bikes
  - no guest info yet.
- R2. Staff must be able to add bikes by entering a `bike_number` in a text input and tapping “Add bike”.
- R3. When staff attempts to add a bike:
  - The system must look up the bike by `bike_number` within the current `hotel_id`.
  - If the bike does not exist → show a clear error (e.g., “Bike #X not found”) and do not add.
  - If the bike exists but `status ∈ {RENTED, OOO}` → show a clear error (e.g., “Bike #X is not available”) and do not add.
  - Only bikes with `status = AVAILABLE` may be added.
- R4. The list of assigned bikes must be shown on screen, including each bike number (and optionally type).
- R5. Staff must be able to remove any previously added bike from the draft (e.g., via a “Remove” or “X” button) prior to switching to Guest Mode.
- R6. Duplicate bike numbers must not be allowed in the same draft.
- R7. Staff must not be able to proceed to Guest Mode unless at least **one** bike has been successfully added.
- R8. When staff taps **“Continue & hand to guest”**, the UI must switch into Guest Mode for this draft and hide any staff navigation and controls.

### 6.2 Guest Mode – Rental Details, T&C, Signature

**Narrative:**

1. Staff physically hands the iPad to the guest.  
2. Guest sees a screen showing:
   - Assigned bike numbers (read-only).
   - A form to enter:
     - Return date.
     - Return time.
     - Room number.
     - Bed number.
   - The Terms & Conditions text.
   - A signature pad.
3. Guest reads T&C, fills the form, signs, and taps **“Confirm rental”**.
4. After success, a simple confirmation is shown and the receptionist takes the iPad back.

**Requirements:**

- R9. Guest Mode must show:
  - The list of assigned bike numbers for the draft (read-only).
  - Input fields for `return_date`, `return_time`, `room_number`, `bed_number`.
  - The full T&C text (scrollable if necessary).
  - A signature pad and a “Confirm rental” button.
- R10. In Guest Mode, staff-only navigation and controls must not be visible or accessible.
- R11. The form must **not** collect guest name, email, or phone (data minimisation).
- R12. Form validation must ensure:
  - `return_date` and `return_time` represent a time in the future.
  - `room_number` is not empty.
  - `bed_number` is not empty (if required by business rules).
- R13. The signature pad must allow the guest to draw a signature, clear it, and redraw before confirmation.
- R14. When “Confirm rental” is pressed:
  - The system must verify:
    - At least one bike is assigned to the draft.
    - The form passes validation.
    - A signature has been provided (not blank).
  - If any condition fails, the system must show a clear error and keep the user on the same screen with their data preserved where possible.

### 6.3 Finalize Draft → Active Rental

This describes what the system does internally when the guest confirms.

**Narrative:**

1. System receives submission for the draft:
   - `return_date`, `return_time`
   - `room_number`, `bed_number`
   - Signature data
2. System stores signature, computes rental times, and creates a finalized rental.

**Requirements:**

- R15. On successful confirmation in Guest Mode, the system must:
  - Store the signature as a `Signature` record, obtaining a `signature_id`.
  - Compute `start_at` as the current timestamp.
  - Compute `due_at` from the guest-provided `return_date` and `return_time`.
  - Determine current `tnc_version`.
- R16. The system must create exactly one `Rental` record with:
  - `hotel_id`
  - `status = ACTIVE`
  - `start_at`, `due_at`
  - `room_number`, `bed_number`
  - `tnc_version`
  - `signature_id`
- R17. For each bike in the draft, the system must:
  - Create a corresponding `RentalItem` linked to the new `Rental`.
  - Set `rental_item.status = RENTED`.
  - Set `rental_item.returned_at = null`, `lost_reason = null`.
  - Set the `bike.status = RENTED`.
- R18. All changes in R16–R17 must happen atomically; partial creation is not allowed.
- R19. If any bike has become unavailable (e.g., due to a race condition) between assignment and finalization:
  - The system must refuse to finalize the rental.
  - Show an appropriate error message (e.g., “One or more bikes are no longer available; please contact reception”).
  - The draft must remain in a state that staff can review or restart.
- R20. After successful finalization:
  - The iPad must show a simple “Rental created” confirmation screen in Guest Mode.
  - The receptionist is expected to take the iPad back and return it to Staff Mode.
  - The new rental must appear in the Home Screen’s Active Rentals list.

---

## 7. Rental Detail View (Staff Mode)

**Goal:** Provide a full overview of a specific rental and allow staff to perform partial returns, full returns, and mark bikes lost.

**Narrative:**

1. From the Home Screen’s Active Rentals list, staff taps a rental.  
2. System opens **Rental Detail**, showing contract info and all bikes in that rental.  
3. Staff can perform actions per bike or on all remaining bikes.

**Requirements:**

- RD1. Rental Detail must display:
  - Rental id/label.
  - Room number and bed number.
  - `start_at`.
  - `due_at`.
  - Rental status (`ACTIVE`, `OVERDUE`, or `CLOSED`).
- RD2. Rental Detail must show a “Bikes in this rental” section listing all `RentalItems` with, for each:
  - `bike_number`.
  - Bike type (if available).
  - `rental_item.status` badge (`RENTED`, `RETURNED`, `LOST`).
  - If `status = RENTED`:
    - A “Return bike” action/button.
    - A “Mark as lost” action/button.
  - If `status = RETURNED`:
    - `returned_at` timestamp.
  - If `status = LOST`:
    - A visible lost indicator and optionally the `lost_reason`.
- RD3. If the rental has at least one `RENTED` bike, a contract-level action **“Return all remaining bikes”** must be visible.
- RD4. If the rental is `CLOSED`, no return or lost actions must be available.

---

## 8. Returns (Partial and Full)

### 8.1 Partial Return – Single Bike

**Narrative:**

1. Guest returns one of the bikes from a rental.  
2. Staff opens Rental Detail.  
3. Staff taps “Return bike” on the relevant bike row.  
4. System confirms and updates only that bike, leaving others unchanged.

**Requirements:**

- RB1. “Return bike” must be available only for `RentalItems` where `status = RENTED`.
- RB2. When “Return bike” is tapped, the system must show a confirmation dialog:
  - E.g., “Return bike #<bike_number> for rental <rental-id>? [Cancel] [Confirm]”
- RB3. On confirmation:
  - Set `rental_item.status = RETURNED`.
  - Set `rental_item.returned_at = now`.
  - Update `bike.status = AVAILABLE` (unless prevented by OOO or another rule).
  - Recalculate `rental.status` according to the invariants:
    - If at least one `RentalItem` is still `RENTED`, rental remains `ACTIVE` or becomes `OVERDUE` based on current time and `due_at + grace`.
    - If all `RentalItems` are `RETURNED` or `LOST`, set `rental.status = CLOSED` and `rental.return_at = now`.
- RB4. After a successful return, the Rental Detail UI must update the bike row to show `status = RETURNED` and `returned_at`.
- RB5. After the update, a short-lived notification (toast/banner) must appear:
  - E.g., “Bike #17 returned. [Undo]”
- RB6. If the user taps **“Undo”** within the allowed window:
  - Set `rental_item.status` back to `RENTED`.
  - Clear or revert `rental_item.returned_at`.
  - Set `bike.status = RENTED`.
  - Recalculate `rental.status` again.

### 8.2 Partial Return – Multiple Bikes

**Narrative:**

1. Guest returns multiple bikes from the same rental at once.  
2. Staff selects those bikes in the Rental Detail UI (e.g., using checkboxes).  
3. Staff taps “Return selected bikes”.  
4. System confirms and applies the return logic to each selected bike.

**Requirements:**

- RB7. Rental Detail must allow staff to select multiple bikes with `status = RENTED` (e.g., checkboxes).
- RB8. A “Return selected bikes” action must be visible/enabled when at least one bike is selected.
- RB9. On clicking “Return selected bikes”, the system must show a confirmation dialog summarizing the action:
  - E.g., “Return X bikes on rental <rental-id>? Bikes: #5, #7, #9 [Cancel] [Confirm]”
- RB10. On confirmation:
  - Apply RB3 (status changes) to each selected bike.
  - Recalculate `rental.status` after all selected bikes have been updated.
- RB11. After success, the UI must reflect the new statuses and provide a summary notification, such as:
  - “3 bikes returned.”
- RB12. Undo behaviour for multi-bike returns may be:
  - Implemented as an undo for the entire batch, or
  - Omitted if too complex, as long as this is clearly documented and confirmation dialogs are present.

### 8.3 Return All Remaining Bikes (Close Rental)

**Narrative:**

1. Guest returns all remaining bikes for a rental.  
2. Staff taps “Return all remaining bikes” in Rental Detail.  
3. System confirms and returns all bikes still marked as `RENTED`.  
4. Rental becomes `CLOSED`.

**Requirements:**

- RB13. “Return all remaining bikes” must be visible when:
  - Rental is not `CLOSED`, and
  - At least one `RentalItem.status = RENTED`.
- RB14. On tap, the system must show a confirmation dialog such as:
  - “Return all remaining bikes on rental <rental-id> and close the contract? Remaining bikes: #3, #4 [Cancel] [Confirm]”
- RB15. On confirmation:
  - For each `RENTED` RentalItem:
    - Set `status = RETURNED`.
    - Set `returned_at = now`.
    - Set `bike.status = AVAILABLE` (if not OOO).
  - Set `rental.status = CLOSED`.
  - Set `rental.return_at = now`.
- RB16. After success:
  - Rental Detail view must show `status = CLOSED`.
  - No further return or lost actions must be available.
  - A confirmation message must be shown (e.g., “Rental <id> closed. All bikes returned.”).
- RB17. Undo for this action is optional; if not implemented, the confirmation dialog acts as the main safeguard.

---

## 9. Marking Bikes as Lost

**Narrative:**

1. Guest reports that a bike or key is lost (cannot be physically returned).  
2. Staff opens the relevant rental in Rental Detail.  
3. Staff taps “Mark as lost” on the specific bike.  
4. System confirms, optionally captures a reason, and marks that bike as `LOST`.

**Requirements:**

- RL1. “Mark as lost” must be available for `RentalItems` where `status = RENTED`.
- RL2. On tap, the system must show a confirmation dialog:
  - E.g.,  
    “Mark bike #<bike_number> as LOST for rental <rental-id>?  
     Optional note (e.g., ‘key lost’, ‘bike missing’): [text field]  
     [Cancel] [Confirm]”
- RL3. On confirmation:
  - Set `rental_item.status = LOST`.
  - Store `lost_reason` (possibly empty).
  - Adjust `bike.status` so that the bike is not available for new rentals without further intervention (e.g., `OOO` or a dedicated lost state if designed later).
  - Recalculate `rental.status` according to invariants:
    - If any other `RentalItem.status = RENTED` exists, rental remains `ACTIVE` or `OVERDUE`.
    - If all are `RETURNED` or `LOST`, rental becomes `CLOSED` and `return_at = now`.
- RL4. Undo for “Mark as lost” is optional; if not implemented, the dialog text should clearly indicate that this is a significant, potentially irreversible action.

---

## 10. Overdue & Forgotten Rentals

**Narrative:**

- When a rental passes its `due_at + grace` time, it becomes `OVERDUE`.  
- Some rentals might remain open even though bikes are physically back (forgotten to close).  
- Staff must be able to detect these and close them after verifying bikes physically.

**Requirements:**

- O1. A rental becomes `OVERDUE` when:
  - At least one `RentalItem.status = RENTED`, and
  - Current time > `due_at + grace`.
- O2. Overdue rentals must be visibly highlighted in:
  - Home Screen counts (overdue rentals).
  - Home Screen Active Rentals list (e.g., red badge “Overdue”).
- O3. Rental Detail must show that the rental is overdue (badge, text).
- O4. Overdue status must **not** block staff from:
  - Returning bikes (single or multiple).
  - Returning all remaining bikes and closing the rental.
- O5. Staff must be able to use the return actions to clean up “forgotten” rentals after physically confirming bikes are present.
- O6. If needed later, the system may log or expose how long rentals were overdue (for analysis), but this is not required for basic behaviour.

---

## 11. Behaviour Summary for Implementers / AI

When generating code or tests from this spec:

1. Model rentals as **multi-line contracts**: a `Rental` with multiple `RentalItems`.
2. Enforce that **no bike** can be part of more than one active/overdue rental (`status = RENTED`) at any time.
3. Always derive `rental.status` from:
   - The statuses of all `RentalItems`, and
   - The relationship between current time and `due_at + grace`.
4. Support **three return flows**:
   - One bike at a time.
   - Multiple selected bikes at once.
   - All remaining bikes in one action (close rental).
5. Apply **design for forgiveness**:
   - Confirm dialogs for critical actions.
   - Undo for at least single-bike returns (short-lived).
   - Clear visual status updates and feedback messages after actions.
6. Keep guest interaction minimal and simple:
   - Guest only sees assigned bikes, rental details form, T&C, and signature pad.
   - No staff navigation in Guest Mode.
   - No personal data beyond room/bed and signature.
7. Ensure that at the moment of signing, the T&C version and the bike list are fixed and stored with the rental.

---
## 12. Bike Inventory & Out-of-Order (OOO) Management

## 13. Maintenance & OOO Export

## 14. GDPR & Data Retention

## 15. Coding Guidelines & Project Conventions

## 16. Architecture Decisions (Locked for MVP)

This section records the key architectural decisions for the MVP.  
The decisions are intentionally pragmatic given the scope (solo developer, 2–3 weeks).

### AD1. Repository strategy: Monorepo (split-friendly)

**Decision:** Use a monorepo containing backend, frontend, and documentation.

**Layout:**
```text
/steelhouse-bike
  /backend
  /frontend
  /docs
```

**Rationale:**

* Reduces context switching between API and UI.
* Improves AI-assisted development by keeping full project context in one workspace.
* Enables easy future split into polyrepos.

**Split-friendly rules:**

* No shared build steps across `/backend` and `/frontend`.
* Separate dependency manifests for each folder.
* Frontend uses `API_BASE_URL`.

---

### AD2. Backend application style

**Decision:** Use a single Spring Boot application (monolith), structured with feature-based packages.

**Rationale:**

* Reduces deployment complexity.
* Fits an internal staff tool.

---

### AD3. Backend stack

**Decision:**

* Java 21
* Spring Boot
* Spring Web (REST)
* Spring Security (JWT)
* Spring Data JPA
* Lombok (restricted)
* Maven

**Rationale:** Aligns with course tooling and supports fast iteration.

---

### AD4. Database

**Decision:** Use MySQL for MVP.

**Rationale:**

* Familiar and low risk.
* Business invariants enforced in service layer + tests.

---

### AD5. Frontend

**Decision:** Implement a minimal React SPA with Tailwind CSS.

**Rationale:**

* Modern, responsive UI for desktop + iPad + mobile.
* Strong learning value without excessive complexity.

**Scope discipline:**

* No Redux or advanced global state in v1.
* Hooks + React Router only.

---

### AD6. Authentication

**Decision:** JWT access tokens.

**Rationale:**

* Aligns with teaching patterns.
* Supports decoupled React + Spring architecture.

**Security constraints:**

* JWT must include `hotelId`.
* Backend must not trust client-sent hotel identifiers.
* All queries are scoped using the authenticated context.

---

### AD7. Code organisation

**Decision:** Feature-based packages with internal layering.

**Rules:**

* Thin controllers.
* DTOs at boundaries.
* Business logic in services.
* No JPA entities exposed from controllers.

---

### AD8. Lombok usage

**Allowed:**

* `@Getter`, `@Setter`
* `@NoArgsConstructor`, `@AllArgsConstructor`
* `@RequiredArgsConstructor`

**Avoid:**

* `@Data` on JPA entities.

---

### AD9. Testing

**Decision:** Prioritise unit tests for service-layer business rules.

**Minimum targets:**

* Create rental with multiple bikes.
* Partial returns.
* Return all remaining bikes.
* Mark bike lost.
* Prevent assigning OOO or already-rented bikes.

---

## 17. React Route Map (MVP)

The React SPA for v1 is intentionally minimal.

### Routes

- `/login`
  - Hotel credential login.
  - On success, store JWT and redirect to `/`.

- `/`
  - Home screen after login.
  - Shows summary counts and list of ACTIVE + OVERDUE rentals.
  - Clicking a rental opens `/rentals/:id`.

- `/rentals/new`
  - Staff creates a rental draft.
  - Assigns bikes by typing bike numbers.

- `/rentals/new/guest`
  - Guest-mode style view.
  - Guest fills:
    - return date
    - return time
    - room number
    - bed number
  - Guest reads T&C and signs.
  - Confirm → finalize rental → redirect to `/rentals/:id`.

- `/rentals/:id`
  - Rental detail.
  - Per-bike actions:
    - Return bike
    - Mark lost
  - Batch actions:
    - Return selected bikes
    - Return all remaining bikes

- `/bikes`
  - Inventory view.
  - Search by bike number.
  - Filter by status.
  - Mark OOO with note.
  - Mark fixed if not actively rented.

- `/maintenance`
  - Optional MVP screen.
  - Can be merged into `/bikes?status=OOO`.
  - Export OOO list to CSV.

### UI rules

- Responsive layout for iPad + desktop + mobile.
- Large touch targets and minimal typing.
- No complex global state in v1.
- All API calls use `API_BASE_URL`.

---

## 18. API Contracts (MVP)

All endpoints are under `/api`.  
All protected endpoints require:
`Authorization: Bearer <accessToken>`

The backend must scope all data using `hotelId` from JWT claims.

---

### 1) Auth

#### POST `/api/auth/login`

**Request**
```json
{
  "hotelCode": "STEELHOUSE",
  "password": "********"
}
```

**Response 200**

```json
{
  "accessToken": "eyJhbGciOi...",
  "hotelName": "SteelHouse Copenhagen"
}
```

**Errors**

* 401/400 with a generic message.

---

### 2) Home Overview

#### GET `/api/overview`

**Response 200**

```json
{
  "counts": {
    "bikesAvailable": 0,
    "bikesRented": 0,
    "bikesOoo": 0,
    "rentalsActive": 0,
    "rentalsOverdue": 0
  },
  "activeRentals": [
    {
      "rentalId": 123,
      "roomNumber": "402",
      "bedNumber": "B",
      "dueAt": "2025-12-05T18:00:00",
      "status": "ACTIVE",
      "bikesOut": 2,
      "bikesTotal": 4
    }
  ]
}
```

---

### 3) Bikes

#### GET `/api/bikes?status=&q=`

**Response 200**

```json
[
  {
    "bikeId": 5,
    "bikeNumber": "5",
    "bikeType": "ADULT",
    "status": "AVAILABLE",
    "oooNote": null,
    "oooSince": null
  }
]
```

#### PATCH `/api/bikes/{bikeId}/ooo`

**Request**

```json
{
  "note": "Flat tire"
}
```

**Response 200**

```json
{
  "bikeId": 5,
  "status": "OOO",
  "oooNote": "Flat tire",
  "oooSince": "2025-12-05T10:15:00"
}
```

#### PATCH `/api/bikes/{bikeId}/available`

**Rules**

* Must fail if bike is currently part of a RENTED rental item.

---

### 4) Rentals – Draft + Finalize

#### POST `/api/rentals/drafts`

**Response 201**

```json
{
  "draftId": "d-abc123",
  "assignedBikes": []
}
```

#### POST `/api/rentals/drafts/{draftId}/bikes`

**Request**

```json
{
  "bikeNumber": "5"
}
```

**Response 200**

```json
{
  "draftId": "d-abc123",
  "assignedBikes": [
    { "bikeId": 5, "bikeNumber": "5", "bikeType": "ADULT" }
  ]
}
```

**Errors**

* 404 if bike number not found.
* 409 if bike is OOO or already rented.

#### DELETE `/api/rentals/drafts/{draftId}/bikes/{bikeId}`

Removes a bike from the draft.

---

#### POST `/api/rentals/drafts/{draftId}/finalize`

**Request**

```json
{
  "roomNumber": "402",
  "bedNumber": "B",
  "returnDate": "2025-12-06",
  "returnTime": "12:00",
  "tncVersion": "v1",
  "signatureBase64Png": "data:image/png;base64,iVBORw0..."
}
```

**Response 201**

```json
{
  "rentalId": 123,
  "status": "ACTIVE",
  "startAt": "2025-12-05T10:30:00",
  "dueAt": "2025-12-06T12:00:00",
  "roomNumber": "402",
  "bedNumber": "B",
  "bikes": [
    { "bikeId": 5, "bikeNumber": "5", "status": "RENTED" }
  ]
}
```

---

### 5) Rentals – Lists & Detail

#### GET `/api/rentals?status=ACTIVE|OVERDUE|CLOSED`

**Response 200**

```json
[
  {
    "rentalId": 123,
    "roomNumber": "402",
    "bedNumber": "B",
    "startAt": "2025-12-05T10:30:00",
    "dueAt": "2025-12-06T12:00:00",
    "status": "ACTIVE",
    "bikesOut": 1,
    "bikesTotal": 1
  }
]
```

#### GET `/api/rentals/{rentalId}`

**Response 200**

```json
{
  "rentalId": 123,
  "roomNumber": "402",
  "bedNumber": "B",
  "startAt": "2025-12-05T10:30:00",
  "dueAt": "2025-12-06T12:00:00",
  "status": "ACTIVE",
  "items": [
    {
      "rentalItemId": 999,
      "bikeId": 5,
      "bikeNumber": "5",
      "status": "RENTED",
      "returnedAt": null,
      "lostReason": null
    }
  ]
}
```

---

### 6) Rentals – Returns

#### POST `/api/rentals/{rentalId}/items/{rentalItemId}/return`

**Response 200**

```json
{
  "rentalId": 123,
  "rentalItemId": 999,
  "bikeNumber": "5",
  "itemStatus": "RETURNED",
  "returnedAt": "2025-12-05T14:00:00",
  "rentalStatus": "CLOSED"
}
```

#### POST `/api/rentals/{rentalId}/return-selected`

**Request**

```json
{
  "rentalItemIds": [999, 1000]
}
```

**Response 200**

```json
{
  "rentalId": 123,
  "returnedItemIds": [999, 1000],
  "rentalStatus": "ACTIVE"
}
```

#### POST `/api/rentals/{rentalId}/return-all`

**Response 200**

```json
{
  "rentalId": 123,
  "rentalStatus": "CLOSED",
  "returnAt": "2025-12-05T14:10:00"
}
```

---

### 7) Rentals – Mark Lost

#### POST `/api/rentals/{rentalId}/items/{rentalItemId}/lost`

**Request**

```json
{
  "reason": "Key lost"
}
```

**Response 200**

```json
{
  "rentalId": 123,
  "rentalItemId": 999,
  "bikeNumber": "5",
  "itemStatus": "LOST",
  "lostReason": "Key lost",
  "rentalStatus": "ACTIVE"
}
```

---

### 8) Maintenance Export

#### GET `/api/maintenance/ooo/export`

Returns a CSV download of current OOO bikes.

**Columns**

* `bike_number`
* `bike_type`
* `ooo_note`
* `ooo_since_date`
