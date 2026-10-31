#!/bin/sh
# Reemplaza la base del compose con un respaldo. Uso: scripts/restaurar.sh archivo.dump
set -eu
ARCHIVO="${1:?Indica el archivo de respaldo}"
docker compose stop api
docker compose exec -T db sh -c 'dropdb -U "$POSTGRES_USER" --if-exists "$POSTGRES_DB" && createdb -U "$POSTGRES_USER" "$POSTGRES_DB"'
docker compose exec -T db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner' < "$ARCHIVO"
docker compose start api
echo "Base restaurada desde $ARCHIVO"
