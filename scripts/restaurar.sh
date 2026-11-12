#!/bin/sh
# Reemplaza la base del compose con un respaldo. Uso: scripts/restaurar.sh archivo.dump
# Antes de borrar nada comprueba que el respaldo exista, no esté vacío y se pueda leer.
set -eu
ARCHIVO="${1:?Indica el archivo de respaldo}"
if [ ! -s "$ARCHIVO" ]; then
  echo "No existe o está vacío: $ARCHIVO. No se tocó la base." >&2
  exit 1
fi
TABLAS=$(docker compose exec -T db pg_restore -l < "$ARCHIVO" | grep -c "TABLE DATA" || true)
if [ "$TABLAS" -eq 0 ]; then
  echo "El respaldo no se puede leer o no tiene datos: $ARCHIVO. No se tocó la base." >&2
  exit 1
fi
docker compose stop api
docker compose exec -T db sh -c 'dropdb -U "$POSTGRES_USER" --if-exists "$POSTGRES_DB" && createdb -U "$POSTGRES_USER" "$POSTGRES_DB"'
docker compose exec -T db sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner' < "$ARCHIVO"
docker compose start api
echo "Base restaurada desde $ARCHIVO ($TABLAS tablas con datos)"
