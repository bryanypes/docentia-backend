#!/bin/sh
# Crea un respaldo comprimido de la base del compose y lo verifica. Uso: scripts/respaldar.sh [archivo]
# Falla (código distinto de 0) si el archivo queda vacío o si pg_restore no puede leer su índice.
set -eu
ARCHIVO="${1:-respaldo-docentia-$(date +%Y%m%d-%H%M%S).dump}"
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$ARCHIVO"
if [ ! -s "$ARCHIVO" ]; then
  echo "El respaldo quedó vacío: $ARCHIVO" >&2
  exit 1
fi
TABLAS=$(docker compose exec -T db pg_restore -l < "$ARCHIVO" | grep -c "TABLE DATA" || true)
if [ "$TABLAS" -eq 0 ]; then
  echo "El respaldo no se puede leer o no tiene datos: $ARCHIVO" >&2
  exit 1
fi
echo "Respaldo creado y verificado: $ARCHIVO ($(wc -c < "$ARCHIVO") bytes, $TABLAS tablas con datos)"
