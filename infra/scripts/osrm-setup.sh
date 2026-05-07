#!/usr/bin/env bash
# OSRM data setup — Mazowsze (dev) lub Polska (prod)
# Uruchom raz przed pierwszym `docker compose up`
set -euo pipefail

OSRM_DATA_DIR="$(dirname "$0")/../osrm-data"
mkdir -p "$OSRM_DATA_DIR"
cd "$OSRM_DATA_DIR"

REGION="${OSRM_REGION:-mazowieckie}"
PBF_URL="${OSRM_PBF_URL:-https://download.geofabrik.de/europe/poland/${REGION}-latest.osm.pbf}"
PBF_FILE="${REGION}-latest.osm.pbf"
OSRM_BASE="${REGION}"

echo "==> Pobieranie danych OSM dla regionu: $REGION"
if [ ! -f "$PBF_FILE" ]; then
    wget -c "$PBF_URL" -O "$PBF_FILE"
else
    echo "    $PBF_FILE już istnieje, pomijam pobieranie."
fi

echo "==> Pre-processing OSRM (ekstrakcja profilu car)..."
docker run --rm -t \
    -v "$(pwd):/data" \
    ghcr.io/project-osrm/osrm-backend:v5.27 \
    osrm-extract -p /opt/car.lua /data/"$PBF_FILE"

echo "==> Partycjonowanie OSRM..."
docker run --rm -t \
    -v "$(pwd):/data" \
    ghcr.io/project-osrm/osrm-backend:v5.27 \
    osrm-partition /data/"$OSRM_BASE".osrm

echo "==> Dostosowywanie OSRM..."
docker run --rm -t \
    -v "$(pwd):/data" \
    ghcr.io/project-osrm/osrm-backend:v5.27 \
    osrm-customize /data/"$OSRM_BASE".osrm

echo ""
echo "GOTOWE. Dane OSRM w: $OSRM_DATA_DIR"
echo "Uruchom: docker compose up osrm"
echo ""
echo "Aby uruchomić OSRM z tymi danymi — zaktualizuj docker-compose.yml:"
echo "  command: osrm-routed --algorithm mld /data/${OSRM_BASE}.osrm"
