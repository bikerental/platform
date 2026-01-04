-- V3: Add is_admin column to hotels table
-- Admin users can manage all hotels instead of just their own

ALTER TABLE hotels ADD COLUMN is_admin BOOLEAN NOT NULL DEFAULT FALSE;
