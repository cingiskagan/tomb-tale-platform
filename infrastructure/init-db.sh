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

    -- Each service owns its own schema and has no rights in the other's.
    -- Ownership covers CREATE/ALTER/DROP inside the schema, so no per-table
    -- grants or ALTER DEFAULT PRIVILEGES are needed.
    CREATE SCHEMA player   AUTHORIZATION ${POSTGRES_PLAYER_USER};
    CREATE SCHEMA commerce AUTHORIZATION ${POSTGRES_COMMERCE_USER};

    REVOKE ALL ON SCHEMA public FROM ${POSTGRES_PLAYER_USER};
    REVOKE ALL ON SCHEMA public FROM ${POSTGRES_COMMERCE_USER};

    -- Belt and braces for psql sessions and any unqualified SQL (data.sql).
    -- The authoritative setting is spring.jpa.properties.hibernate.default_schema
    -- in each service's application.yml, where a reviewer can see it.
    ALTER ROLE ${POSTGRES_PLAYER_USER}   SET search_path = player;
    ALTER ROLE ${POSTGRES_COMMERCE_USER} SET search_path = commerce;

    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_PLAYER_USER};
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_COMMERCE_USER};
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_ZITADEL_USER};
EOSQL

echo "✅  init-db.sh: Users created successfully."
