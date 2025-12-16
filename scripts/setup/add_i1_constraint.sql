-- =============================================================================
-- ONE-TIME SETUP SCRIPT: Invariant I1 Constraint
-- =============================================================================
-- 
-- PURPOSE:
-- Enforces Invariant I1: One RENTED rental item per bike at a time.
-- Uses a MySQL generated column + unique index approach.
--
-- HOW IT WORKS:
-- - Generated column 'rented_bike_id_if_rented' equals bike_id when status = 'RENTED', NULL otherwise
-- - MySQL unique indexes ignore NULL values, so multiple RETURNED/LOST items for same bike are allowed
-- - But only ONE RENTED item per bike can exist (unique constraint on non-NULL values)
--
-- PREREQUISITES:
-- - MySQL 8.0+ database
-- - The rental_items table must exist (created by Hibernate on first app startup)
-- - Run this AFTER the application has started at least once with ddl-auto=update
--
-- USAGE:
-- Run this script manually against your MySQL database:
--
--   mysql -u root -p bikerental_platform < scripts/setup/add_i1_constraint.sql
--
-- This is a ONE-TIME setup script. It is idempotent (safe to run multiple times).
--
-- NOTE ON MIGRATION STRATEGY:
-- This project uses Hibernate ddl-auto=update for schema management.
-- This script handles the generated column which Hibernate cannot create.
-- For production environments, consider migrating to Flyway/Liquibase with ddl-auto=validate.
-- =============================================================================

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
        
        SELECT 'I1 constraint added successfully.' AS result;
    ELSE
        SELECT 'I1 constraint already exists. No changes made.' AS result;
    END IF;
END //

DELIMITER ;

-- Execute the procedure
CALL add_i1_constraint_if_not_exists();

-- Clean up
DROP PROCEDURE IF EXISTS add_i1_constraint_if_not_exists;

