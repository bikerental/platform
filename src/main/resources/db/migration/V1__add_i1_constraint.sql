-- Invariant I1: One RENTED rental item per bike at a time
-- This migration adds a generated column and unique index to enforce the constraint.
--
-- How it works:
-- - Generated column 'rented_bike_id_if_rented' equals bike_id when status = 'RENTED', NULL otherwise
-- - MySQL unique indexes ignore NULL values, so multiple RETURNED/LOST items for same bike are allowed
-- - But only ONE RENTED item per bike can exist (unique constraint on non-NULL values)
--
-- Prerequisites:
-- - Run this AFTER the rental_items table is created by Hibernate (ddl-auto=update)
-- - The table must exist with columns: bike_id, status
--
-- Usage:
-- For MySQL 8.0+, run this SQL manually after first application startup:
--   mysql -u root -p bikerental_platform < src/main/resources/db/migration/V1__add_i1_constraint.sql
--
-- Note: This is designed for manual execution since the project uses ddl-auto=update.
-- For production, consider migrating to Flyway or Liquibase for automated migrations.

-- Only add the column if it doesn't exist (idempotent)
-- MySQL 8.0 doesn't have IF NOT EXISTS for columns, so we use a procedure
DELIMITER //

CREATE PROCEDURE add_i1_constraint_if_not_exists()
BEGIN
    -- Check if the column already exists
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
        AND TABLE_NAME = 'rental_items' 
        AND COLUMN_NAME = 'rented_bike_id_if_rented'
    ) THEN
        -- Add generated column
        ALTER TABLE rental_items
        ADD COLUMN rented_bike_id_if_rented BIGINT
        GENERATED ALWAYS AS (CASE WHEN status = 'RENTED' THEN bike_id ELSE NULL END) STORED;
        
        -- Add unique index on the generated column
        CREATE UNIQUE INDEX uk_one_rented_per_bike ON rental_items(rented_bike_id_if_rented);
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL add_i1_constraint_if_not_exists();

-- Clean up
DROP PROCEDURE IF EXISTS add_i1_constraint_if_not_exists;

