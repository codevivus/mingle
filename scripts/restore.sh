#!/usr/bin/env bash
# Restores a dump made by backup.sh into an EMPTY database:  ./scripts/restore.sh backups/mingle-....sql.gz
# It refuses to run when tables already exist, so it can never overwrite live data. To restore over a
# live database, stop the application containers, drop and recreate the database yourself, then run this.
set -euo pipefail

file="${1:?usage: restore.sh <dump.sql.gz>}"
user="${DB_USERNAME:-mingle}"

tables="$(docker compose exec -T postgres psql -U "$user" -d mingle -tAc \
  "select count(*) from information_schema.tables where table_schema = 'public'")"
if [ "${tables//[[:space:]]/}" != "0" ]; then
  echo "refusing to restore: the database already has $tables tables" >&2
  exit 1
fi

gzip -dc "$file" | docker compose exec -T postgres psql -U "$user" -d mingle -v ON_ERROR_STOP=1 >/dev/null
echo "restored $file"
