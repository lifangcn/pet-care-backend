#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

ROOT=${PETCARE_ROOT:-/opt/1panel/apps/petcare}
ENV_FILE=${ENV_FILE:-$ROOT/conf/petcare.env}
STAMP=$(date +%Y%m%d-%H%M%S)
DEST="$ROOT/backups/$STAMP"
TMP="$ROOT/backups/.partial-$STAMP"
mkdir -p "$TMP"
trap 'rm -rf "$TMP"' EXIT

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

docker exec -e PGPASSWORD="$POSTGRES_SUPERUSER_PASSWORD" petcare-postgres \
  pg_dump -U postgres -d "${POSTGRES_DB:-petcare}" -Fc > "$TMP/petcare.dump"
docker exec -e PGPASSWORD="$POSTGRES_SUPERUSER_PASSWORD" petcare-postgres \
  pg_dumpall -U postgres --roles-only > "$TMP/roles.sql"
docker exec -i petcare-postgres pg_restore --list < "$TMP/petcare.dump" >/dev/null
docker exec -e REDISCLI_AUTH="$REDIS_PASSWORD" petcare-redis \
  redis-cli --no-auth-warning --rdb /tmp/petcare-backup.rdb >/dev/null
docker cp petcare-redis:/tmp/petcare-backup.rdb "$TMP/redis.rdb"
docker exec petcare-redis rm -f /tmp/petcare-backup.rdb
cp "$ROOT/compose.yml" "$TMP/compose.yml"
sha256sum "$TMP"/* > "$TMP/SHA256SUMS"
mv "$TMP" "$DEST"
trap - EXIT
echo "backup created: $DEST"
