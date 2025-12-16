-- Seed data for development/testing only
-- This file is loaded automatically by Spring Boot when spring.sql.init.mode=always
-- Password for STEELHOUSE is 'password123' (bcrypt, cost 10)
-- IMPORTANT: No credentials are reset on startup

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
-- Get hotel_id first, then insert bikes only if they don't exist
INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT 
  h.hotel_id,
  'B001',
  'ADULT',
  'AVAILABLE',
  NULL,
  NULL
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B001');

INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT 
  h.hotel_id,
  'B002',
  'ADULT',
  'AVAILABLE',
  NULL,
  NULL
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B002');

INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT 
  h.hotel_id,
  'B003',
  'ADULT',
  'AVAILABLE',
  NULL,
  NULL
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B003');

INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT 
  h.hotel_id,
  'B004',
  'KIDS',
  'AVAILABLE',
  NULL,
  NULL
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B004');

INSERT INTO bikes (hotel_id, bike_number, bike_type, status, ooo_note, ooo_since)
SELECT 
  h.hotel_id,
  'B005',
  'ADULT',
  'OOO',
  'Flat tire - needs repair',
  NOW()
FROM hotels h
WHERE h.hotel_code = 'STEELHOUSE'
  AND NOT EXISTS (SELECT 1 FROM bikes WHERE hotel_id = h.hotel_id AND bike_number = 'B005');

