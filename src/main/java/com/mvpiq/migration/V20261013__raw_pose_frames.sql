-- ============================================================
-- Add raw_pose_frames column to shot_events table
-- ============================================================

-- Add raw_pose_frames column to store pose frame data for each shot
ALTER TABLE public.shot_events 
ADD COLUMN IF NOT EXISTS raw_pose_frames jsonb;

COMMENT ON COLUMN public.shot_events.raw_pose_frames IS 
'Array of raw pose frames captured during the shot, including keypoints and coordinate space data';
