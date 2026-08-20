#!/bin/bash
# Creates the per-service databases in the shared Postgres instance.
# Runs automatically on first container startup via docker-entrypoint-initdb.d.
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE payments_db;
    CREATE DATABASE accounts_db;
    CREATE DATABASE notifications_db;
EOSQL
