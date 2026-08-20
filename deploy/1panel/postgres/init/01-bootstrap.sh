#!/usr/bin/env bash
set -Eeuo pipefail

: "${PETCARE_MIGRATOR_PASSWORD:?missing PETCARE_MIGRATOR_PASSWORD}"
: "${PETCARE_CORE_DB_PASSWORD:?missing PETCARE_CORE_DB_PASSWORD}"
: "${PETCARE_AI_DB_PASSWORD:?missing PETCARE_AI_DB_PASSWORD}"

psql --set=ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=migrator_password="$PETCARE_MIGRATOR_PASSWORD" \
  --set=core_password="$PETCARE_CORE_DB_PASSWORD" \
  --set=ai_password="$PETCARE_AI_DB_PASSWORD" <<'SQL'
SELECT 'CREATE ROLE petcare_app NOLOGIN'
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'petcare_app') \gexec

SELECT format('CREATE ROLE petcare_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L', :'migrator_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'petcare_migrator') \gexec
SELECT format('ALTER ROLE petcare_migrator PASSWORD %L', :'migrator_password') \gexec

SELECT format('CREATE ROLE petcare_core LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L', :'core_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'petcare_core') \gexec
SELECT format('ALTER ROLE petcare_core PASSWORD %L', :'core_password') \gexec

SELECT format('CREATE ROLE petcare_ai LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD %L', :'ai_password')
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'petcare_ai') \gexec
SELECT format('ALTER ROLE petcare_ai PASSWORD %L', :'ai_password') \gexec

GRANT petcare_app TO petcare_core, petcare_ai;
SELECT format('REVOKE CREATE ON DATABASE %I FROM PUBLIC', current_database()) \gexec
SELECT format('GRANT CREATE ON DATABASE %I TO petcare_migrator', current_database()) \gexec
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

CREATE SCHEMA IF NOT EXISTS petcare AUTHORIZATION petcare_migrator;
ALTER SCHEMA petcare OWNER TO petcare_migrator;
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA petcare;
CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA petcare;
SQL
