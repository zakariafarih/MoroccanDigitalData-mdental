-- Add gateway client for secure inter-service communication
INSERT INTO oauth_clients (
    id,
    client_id,
    client_name,
    client_secret,
    scope,
    created_at,
    created_by
) VALUES (
             'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
             'gateway-core',
             'API Gateway',
             'gateway-secret-value',
             'gateway',
             NOW(),
             'system'
         );