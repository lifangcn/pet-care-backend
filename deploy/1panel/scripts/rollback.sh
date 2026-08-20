#!/usr/bin/env bash
set -Eeuo pipefail

ROOT=${PETCARE_ROOT:-/opt/1panel/apps/petcare}
TARGET=${1:?usage: rollback.sh RELEASE_ID}
RELEASE="$ROOT/app/releases/$TARGET"
[[ -f "$RELEASE/pet-care-core.jar" && -f "$RELEASE/pet-care-ai.jar" ]] || {
  echo "invalid release: $RELEASE" >&2
  exit 1
}

"$ROOT/scripts/backup.sh"
ln -sfn "releases/$TARGET" "$ROOT/app/current.next"
mv -Tf "$ROOT/app/current.next" "$ROOT/app/current"
docker compose --env-file "$ROOT/conf/petcare.env" -f "$ROOT/compose.yml" \
  up -d --build --no-deps petcare-core petcare-ai
echo "application release switched to $TARGET; database migrations were not rolled back"
