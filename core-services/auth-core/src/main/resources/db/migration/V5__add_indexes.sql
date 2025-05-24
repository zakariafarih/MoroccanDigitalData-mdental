-- Add missing indexes for better performance
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires_at ON refresh_tokens (expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_tenant_username ON users (tenant_id, username);
CREATE UNIQUE INDEX IF NOT EXISTS ux_user_tenant_email ON users (tenant_id, email);