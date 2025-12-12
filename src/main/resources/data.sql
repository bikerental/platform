-- Seed data for development/testing
-- Password is 'password123' hashed with bcrypt cost 10
-- Hash generated with: BCryptPasswordEncoder(10).encode("password123")

-- Insert test hotel (only if not exists)
INSERT INTO hotels (hotel_code, hotel_name, password_hash, created_at)
SELECT 'STEELHOUSE', 'SteelHouse Copenhagen', '$2a$10$iOURAF1RHhsMCdRr2U1n/.JTfyq4cyuokEyEn.E/YwIsWWRX8biAi', NOW()
WHERE NOT EXISTS (SELECT 1 FROM hotels WHERE hotel_code = 'STEELHOUSE');

-- Reset password for STEELHOUSE to 'password123' (ensures consistent dev experience)
-- This runs on every startup to ensure the test password works
UPDATE hotels 
SET password_hash = '$2a$10$iOURAF1RHhsMCdRr2U1n/.JTfyq4cyuokEyEn.E/YwIsWWRX8biAi'
WHERE hotel_code = 'STEELHOUSE';
