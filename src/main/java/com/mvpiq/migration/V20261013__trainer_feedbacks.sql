-- ============================================================
-- Create trainer_feedbacks table
-- ============================================================

-- Create trainer_feedbacks table to store feedback from trainers to players
CREATE TABLE IF NOT EXISTS public.trainer_feedbacks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trainer_id UUID NOT NULL,
    player_id UUID NOT NULL,
    feedback TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT trainer_feedbacks_trainer_id_fkey FOREIGN KEY (trainer_id) REFERENCES public.users(id) ON DELETE CASCADE,
    CONSTRAINT trainer_feedbacks_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.users(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_trainer_feedbacks_trainer_id ON public.trainer_feedbacks(trainer_id);
CREATE INDEX IF NOT EXISTS idx_trainer_feedbacks_player_id ON public.trainer_feedbacks(player_id);
CREATE INDEX IF NOT EXISTS idx_trainer_feedbacks_created_at ON public.trainer_feedbacks(created_at DESC);

COMMENT ON TABLE public.trainer_feedbacks IS 'Stores feedback provided by trainers to players';
COMMENT ON COLUMN public.trainer_feedbacks.id IS 'Unique identifier for the feedback';
COMMENT ON COLUMN public.trainer_feedbacks.trainer_id IS 'Reference to the user who provided the feedback (trainer)';
COMMENT ON COLUMN public.trainer_feedbacks.player_id IS 'Reference to the user receiving the feedback (player)';
COMMENT ON COLUMN public.trainer_feedbacks.feedback IS 'The actual feedback text content';
COMMENT ON COLUMN public.trainer_feedbacks.created_at IS 'Timestamp when the feedback was created';
