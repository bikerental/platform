# Implementation Plan – Bike Rental System (MVP)

> This checklist derives from the design docs (requirements.mdc, db_schema.mdc, api_endpoints.mdc).  
> Each item maps to one or more spec requirements. Prioritize top-to-bottom; core features first.

---

## Phase 1: Project Setup & Infrastructure

- [x] **1.1** Create monorepo structure: backend at root, `/frontend`, `/docs`
- [x] **1.2** Initialize Spring Boot backend (Java 21, Maven, Spring Web, Spring Security, Spring Data JPA, Lombok)
- [x] **1.3** Configure MySQL datasource and JPA properties in `application.properties`
- [x] **1.4** Initialize React frontend (Vite, Tailwind CSS, React Router)
- [x] **1.5** Configure frontend `API_BASE_URL` environment variable for backend calls
- [x] **1.6** Add JWT dependency (jjwt) to pom.xml
- [x] **1.7** Add validation starter to pom.xml
- [x] **1.8** Add H2 test dependency for integration tests
- [x] **1.9** Configure CORS for frontend origin (in SecurityConfig or WebMvcConfigurer)

---

## Phase 2: Authentication & Hotel Context

### Backend
- [x] **2.1** Create `Hotel` entity (hotel_id, hotel_code, hotel_name, password_hash)
- [x] **2.2** Create `HotelRepository`
- [x] **2.3** Implement JWT utility (generate token with hotelId/hotelCode/exp, validate, extract claims)
  - **Note:** Set JWT expiry to 8–12 hours (one staff shift)
- [x] **2.4** Implement `AuthService` (authenticate hotel credentials, return JWT + hotel name)
  - **Note:** Use bcrypt (cost factor 10+) for password verification
- [x] **2.5** Create `AuthController` with `POST /api/auth/login`
- [x] **2.6** Configure Spring Security: stateless session, JWT filter, permit `/api/auth/**`, protect all else
- [x] **2.7** Implement `HotelContext` helper to extract hotelId from SecurityContext for scoping queries

### Frontend
- [x] **2.8** Create `/login` route and LoginPage component (hotel code + password form)
- [x] **2.9** Implement auth service (call login API, store JWT in localStorage/memory)
- [x] **2.10** Create AuthContext/hook for current auth state and logout
- [x] **2.11** Implement ProtectedRoute wrapper (redirect to `/login` if unauthenticated)
- [x] **2.12** Add global Axios/fetch interceptor to attach `Authorization: Bearer` header
- [x] **2.13** Handle 401 responses globally (clear token, redirect to `/login`)
- [x] **2.14** Display logged-in hotel name in header/nav

---

## Phase 3: Bike Entity & Inventory

### Backend
- [x] **3.1** Create `Bike` entity (bike_id, hotel_id, bike_number, bike_type, status enum, ooo_note, ooo_since)
- [x] **3.2** Create `BikeRepository` with queries scoped by hotelId
- [x] **3.3** Implement `BikeService`:
  - [x] List bikes (filter by status, search by bike_number)
  - [x] Find bike by number within hotel
  - [x] Mark bike OOO (set status, note, ooo_since)
  - [x] Mark bike available (fail if currently in RENTED rental item)
- [x] **3.4** Create `BikeController`:
  - [x] `GET /api/bikes?status=&q=`
  - [x] `PATCH /api/bikes/{bikeId}/ooo`
  - [x] `PATCH /api/bikes/{bikeId}/available`
- [x] **3.5** Add DB seed script for initial hotel + bikes (dev/test data)

### Frontend
- [x] **3.6** Create `/bikes` route and BikesPage component
- [x] **3.7** Implement bike list with status filter and search input
- [x] **3.8** Add "Mark OOO" action with note input dialog
- [x] **3.9** Add "Mark Available" action (disabled if bike is rented)
- [x] **3.10** Display bike status badges (AVAILABLE, RENTED, OOO)

---

## Phase 4: Rental & RentalItem Entities

### Backend
- [x] **4.1** Create `HotelSettings` entity (settings_id, hotel_id FK unique, rental_duration_options JSON, grace_minutes, tnc_text, tnc_version)
  - **Note:** MVP uses hardcoded defaults; entity created for future Settings UI
- [x] **4.2** Create `HotelSettingsRepository`
- [x] **4.3** Implement `HotelSettingsService` with fallback to defaults when settings null/missing
- [x] **4.4** Create `Signature` entity (signature_id, hotel_id, storage_ref/blob, created_at)
  - **Note:** Added hotelId field for multi-tenant scoping
- [x] **4.5** Create `SignatureRepository`
  - **Note:** Includes hotel-scoped findBySignatureIdAndHotelId method
- [x] **4.6** Create `Rental` entity (rental_id, hotel_id, status enum, start_at, due_at, return_at, room_number, bed_number nullable, tnc_version, signature_id FK)
- [x] **4.7** Create `RentalRepository` with queries scoped by hotelId (find by status, find active/overdue)
- [x] **4.8** Create `RentalItem` entity (rental_item_id, rental_id FK, bike_id FK, status enum, returned_at, lost_reason)
- [x] **4.9** Create `RentalItemRepository`
  - **Note:** Bike-scoped queries include hotelId for multi-tenant safety
- [x] **4.10** Add unique constraint helper for invariant I1 (one RENTED item per bike at a time)
  - **Note:** SQL migration script in scripts/setup/add_i1_constraint.sql (manual execution)

---

## Phase 5: New Rental Flow (Staff Mode)

> **Note:** No backend draft persistence. Frontend manages bike assignment state locally. Backend validates bike availability at rental creation time.

### Backend
- [x] **5.1** Implement `GET /api/bikes/{bikeNumber}` to check bike availability by number (returns bike details + status)
  - **Note:** Already exists as `GET /api/bikes/by-number/{bikeNumber}` from Phase 3

### Frontend
- [x] **5.2** Create `/rentals/new` route and NewRentalPage (staff mode)
- [x] **5.3** Manage assigned bikes in React state (no backend call until finalization)
- [x] **5.4** Implement bike number input + "Add bike" button (validate via `GET /api/bikes/{bikeNumber}`)
- [x] **5.5** Display assigned bikes list with remove buttons
- [x] **5.6** Show validation errors (bike not found, not available, duplicate in local list)
- [x] **5.7** "Continue & hand to guest" button (disabled if no bikes); navigates to `/rentals/new/guest` with bike data in state/URL

---

## Phase 6: Guest Mode & Rental Creation

> **Note:** Single atomic `POST /api/rentals` creates the rental with all data. No draft endpoints.

### Backend
- [x] **6.1** Implement `SignatureService` (store base64 PNG as MEDIUMBLOB, return signature_id)
- [x] **6.2** Implement `RentalService.createRental()`:
- [x] Validate request has ≥1 bike number
- [x] Validate all bikes exist, belong to hotel, and are AVAILABLE (not RENTED/OOO)
- [x] Store signature
- [x] Compute start_at = now (UTC), due_at from returnDateTime
- [x] Create Rental (ACTIVE) with room/bed, tnc_version, signature_id
- [x] Create RentalItems (RENTED) for each bike
- [x] Update each bike status to RENTED
- [x] All in single transaction (atomic)
- [x] Return created rental
- [x] **6.3** Create endpoint `POST /api/rentals`
- Request: `{ bikeNumbers: string[], roomNumber, bedNumber?, returnDateTime, tncVersion, signatureBase64Png }`
- Response 201: rental with id, status, startAt, dueAt, items
- [x] **6.4** Handle creation errors (409 if any bike unavailable with specific bike numbers in error)

### Frontend
- [x] **6.5** Create `/rentals/new/guest` route and GuestModePage component
- [x] **6.6** Hide all staff navigation (header, sidebar) in guest mode
- [x] **6.7** Display assigned bikes (read-only list from state/URL params)
- [x] **6.8** Form inputs: return date picker, return time picker, room number (required), bed number (optional)
- [x] **6.9** Display T&C text (scrollable)
- [x] **6.10** Implement signature pad (canvas-based or library like react-signature-canvas)
- [x] **6.11** "Clear signature" button
- [x] **6.12** "Confirm rental" button with validation (future return time, room filled, signature present)
- [x] **6.13** On confirm: call `POST /api/rentals`; on success show "Rental created" confirmation screen
- [x] **6.14** Handle 409 error: show which bike(s) became unavailable, allow staff to reassign
- [x] **6.15** Confirmation screen has "Done" button to return to staff mode (navigate to `/rentals/:id` or `/`)

---

## Phase 7: Home Screen & Overview

### Backend
- [x] **7.1** Implement `OverviewService`:
- [x] Count bikes by status (AVAILABLE, RENTED, OOO) for hotel
- [x] Count rentals by status (ACTIVE, OVERDUE) for hotel
- [x] List active/overdue rentals with summary (id, room/bed, dueAt, status, bikesOut, bikesTotal)
- [x] **7.2** Create `OverviewController` with `GET /api/overview`

### Frontend
- [x] **7.3** Create `/` route and HomePage component
- [x] **7.4** Display summary counts in header/cards (bikes available/rented/OOO, rentals active/overdue)
- [x] **7.5** Display active rentals list (overdue highlighted, sorted overdue-first then by dueAt)
- [x] **7.6** Each rental row: id, room/bed, due datetime, status badge, bikes out/total
- [x] **7.7** Rows clickable → navigate to `/rentals/:id`
- [x] **7.8** Implement search/filter (at least one of: room number, bike number, rental id)
- [x] **7.9** Add "New rental" button → navigate to `/rentals/new`

---

## Phase 8: Rental Detail & Contract View

### Backend
- [x] **8.1** Implement `RentalService.getRentalDetail()`:
- [x] Return rental with all items (bike number, type, item status, returned_at, lost_reason)
- [x] Include signature reference/URL
- [x] **8.2** Create `GET /api/rentals/{rentalId}` endpoint
- [x] **8.3** Implement `GET /api/rentals/{rentalId}/contract`:
- [x] Generate contract document (PDF or HTML) with bike list, room/bed, times, T&C version, embedded signature
- [x] **8.4** Implement `GET /api/rentals/{rentalId}/signature` (return signature image)

### Frontend
- [x] **8.5** Create `/rentals/:id` route and RentalDetailPage component
- [x] **8.6** Display rental info: id, room/bed, start_at, due_at, status badge
- [x] **8.7** Display bikes list with per-item status badge
- [x] **8.8** For RENTED items: show "Return bike" and "Mark lost" buttons
- [x] **8.9** For RETURNED items: show returned_at timestamp
- [x] **8.10** For LOST items: show lost indicator + reason
- [x] **8.11** "View contract" button → open contract PDF/HTML (new tab or modal)
- [x] **8.12** Signature preview/thumbnail in detail view

---

## Phase 9: Returns (Single, Multi, All)

### Backend
- [x] **9.1** Implement `RentalService.returnBike(rentalId, rentalItemId)`:
- [x] Set item status RETURNED, returned_at = now
- [x] Set bike status AVAILABLE (unless OOO)
- [x] Recalc rental status (CLOSED if all returned/lost)
- [x] Set rental.return_at if CLOSED
- [x] **9.2** Create `POST /api/rentals/{rentalId}/items/{rentalItemId}/return`
- [x] **9.3** Implement `RentalService.returnSelected(rentalId, rentalItemIds[])`:
- [x] Apply return logic to each item
- [x] Recalc rental status after batch
- [x] **9.4** Create `POST /api/rentals/{rentalId}/return-selected`
- [x] **9.5** Implement `RentalService.returnAll(rentalId)`:
- [x] Return all RENTED items
- [x] Set rental CLOSED, return_at = now
- [x] **9.6** Create `POST /api/rentals/{rentalId}/return-all`

### Frontend
- [x] **9.7** Single-bike return: confirmation dialog → call API → update UI → show toast with Undo
- [x] **9.8** Implement undo for single-bike return (30 second window)
- [x] **9.9** Multi-select: checkboxes on RENTED items; "Return selected" button (enabled when ≥1 selected)
- [x] **9.10** Multi-select confirmation dialog listing bike numbers → call API → update UI → show summary toast
- [x] **9.11** "Return all remaining bikes" button (visible when rental not CLOSED and ≥1 RENTED)
- [x] **9.12** Return-all confirmation dialog listing remaining bikes → call API → update UI → show confirmation message
- [x] **9.13** Hide return/lost actions when rental is CLOSED

---

## Phase 10: Mark Lost

### Backend
- [x] **10.1** Implement `RentalService.markLost(rentalId, rentalItemId, reason)`:
- [x] Set item status LOST, store lost_reason
- [x] Set bike status OOO (or similar to keep out of availability)
- [x] Recalc rental status
- [x] **10.2** Create `POST /api/rentals/{rentalId}/items/{rentalItemId}/lost`

### Frontend
- [x] **10.3** "Mark as lost" button on RENTED items
- [x] **10.4** Confirmation dialog with optional reason text input
- [x] **10.5** On confirm: call API → update UI → show notification
- [x] **10.6** Lost items display indicator + reason in rental detail

---

## Phase 11: Overdue Handling

### Backend
- [x] **11.1** Implement rental status derivation logic:
- [x] If all items RETURNED/LOST → CLOSED
- [x] Else if any RENTED and now > due_at + grace → OVERDUE
- [x] Else → ACTIVE
- [x] **11.2** Ensure status recalculated on every return/lost action
- [x] **11.3** Overview endpoint returns correct overdue counts and flags

### Frontend
- [x] **11.4** Overdue rentals highlighted in home list (e.g., red badge, sorted first)
- [x] **11.5** Rental detail shows overdue status badge when applicable
- [x] **11.6** Overdue does NOT disable return/lost actions (informational only)

---

## Phase 12: Maintenance Export

### Backend
- [x] **12.1** Implement `GET /api/maintenance/ooo/export`:
- [x] Query OOO bikes for hotel (scoped by hotelId from JWT)
- [x] Return Excel (.xlsx) with columns: Bike Number, Bike Type, OOO Note, OOO Since
- [x] Sort by ooo_since ASC (oldest first), nulls last, then bike_number ASC
- [x] Uses Apache POI for Excel generation
- [x] Headers: Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
- [x] Integration tests: ordering, hotel scoping, header-only for empty results

### Frontend
- [x] **12.2** `/bikes?status=OOO` shows OOO bikes sorted oldest first (matches export)
- [x] **12.3** "Export OOO to Excel" button visible when filtering by OOO status
- [x] Button has loading state and error toast on failure

---

## Phase 13: Rentals List (History)

### Backend
- [ ] **13.1** Implement `GET /api/rentals?status=ACTIVE|OVERDUE|CLOSED`:
- [ ] Filter by status (can be combined or individual)
- [ ] Return list with summary fields

### Frontend
- [ ] **13.2** Optionally add rentals history view or filter on home (CLOSED rentals)
- [ ] **13.3** Allow searching/filtering past rentals by room, bike, or rental id

---

## Phase 14: Polish & Edge Cases

- [ ] **14.1** Add loading states and spinners throughout frontend
- [ ] **14.2** Add error handling and user-friendly error messages
- [ ] **14.3** Ensure all confirmation dialogs are clear about affected bikes/rentals
- [ ] **14.4** Test race condition handling (bike becomes unavailable during finalization)
- [ ] **14.5** Ensure atomic transactions in all multi-step backend operations
- [ ] **14.6** Responsive layout testing (iPad primary, desktop, mobile)
- [ ] **14.7** Large touch targets for iPad use
- [ ] **14.8** Add logout functionality (clear JWT, redirect to login)

---

## Phase 15: Testing

- [ ] **15.1** Unit tests: `RentalService` – create rental, partial returns, return all, mark lost
- [x] **15.2** Unit tests: `BikeService` – OOO/available transitions, availability checks
- [ ] **15.3** Unit tests: Invariant I1 – prevent double-renting a bike
- [ ] **15.4** Unit tests: Invariant I2 – OOO bikes cannot be assigned
- [ ] **15.5** Unit tests: Rental status derivation logic
- [ ] **15.6** Integration tests: Create rental → return flow (bike availability validation)
- [ ] **15.7** Integration tests: Authentication and hotel scoping
- [ ] **15.8** Frontend component tests (optional for MVP)

---

## Admin Feature (Added)

> Hotel management via admin API. See `docs/admin_api.md` for full documentation.

- [x] Admin authentication (via config-based credentials)
- [x] Role-based access control (ROLE_ADMIN vs ROLE_HOTEL)
- [x] `GET /api/admin/hotels` – list all hotels
- [x] `POST /api/admin/hotels` – create new hotel
- [x] `POST /api/admin/hotels/{hotelId}/reset-password` – reset hotel password

---

## Future / Out of Scope (Post-MVP)

### Settings & Configuration (Architecture Ready)
- [ ] `GET /api/settings` – return hotel settings (durations, grace, T&C)
- [ ] `PATCH /api/settings` – update hotel settings
- [ ] `/settings` route – UI for configuring rental durations, grace period, T&C editor

### Bike Management (via SQL/Admin API)
- [ ] `POST /api/admin/bikes` – create new bike (admin-only, hotel-scoped)
- [ ] `DELETE /api/admin/bikes/{bikeId}` – soft-delete/archive bike (fail if rental history)
- [ ] Bike creation UI in admin panel (future)

### Other Enhancements
- [ ] Refresh token / token renewal
- [ ] Individual staff accounts for audit trails
- [ ] Admin UI (currently API-only)
- [ ] GDPR data retention and purging
- [ ] Analytics / overdue duration logging / bike maintenance logging
- [ ] Multi-language support
- [ ] Email/SMS notifications

---

## Progress Tracking

| Phase | Status | Notes |
|-------|--------|-------|
| 1. Setup | Complete | Backend at root, frontend in /frontend. JWT/validation/H2 deps added. CORS configured. |
| 2. Auth | Complete | Hotel entity, JWT service, Spring Security, login endpoint, protected routes, AuthContext. |
| 3. Bikes | Complete | Bike entity, repository, service, controller. OOO/AVAILABLE transitions with business rules. Unit tests + integration test for unique constraint. |
| 4. Rental Entities | Complete | HotelSettings, Signature, Rental, RentalItem entities with repos. HotelSettingsService with defaults. I1 constraint SQL migration script. All hotel-scoped for multi-tenant safety. |
| 5. New Rental Flow | Complete | Backend endpoint already existed. Frontend: NewRentalPage with bike assignment, validation, error handling. |
| 6. Guest/Create | Complete | Backend: SignatureService, RentalService.createRental, POST /api/rentals with 409 handling. Frontend: GuestModePage with signature pad, T&C, form validation, success/error screens. |
| 7. Home Overview | Complete | Backend: OverviewService with bike/rental counts and active rentals list, OverviewController with GET /api/overview. Frontend: HomePage with stat cards, active rentals table with search/filter, clickable rows, "New rental" button. Auto-refresh every 30s. |
| 8. Rental Detail | Complete | Backend: RentalService.getRentalDetail, RentalContractService for HTML contract & signature, GET /api/rentals/{id}, /contract, /signature endpoints. Frontend: RentalDetailPage with info cards, bikes list, signature preview, contract viewer. |
| 9. Returns | Complete | Backend: returnBike, returnSelected, returnAll methods with rental status recalculation. Undo return support. Frontend: RentalBikesList with multi-select, confirmation dialogs, 30s undo window, toast notifications. Actions hidden when rental is CLOSED. |
| 10. Mark Lost | Not started | |
| 11. Overdue | Not started | |
| 12. Export | Complete | Backend: MaintenanceController with Excel export (.xlsx), proper formatting, sorting. Frontend: Export button on OOO view with loading/error states. Integration tests cover ordering, hotel scoping, edge cases. |
| 13. Rentals List | Not started | |
| 14. Polish | Not started | |
| 15. Testing | Not started | |

---

*Tip: Treat each numbered item as a potential commit. Check off items as you complete them. Update the Progress Tracking table after completing each phase.*

