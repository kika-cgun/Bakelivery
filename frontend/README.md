# Bakelivery — Frontend

Dwie niezależne aplikacje Vite + React 19 + TypeScript + Tailwind CSS.

## Struktura

```
frontend/
├── shop/          # Sklep klienta (port 5173)
├── ops/           # Panel operacyjny — piekarz/kierowca/dyspozytor (port 5174)
└── packages/ui/   # Wspólne: typy, API hooki, utils, tokeny
```

## Start (dev)

```bash
# Sklep
cd shop && npm install && npm run dev

# Ops panel
cd ops && npm install && npm run dev
```

Backend (api-gateway) musi działać na `:8080`, realtime-service na `:8092`.

## Stack

- React 19 + Vite 6 + TypeScript 5.7 (strict)
- Tailwind CSS 3 · TanStack Query v5 · Zustand v5
- React Router v7 · Lucide React · React Leaflet

## Design

**Warm Minimal** — amber `#d97706`, background `#FFF7ED`, Calistoga + Inter + JetBrains Mono.

Szczegóły: `docs/superpowers/specs/2026-05-17-frontend-design.md`
