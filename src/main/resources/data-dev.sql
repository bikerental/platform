-- Seed data for development/testing only
-- This file is loaded ONLY under the 'dev' Spring profile
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
