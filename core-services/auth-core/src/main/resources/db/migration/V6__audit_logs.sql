-- Create audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id UUID PRIMARY KEY,
                                          tenant_id UUID NOT NULL,
                                          user_id UUID,
                                          event_type VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    request_id VARCHAR(100),
    details JSONB,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    version INT NOT NULL DEFAULT 0
    );

-- Add indexes for audit logs
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant ON audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);