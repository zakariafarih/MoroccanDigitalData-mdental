CREATE TABLE IF NOT EXISTS realms (
                                      id UUID PRIMARY KEY,
                                      name VARCHAR(100) NOT NULL UNIQUE,
    clinic_slug VARCHAR(100) NOT NULL,
    issuer VARCHAR(255) NOT NULL,
    admin_username VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    version INT NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS oauth_clients (
                                             id UUID PRIMARY KEY,
                                             client_id VARCHAR(100) NOT NULL UNIQUE,
    client_name VARCHAR(255) NOT NULL,
    client_secret VARCHAR(255) NOT NULL,
    scope VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    deleted_by VARCHAR(100),
    version INT NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS outbox (
                                      id UUID PRIMARY KEY,
                                      aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL,
    version INT NOT NULL DEFAULT 0
    );