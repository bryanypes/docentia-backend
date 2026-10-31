#!/bin/sh
# Crea un respaldo comprimido de la base del compose. Uso: scripts/respaldar.sh [archivo]
set -eu
ARCHIVO="${1:-respaldo-docentia-$(date +%Y%m%d-%H%M%S).dump}"
docker compose exec -T db sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$ARCHIVO"
echo "Respaldo creado: $ARCHIVO"
