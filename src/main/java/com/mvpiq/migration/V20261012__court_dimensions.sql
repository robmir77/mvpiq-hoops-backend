-- ============================================================
-- Add court width and height fields to court_calibrations
-- ============================================================

-- Add court dimensions to court_calibrations table
-- These fields store the real-world dimensions of the basketball court
-- All fields are nullable to support existing calibrations without this data

ALTER TABLE public.court_calibrations
ADD COLUMN IF NOT EXISTS court_width DOUBLE PRECISION;

ALTER TABLE public.court_calibrations
ADD COLUMN IF NOT EXISTS court_height DOUBLE PRECISION;
