-- Schema generated for AI Software Factory design stage
CREATE TABLE IF NOT EXISTS workflow_runs (
  id VARCHAR(64) PRIMARY KEY,
  requirement TEXT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS artifacts (
  id VARCHAR(64) PRIMARY KEY,
  workflow_id VARCHAR(64) NOT NULL,
  stage VARCHAR(64) NOT NULL,
  name VARCHAR(255) NOT NULL,
  path TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_artifacts_workflow_id ON artifacts(workflow_id);
