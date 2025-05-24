-- Platform tenant (root tenant)
INSERT INTO tenants (id, slug, name, active, created_at, created_by)
VALUES (
           'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
           'platform',
           'MDental Platform',
           true,
           NOW(),
           'system'
       );

-- Super admin user
INSERT INTO users (
    id, tenant_id, username, email, password_hash, first_name, last_name,
    email_verified, locked, created_at, created_by
)
VALUES (
           'b9eb4d45-d384-4e75-b491-38e1b3f7bfd2',
           'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
           'superadmin',
           'admin@mdental.org',
           '$2a$12$ZPxGJClY75hH55wgmFZk0OBNziKQ2oq.wj.ZARNCHCDpYWoXJkgp6', -- bcrypt for 'changeme123'
           'Super',
           'Admin',
           true,
           false,
           NOW(),
           'system'
       );

-- Super admin role
INSERT INTO user_roles (user_id, role)
VALUES ('b9eb4d45-d384-4e75-b491-38e1b3f7bfd2', 'SUPER_ADMIN');