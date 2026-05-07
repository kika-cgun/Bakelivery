# Bakelivery

## Faza 5 — pierwsze uruchomienie (OSRM)

1. Pobierz i przetworz dane OSM dla Mazowsza (wymaga ~2 GB wolnego miejsca, ~15 min):
   ```bash
   cd infra
   ./scripts/osrm-setup.sh
   ```

2. Uruchom wszystkie serwisy:
   ```bash
   docker compose up
   ```