# postgres-init/01-create-databases.sh
#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE keycloak;
    CREATE DATABASE mdental_auth;
    CREATE DATABASE mdental_clinic;

    GRANT ALL PRIVILEGES ON DATABASE keycloak TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE mdental_auth TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE mdental_clinic TO postgres;
EOSQL