#!/bin/bash
# ---------------------------------------------------------------------------
# init-mongo.sh — MongoDB entrypoint init script.
#
# Runs ONCE on first container start (when the data volume is empty).
# Creates the audit database and its two users (ADR 0029):
#   - service-audit's user can insert into events and do nothing else.
#   - audit_reader can read the database and change nothing, for people who
#     browse the events, for example in MongoDB Compass.
#
# Environment variables (injected via docker-compose):
#   MONGO_INITDB_ROOT_USERNAME / MONGO_INITDB_ROOT_PASSWORD
#   MONGO_AUDIT_USER           / MONGO_AUDIT_PASSWORD
#   MONGO_AUDIT_READER_USER    / MONGO_AUDIT_READER_PASSWORD
# ---------------------------------------------------------------------------
set -euo pipefail

echo "🔧  init-mongo.sh: Creating the audit database and its users..."

# The audit credentials reach mongosh through its environment, not through
# the script text, so a quote in a password cannot break the JavaScript.
# The collection exists before the first insert, so the writer's role needs
# no right to create it.
mongosh --quiet --host 127.0.0.1 \
    --username "$MONGO_INITDB_ROOT_USERNAME" \
    --password "$MONGO_INITDB_ROOT_PASSWORD" \
    --authenticationDatabase admin \
    audit \
    --eval '
        db.createCollection("events");
        db.createRole({
            role: "eventWriter",
            privileges: [{ resource: { db: "audit", collection: "events" }, actions: ["insert"] }],
            roles: []
        });
        db.createUser({
            user: process.env.MONGO_AUDIT_USER,
            pwd: process.env.MONGO_AUDIT_PASSWORD,
            roles: ["eventWriter"]
        });
        db.createUser({
            user: process.env.MONGO_AUDIT_READER_USER,
            pwd: process.env.MONGO_AUDIT_READER_PASSWORD,
            roles: [{ role: "read", db: "audit" }]
        });
    '

echo "✅  init-mongo.sh: The audit database and its users exist."
