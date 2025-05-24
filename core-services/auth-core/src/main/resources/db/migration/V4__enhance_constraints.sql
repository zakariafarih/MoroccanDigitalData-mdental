-- src/main/resources/db/migration/V4__enhance_constraints.sql
-- Add foreign key constraints
ALTER TABLE users
    ADD CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_refresh_token_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

ALTER TABLE verification_tokens
    ADD CONSTRAINT fk_verification_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE user_roles
    ADD CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE tenant_features
    ADD CONSTRAINT fk_tenant_feature_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- Add NOT NULL constraints to boolean columns
ALTER TABLE users
    ALTER COLUMN email_verified SET NOT NULL,
ALTER COLUMN locked SET NOT NULL;

ALTER TABLE refresh_tokens
    ALTER COLUMN revoked SET NOT NULL;

-- Add unique constraints for columns that should be unique within tenant
-- (These might already exist via the @Table annotations, but adding for clarity)
ALTER TABLE users
    ADD CONSTRAINT uq_users_tenant_username UNIQUE (tenant_id, username),
    ADD CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email);