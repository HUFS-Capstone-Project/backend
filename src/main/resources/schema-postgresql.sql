DROP INDEX IF EXISTS idx_link_dispatch_attempts_recovery;

CREATE INDEX IF NOT EXISTS idx_link_dispatch_attempts_active_recovery
ON link_processing_dispatch_attempts (status, claimed_at, created_at, id)
WHERE active_slot = 1;
