-- ============================================================
-- Change raw_pose_frames column from jsonb to text
-- ============================================================

-- Change column type from jsonb to text to work with Hibernate converter
ALTER TABLE public.shot_events 
ALTER COLUMN raw_pose_frames TYPE text USING raw_pose_frames::text;

COMMENT ON COLUMN public.shot_events.raw_pose_frames IS 
'Array of raw pose frames captured during the shot, including keypoints and coordinate space data (stored as JSON text)';
