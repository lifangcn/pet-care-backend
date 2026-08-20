#!/usr/bin/env bash
set -Eeuo pipefail

ROOT=${PETCARE_ROOT:-/opt/1panel/apps/petcare}
ENV_FILE=${ENV_FILE:-$ROOT/conf/petcare.env}
COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$ROOT/compose.yml")

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

"$ROOT/scripts/preflight.sh"

"${COMPOSE[@]}" up -d postgres redis kafka
for _ in {1..36}; do
  if [[ $(docker inspect -f '{{.State.Health.Status}}' petcare-kafka 2>/dev/null || true) == healthy ]]; then
    break
  fi
  sleep 5
done
[[ $(docker inspect -f '{{.State.Health.Status}}' petcare-kafka) == healthy ]] || {
  docker logs --tail 200 petcare-kafka >&2
  exit 1
}
"${COMPOSE[@]}" run --rm kafka-init
"${COMPOSE[@]}" up -d --build petcare-core

echo "Waiting for Core and Flyway..."
for _ in {1..36}; do
  if [[ $(docker inspect -f '{{.State.Health.Status}}' petcare-core 2>/dev/null || true) == healthy ]]; then
    break
  fi
  sleep 5
done
[[ $(docker inspect -f '{{.State.Health.Status}}' petcare-core) == healthy ]] || {
  docker logs --tail 200 petcare-core >&2
  exit 1
}
flyway_ok=$(docker exec -e PGPASSWORD="$PETCARE_MIGRATOR_PASSWORD" petcare-postgres \
  psql -U petcare_migrator -d "${POSTGRES_DB:-petcare}" -tAc \
  "SELECT count(*) = 10 AND bool_and(success) FROM petcare.flyway_schema_history;")
[[ $flyway_ok == t ]] || {
  echo "Flyway history is incomplete or contains a failed migration" >&2
  exit 1
}

"${COMPOSE[@]}" up -d --build petcare-ai
for _ in {1..36}; do
  if [[ $(docker inspect -f '{{.State.Health.Status}}' petcare-ai 2>/dev/null || true) == healthy ]]; then
    break
  fi
  sleep 5
done
[[ $(docker inspect -f '{{.State.Health.Status}}' petcare-ai) == healthy ]] || {
  docker logs --tail 200 petcare-ai >&2
  exit 1
}

"${COMPOSE[@]}" ps
echo "application containers are healthy; enable the 1Panel site only after smoke tests"
