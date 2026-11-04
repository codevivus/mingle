#!/usr/bin/env bash
# Dumps the whole database to backups/mingle-<timestamp>.sql.gz and keeps the newest KEEP files.
# Run it from the repository root, for example from cron:  0 3 * * *  cd /opt/mingle && ./scripts/backup.sh
set -euo pipefail

KEEP="${KEEP:-14}"
DIR="${BACKUP_DIR:-backups}"
mkdir -p "$DIR"

file="$DIR/mingle-$(date +%Y%m%d-%H%M%S).sql.gz"
docker compose exec -T postgres pg_dump -U "${DB_USERNAME:-mingle}" -d mingle --no-owner | gzip > "$file"

# An empty or truncated dump is worse than no dump: refuse to keep it.
if [ "$(gzip -dc "$file" | wc -c)" -lt 1000 ]; then
  rm -f "$file"
  echo "backup failed: the dump is empty" >&2
  exit 1
fi

# Delete the oldest files beyond KEEP.
ls -1t "$DIR"/mingle-*.sql.gz | tail -n +"$((KEEP + 1))" | xargs -r rm -f
echo "wrote $file"
