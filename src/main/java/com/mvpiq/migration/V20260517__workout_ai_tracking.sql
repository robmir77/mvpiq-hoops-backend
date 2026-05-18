-- ============================================================
-- Evoluzione Workout AI: nuove tabelle per tracking realtime
-- ============================================================

-- workout_frame_data: dati frame-by-frame dal tracking AI mobile
CREATE TABLE IF NOT EXISTS public.workout_frame_data (
    id                  uuid DEFAULT gen_random_uuid() NOT NULL,
    session_id          uuid NOT NULL,
    frame_timestamp     int8 NOT NULL,
    ball_x              float8 NULL,
    ball_y              float8 NULL,
    ball_confidence     float8 NULL,
    hoop_x              float8 NULL,
    hoop_y              float8 NULL,
    hoop_confidence     float8 NULL,
    pose_data           jsonb NULL,
    trajectory_data     jsonb NULL,
    ball_velocity_x     float8 NULL,
    ball_velocity_y     float8 NULL,
    shot_detected       bool DEFAULT false NOT NULL,
    created_at          timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT workout_frame_data_pkey PRIMARY KEY (id),
    CONSTRAINT fk_frame_data_session
        FOREIGN KEY (session_id) REFERENCES public.workout_sessions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_workout_frame_data_session
    ON public.workout_frame_data USING btree (session_id);
CREATE INDEX IF NOT EXISTS idx_workout_frame_data_timestamp
    ON public.workout_frame_data USING btree (session_id, frame_timestamp);
CREATE INDEX IF NOT EXISTS idx_workout_frame_data_shot_detected
    ON public.workout_frame_data USING btree (session_id, shot_detected)
    WHERE shot_detected = true;

COMMENT ON TABLE public.workout_frame_data IS
    'Dati tracking AI frame-by-frame: posizione palla, canestro, pose, velocità.';

-- pose_analysis: analisi biomeccanica del tiro
CREATE TABLE IF NOT EXISTS public.pose_analysis (
    id                      uuid DEFAULT gen_random_uuid() NOT NULL,
    shot_event_id           uuid NULL,
    elbow_angle             numeric(5,2) NULL,
    knee_angle              numeric(5,2) NULL,
    shoulder_angle          numeric(5,2) NULL,
    wrist_angle             numeric(5,2) NULL,
    release_height          numeric(10,2) NULL,
    release_angle           numeric(5,2) NULL,
    release_velocity        numeric(10,2) NULL,
    shot_smoothness         numeric(5,2) NULL,
    follow_through_score    numeric(5,2) NULL,
    balance_score           numeric(5,2) NULL,
    created_at              timestamptz DEFAULT now() NOT NULL,
    CONSTRAINT pose_analysis_pkey PRIMARY KEY (id),
    CONSTRAINT fk_pose_analysis_shot
        FOREIGN KEY (shot_event_id) REFERENCES public.shot_events(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pose_analysis_shot_event
    ON public.pose_analysis USING btree (shot_event_id);

COMMENT ON TABLE public.pose_analysis IS
    'Analisi biomeccanica del tiro: angoli articolari, altezza rilascio, fluidità.';
