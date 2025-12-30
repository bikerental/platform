-- V2: Seed data for development/testing
-- Password for STEELHOUSE is 'password123' (bcrypt, cost 10)
-- All inserts are idempotent (safe to run multiple times)

-- Insert test hotel only if it does not already exist
INSERT INTO hotels (hotel_code, hotel_name, password_hash, created_at)
SELECT
  'STEELHOUSE',
  'SteelHouse Copenhagen',
  '$2a$10$iOURAF1RHhsMCdRr2U1n/.JTfyq4cyuokEyEn.E/YwIsWWRX8biAi',
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM hotels WHERE hotel_code = 'STEELHOUSE'
);

-- Insert test bikes for STEELHOUSE hotel
INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT
  h.hotel_id, '1', 'ADULT', 'AVAILABLE', NULL, NULL
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B001');

INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT
  h.hotel_id, '2', 'ADULT', 'AVAILABLE', NULL, NULL
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B002');

INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT
  h.hotel_id, '3', 'ADULT', 'AVAILABLE', NULL, NULL
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B003');

INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT
  h.hotel_id, '4', 'KIDS', 'AVAILABLE', NULL, NULL
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B004');

INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT
  h.hotel_id, '5', 'ADULT', 'OOO', 'Flat tire - needs repair', NOW()
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B005');
