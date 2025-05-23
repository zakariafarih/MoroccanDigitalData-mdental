CREATE TABLE IF NOT EXISTS tenants (
                                       id            UUID PRIMARY KEY,
                                       slug          VARCHAR(100) NOT NULL UNIQUE,
    name          VARCHAR(150) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL,
    created_by    VARCHAR(100),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(100),
    deleted_at    TIMESTAMP,
    deleted_by    VARCHAR(100),
    version       INT NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS users (
                                     id             UUID PRIMARY KEY,
                                     tenant_id      UUID NOT NULL REFERENCES tenants(id),
    username       VARCHAR(50) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    locked         BOOLEAN NOT NULL DEFAULT false,
    last_login_at  TIMESTAMP,
    created_at     TIMESTAMP NOT NULL,
    created_by     VARCHAR(100),
    updated_at     TIMESTAMP,
    updated_by     VARCHAR(100),
    deleted_at     TIMESTAMP,
    deleted_by     VARCHAR(100),
    version        INT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, username),
    UNIQUE (tenant_id, email)
    );

CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
    );

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id            UUID PRIMARY KEY,
                                              user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id     UUID NOT NULL,
    token_hash    CHAR(64) NOT NULL,      -- SHA-256 hex
    expires_at    TIMESTAMP NOT NULL,
    revoked       BOOLEAN NOT NULL DEFAULT false,
    created_at    TIMESTAMP NOT NULL,
    created_by    VARCHAR(100),
    updated_at    TIMESTAMP,
    updated_by    VARCHAR(100),
    deleted_at    TIMESTAMP,
    deleted_by    VARCHAR(100),
    version       INT NOT NULL DEFAULT 0,
    UNIQUE (token_hash)
    );

CREATE TABLE IF NOT EXISTS failed_login_attempts (
                                                     id          UUID PRIMARY KEY,
                                                     username    VARCHAR(50) NOT NULL,
    tenant_id   UUID NOT NULL,
    ip_address  VARCHAR(45),
    attempted_at TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    created_by  VARCHAR(100),
    updated_at  TIMESTAMP,
    updated_by  VARCHAR(100),
    deleted_at  TIMESTAMP,
    deleted_by  VARCHAR(100),
    version     INT NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS outbox (
                                      id             UUID PRIMARY KEY,
                                      aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   UUID NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    retry_count    INT NOT NULL DEFAULT 0,
    dead_letter    BOOLEAN NOT NULL DEFAULT false,
    version        INT NOT NULL DEFAULT 0
    );

-- Create indices for better performance
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_email_tenant ON users(email, tenant_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_failed_attempts_username_tenant ON failed_login_attempts(username, tenant_id, attempted_at);
CREATE INDEX idx_outbox_aggregate ON outbox(aggregate_type, aggregate_id);
CREATE INDEX idx_outbox_created ON outbox(created_at) WHERE dead_letter = false;