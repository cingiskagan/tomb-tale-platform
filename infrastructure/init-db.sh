#!/bin/bash
# ---------------------------------------------------------------------------
# init-db.sh — PostgreSQL entrypoint init script.
#
# Runs ONCE on first container start (when the data volume is empty).
# Creates dedicated users for Zitadel and each application microservice,
# enforcing the principle of least privilege.
#
# Environment variables (injected via docker-compose):
#   POSTGRES_ZITADEL_USER   / POSTGRES_ZITADEL_PASSWORD
#   POSTGRES_PLAYER_USER    / POSTGRES_PLAYER_PASSWORD
#   POSTGRES_COMMERCE_USER  / POSTGRES_COMMERCE_PASSWORD
#   POSTGRES_DB
# ---------------------------------------------------------------------------
set -euo pipefail

echo "🔧  init-db.sh: Creating dedicated PostgreSQL users..."

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Zitadel IAM user: needs CREATEDB for its own schema initialization
    CREATE USER ${POSTGRES_ZITADEL_USER}
        WITH PASSWORD '${POSTGRES_ZITADEL_PASSWORD}' CREATEDB;

    -- service-player dedicated user
    CREATE USER ${POSTGRES_PLAYER_USER}
        WITH PASSWORD '${POSTGRES_PLAYER_PASSWORD}';

    -- service-commerce dedicated user
    CREATE USER ${POSTGRES_COMMERCE_USER}
        WITH PASSWORD '${POSTGRES_COMMERCE_PASSWORD}';

    -- Grant both service users full DDL+DML on public schema
    -- (required for Hibernate ddl-auto: update)
    GRANT ALL PRIVILEGES ON SCHEMA public TO ${POSTGRES_PLAYER_USER};
    GRANT ALL PRIVILEGES ON SCHEMA public TO ${POSTGRES_COMMERCE_USER};

    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT ALL ON TABLES TO ${POSTGRES_PLAYER_USER};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT ALL ON TABLES TO ${POSTGRES_COMMERCE_USER};

    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT ALL ON SEQUENCES TO ${POSTGRES_PLAYER_USER};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public
        GRANT ALL ON SEQUENCES TO ${POSTGRES_COMMERCE_USER};

    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_PLAYER_USER};
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_COMMERCE_USER};
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_ZITADEL_USER};
EOSQL

echo "✅  init-db.sh: Users created successfully."
