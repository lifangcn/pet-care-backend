#!/usr/bin/env bash
set -Eeuo pipefail

ROOT=${PETCARE_ROOT:-/opt/1panel/apps/petcare}
ENV_FILE=${ENV_FILE:-$ROOT/conf/petcare.env}
COMPOSE_FILE=${COMPOSE_FILE:-$ROOT/compose.yml}

[[ $EUID -eq 0 ]] || { echo "run as root" >&2; exit 1; }
command -v docker >/dev/null
docker compose version >/dev/null
[[ -f "$ENV_FILE" ]] || { echo "missing $ENV_FILE" >&2; exit 1; }
[[ -f "$COMPOSE_FILE" ]] || { echo "missing $COMPOSE_FILE" >&2; exit 1; }
[[ $(stat -c '%a' "$ENV_FILE") == 600 ]] || { echo "$ENV_FILE must be mode 600" >&2; exit 1; }
if grep -Eq '(^|=)(REPLACE_ME|CHANGE_ME|your_)' "$ENV_FILE"; then
  echo "unresolved placeholder in $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a
required=(
  POSTGRES_SUPERUSER_PASSWORD PETCARE_MIGRATOR_PASSWORD PETCARE_CORE_DB_PASSWORD
  PETCARE_AI_DB_PASSWORD REDIS_PASSWORD JWT_SECRET_KEY SA_TOKEN_JWT_SECRET_KEY
  INTERNAL_SERVICE_TOKEN KAFKA_CLUSTER_ID DEEPSEEK_API_KEY DEEPSEEK_CHAT_MODEL
  ZHIPUAI_API_KEY OSS_ENDPOINT OSS_ACCESS_KEY_ID OSS_ACCESS_KEY_SECRET OSS_BUCKET_NAME
  AUTH_DEMO_ENABLED
)
for name in "${required[@]}"; do
  [[ -n ${!name:-} ]] || { echo "missing required variable: $name" >&2; exit 1; }
done
for name in POSTGRES_SUPERUSER_PASSWORD PETCARE_MIGRATOR_PASSWORD PETCARE_CORE_DB_PASSWORD \
            PETCARE_AI_DB_PASSWORD REDIS_PASSWORD JWT_SECRET_KEY SA_TOKEN_JWT_SECRET_KEY \
            INTERNAL_SERVICE_TOKEN; do
  value=${!name}
  [[ ${#value} -ge 24 ]] || { echo "$name must contain at least 24 characters" >&2; exit 1; }
done
[[ $AUTH_DEMO_ENABLED == true ]] || { echo "restricted demo requires AUTH_DEMO_ENABLED=true" >&2; exit 1; }

[[ -f "$ROOT/app/current/pet-care-core.jar" ]] || { echo "missing core jar" >&2; exit 1; }
[[ -f "$ROOT/app/current/pet-care-ai.jar" ]] || { echo "missing ai jar" >&2; exit 1; }
[[ -L "$ROOT/app/current" ]] || { echo "app/current must be a relative release symlink" >&2; exit 1; }
current_target=$(readlink "$ROOT/app/current")
[[ $current_target == releases/* ]] || { echo "app/current must target releases/<id> relatively" >&2; exit 1; }

install -d -m 0700 -o 999 -g 999 "$ROOT/data/postgres"
install -d -m 0750 -o 999 -g 999 "$ROOT/data/redis"
install -d -m 0750 -o 1000 -g 1000 "$ROOT/data/kafka"
install -d -m 0750 -o 10001 -g 10001 "$ROOT/logs/core" "$ROOT/logs/ai"

swapon --show --noheadings | grep -q . || { echo "swap is required on this host" >&2; exit 1; }
docker network inspect 1panel-network >/dev/null
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --quiet
sha256sum "$ROOT/app/current/"*.jar
echo "preflight passed"
