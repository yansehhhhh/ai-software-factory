-- Migration generated for AI Software Factory design stage
-- Apply after reviewing target database dialect.
ALTER TABLE workflow_runs ADD COLUMN IF NOT EXISTS current_stage VARCHAR(128);
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS artifact_type VARCHAR(64);
