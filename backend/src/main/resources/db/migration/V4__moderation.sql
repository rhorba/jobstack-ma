-- Story 7.1: admin moderation queue
ALTER TABLE job_postings ADD COLUMN rejection_reason TEXT;

CREATE TABLE moderation_actions (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_posting_id UUID NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
  admin_user_id  UUID NOT NULL REFERENCES users(id),
  action         VARCHAR(20) NOT NULL CHECK (action IN ('APPROVE','REJECT','REMOVE')),
  reason         TEXT,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_moderation_actions_posting ON moderation_actions (job_posting_id);
