-- Story 7.4: track whether an expiry-soon notice has already been sent, to avoid re-notifying daily
ALTER TABLE job_postings ADD COLUMN expiry_notice_sent_at TIMESTAMPTZ;
