CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(128) NOT NULL,
  dept VARCHAR(128) NOT NULL,
  roles JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE nodes (
  id VARCHAR(64) PRIMARY KEY,
  node_type VARCHAR(32) NOT NULL,
  node_name VARCHAR(255) NOT NULL,
  owner_user_id VARCHAR(64),
  owner_dept VARCHAR(128),
  parent_id VARCHAR(64),
  path TEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  as_of_date DATE,
  weights JSONB NOT NULL DEFAULT '{}'::jsonb,
  plan JSONB NOT NULL DEFAULT '{}'::jsonb,
  actual JSONB NOT NULL DEFAULT '{}'::jsonb,
  computed JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE compliance_items (
  id BIGSERIAL PRIMARY KEY,
  node_id VARCHAR(64) NOT NULL,
  doc_type VARCHAR(128) NOT NULL,
  is_key BOOLEAN NOT NULL DEFAULT FALSE,
  required_at DATE NOT NULL,
  submitted_at DATE,
  status VARCHAR(32) NOT NULL,
  overdue_days INT NOT NULL DEFAULT 0,
  attachment_url TEXT,
  meta JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE warnings (
  id BIGSERIAL PRIMARY KEY,
  node_id VARCHAR(64) NOT NULL,
  level VARCHAR(16) NOT NULL,
  status VARCHAR(32) NOT NULL,
  deviation DOUBLE PRECISION NOT NULL,
  reason TEXT NOT NULL,
  deviation_top_factors JSONB NOT NULL DEFAULT '[]'::jsonb,
  sla_due_at TIMESTAMP,
  triggered_at TIMESTAMP NOT NULL DEFAULT NOW(),
  resolved_at TIMESTAMP,
  assignees JSONB NOT NULL DEFAULT '[]'::jsonb,
  action_log JSONB NOT NULL DEFAULT '[]'::jsonb
);

CREATE TABLE audit_logs (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  action VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id VARCHAR(128) NOT NULL,
  before JSONB,
  after JSONB,
  diff_summary TEXT,
  ip VARCHAR(64),
  user_agent TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE user_node_acl (
  id BIGSERIAL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  node_id VARCHAR(64) NOT NULL,
  permission VARCHAR(16) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_nodes_parent_id ON nodes(parent_id);
CREATE INDEX idx_nodes_path ON nodes(path);
CREATE INDEX idx_warnings_status_level ON warnings(status, level);
CREATE INDEX idx_compliance_node_status ON compliance_items(node_id, status);
CREATE INDEX idx_audit_entity_created ON audit_logs(entity_id, created_at);
