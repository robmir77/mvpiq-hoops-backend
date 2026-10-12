-- ============================================================
-- Add vision configuration fields to workout_sessions
-- ============================================================

-- Add vision configuration parameters to workout_sessions table
-- These fields store the camera and AI model configuration used during a session
-- All fields are nullable to support existing sessions without this data

ALTER TABLE public.workout_sessions 
ADD COLUMN IF NOT EXISTS fps INTEGER;

ALTER TABLE public.workout_sessions 
ADD COLUMN IF NOT EXISTS camera_resolution_width INTEGER;

ALTER TABLE public.workout_sessions 
ADD COLUMN IF NOT EXISTS camera_resolution_height INTEGER;

ALTER TABLE public.workout_sessions 
ADD COLUMN IF NOT EXISTS pose_resolution VARCHAR(50);

ALTER TABLE public.workout_sessions 
ADD COLUMN IF NOT EXISTS yolo_model_id VARCHAR(100);

ALTER TABLE public.workout_sessions 
ADD COLUMN IF NOT EXISTS move_net_model_id VARCHAR(100);

ALTER TABLE public.workout_sessions 
ADD COLUMN IF NOT EXISTS yolo_delegate VARCHAR(50);

ALTER TABLE public.workout_sessions 
ADD COLUMN IF NOT EXISTS pose_delegate VARCHAR(50);
