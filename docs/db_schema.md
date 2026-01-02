# DB Schema & Constraints – Bike Rental System (v1.0)

## Stack & Scope
- MySQL, Spring Data JPA, Java 21. Hotel scoping comes from JWT; every persisted record must carry or be resolvable to `hotel_id`.
- Core entities: Hotel, HotelSettings, Bike, Rental, RentalItem, Signature.
- **Note:** No RentalDraft entity. Bike assignment during rental creation is managed in frontend state; backend validates availability atomically at rental creation.

## Entities

### Hotel
- `hotel_id` (PK, bigint)
- `hotel_code` (string, unique) – used for login
- `hotel_name` (string)
- `password_hash` (string) – bcrypt with cost factor 10+
- `created_at` (timestamp)

### HotelSettings
- `settings_id` (PK, bigint)
- `hotel_id` (FK to Hotel, unique) – one settings record per hotel
- `rental_duration_options` (JSON array, e.g., `[24, 48, 72]`) – allowed rental durations in hours
- `grace_minutes` (int, default 0) – grace period before rental becomes overdue
- `tnc_text` (text) – Terms & Conditions content
- `tnc_version` (string) – auto-updated when T&C changes (e.g., timestamp or incrementing version)

> **MVP Note:** HotelSettings entity is created for future Settings UI. For MVP, service layer uses hardcoded defaults when settings are null or missing.

### Bike
- `bike_id` (PK, bigint)
- `hotel_id` (FK to hotel/tenant context)
- `bike_number` (string, unique per hotel)
- `bike_type` (enum/string, optional)
- `status` enum: `AVAILABLE`, `RENTED`, `OOO`
- `ooo_note` (text, optional), `ooo_since` (timestamp, optional)

### Rental
- `rental_id` (PK, bigint)
- `hotel_id`
- `status` enum: `ACTIVE`, `OVERDUE`, `CLOSED`
- `start_at` (timestamp), `due_at` (timestamp), `return_at` (timestamp, nullable)
- `room_number` (string), `bed_number` (string, nullable)
- `tnc_version` (string)
- `signature_id` (FK to Signature)

### RentalItem
- `rental_item_id` (PK, bigint)
- `rental_id` (FK to Rental, on delete restrict)
- `bike_id` (FK to Bike, on delete restrict)
- `status` enum: `RENTED`, `RETURNED`, `LOST`
- `returned_at` (timestamp, nullable)
- `lost_reason` (string/text, nullable)

### Signature
- `signature_id` (PK, bigint)
- `signature_data` (MEDIUMBLOB) – stores the PNG signature image directly in DB
- `created_at` (timestamp, UTC)
- Tied to the exact bike list and `tnc_version` used at signing; must be retrievable for staff to review post-creation and to generate a full contract document (PDF/HTML) with embedded signature.

## Relationships & Cardinality
- One Hotel → one HotelSettings (optional; MVP may have no record, use defaults).
- One Hotel → many Bikes, Rentals.
- One Rental → many RentalItems.
- One RentalItem → one Bike.
- A Bike can be part of many RentalItems over time but at most one with `status = RENTED` concurrently.
- Signature referenced by exactly one Rental.

## Constraints & Invariants
- I1: One open rental per bike – enforce in service layer and, if possible, with DB support (e.g., partial unique index on `(bike_id, status)` where status = RENTED; if partial indexes unavailable, guard via transactions/locks).
- I2: Bike status = OOO prevents assignment to rentals (reject at creation time).
- I3: Rental status derived from RentalItems + `due_at + grace` (ACTIVE/OVERDUE/CLOSED); ensure service recalculates on mutations.
- I4: Bike status mirrors participation in RENTED items; set to AVAILABLE when no RENTED items and not OOO; LOST handling keeps bike out of availability until addressed.
- I5: Signature captures final bike list and T&C version at signing; link via `signature_id`.
- Rental creation must be atomic: create Rental + RentalItems + signature storage + bike status updates in one transaction.
- Return/lost actions must recalc rental status and update `return_at` when all items are RETURNED/LOST.

## Indexing Guidelines
- Unique `(hotel_id, bike_number)`.
- Foreign key indexes: `rental_items.bike_id`, `rental_items.rental_id`, `rentals.hotel_id`.
- Query helpers: `rentals.status`, `rentals.due_at` (sorting active/overdue), `bikes.status`.
- Consider covering indexes for `/api/overview` counts (status + hotel_id) and active/overdue listings.

## Status & Time Behaviour
- `due_at` + `grace_minutes` (from HotelSettings, default 0) determines ACTIVE vs OVERDUE when any RENTED item exists.
- `return_at` set when all items returned/lost; remains null otherwise.
- `returned_at` per item set at return time; `lost_reason` optional when marking lost.

## OOO / Availability Rules
- Bikes marked OOO cannot be included in new rentals (rejected at creation time).
- Returning bikes sets status AVAILABLE unless OOO or other later rules block it.
- Marking lost should keep the bike out of availability (OOO or similar handling).
