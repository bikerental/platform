-- H2-specific schema additions for testing
-- This file adds the I1 constraint (generated column + unique index) for H2 tests
-- H2 syntax differs from MySQL for generated columns

-- Note: This schema is applied AFTER Hibernate creates the base tables.
-- The generated column is added separately since H2's ALTER TABLE syntax
-- for generated columns differs from MySQL.

-- For H2, we use COMPUTED columns which are H2's equivalent of generated columns.
-- However, H2 doesn't support adding computed columns via ALTER TABLE easily.
-- 
-- Alternative approach for H2 tests:
-- 1. Tests can rely on the service-level pre-check (existsByBikeIdAndStatus)
-- 2. For explicit I1 testing, we verify via the repository query
-- 
-- The MySQL-specific generated column won't exist in H2, but the 
-- RentalItem entity has it as nullable/read-only, so it won't cause issues.

