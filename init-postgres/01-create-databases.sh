#!/bin/bash
# Creates the per-service databases in the shared Postgres instance and
# enables pg_stat_statements in every app database (slow-query metrics).
# Runs automatically on first container startup via docker-entrypoint-initdb.d
# (only when the data volume is empty).
set -euo pipefail

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE payments_db;
    CREATE DATABASE accounts_db;
    CREATE DATABASE notifications_db;
EOSQL

for db in "$POSTGRES_DB" payments_db accounts_db notifications_db; do
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" \
        -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"
done
