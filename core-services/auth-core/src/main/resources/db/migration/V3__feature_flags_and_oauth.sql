-- Add tenant features table
CREATE TABLE IF NOT EXISTS tenant_features (
                                               id UUID PRIMARY KEY,
                                               tenant_id UUID NOT NULL REFERENCES tenants(id),
    feature_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT false,
    config JSONB,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    version INT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, feature_name)
    );

-- Add verification tokens table
CREATE TABLE IF NOT EXISTS verification_tokens (
                                                   id UUID PRIMARY KEY,
                                                   user_id UUID NOT NULL REFERENCES users(id),
    token_hash CHAR(64) NOT NULL UNIQUE,
    token_type VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    version INT NOT NULL DEFAULT 0
    );

-- Add token previous hash column for rotation
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS previous_token_hash CHAR(64);