-- ============================================================
-- Change tracking_data column from jsonb to text
-- ============================================================

-- Alter the tracking_data column to be text instead of jsonb
ALTER TABLE public.shot_events 
ALTER COLUMN tracking_data TYPE text USING tracking_data::text;
