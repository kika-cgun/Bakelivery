import { useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { OpsLayout } from '@/components/layout/OpsLayout';
import { useActiveDrivers } from '@ui/api/driver';
import { cn } from '@ui/lib/utils';
import type { DriverLocation } from '@ui/types';

// ─── Leaflet icon fix (Vite bundler) ─────────────────────────────────────────

delete (L.Icon.Default.prototype as unknown as Record<string, unknown>)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

// ─── Driver marker icons ──────────────────────────────────────────────────────

function makeDriverIcon(busy: boolean) {
  const bg = busy ? '#6366f1' : '#22c55e';
  return L.divIcon({
    className: '',
    html: `
      <div style="
        width:32px;height:32px;
        background:${bg};
        border-radius:50%;
        border:3px solid white;
        box-shadow:0 2px 8px rgba(0,0,0,.3);
        display:flex;align-items:center;justify-content:center;
      "><svg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='white' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'><circle cx='5.5' cy='17.5' r='3.5'/><circle cx='18.5' cy='17.5' r='3.5'/><circle cx='15' cy='5' r='1'/><path d='M12 17.5V14l-3-3 4-3 2 3h2'/></svg></div>`,
    iconSize:    [32, 32],
    iconAnchor:  [16, 16],
    popupAnchor: [0, -18],
  });
}

const WARSAW: [number, number] = [52.2297, 21.0122];

// ─── Filter helpers ──────────────────────────────────────────────────────────

type DriverFilter = 'all' | 'busy' | 'free';

function filterDrivers(drivers: DriverLocation[], filter: DriverFilter): DriverLocation[] {
  if (filter === 'busy') return drivers.filter((d) => !!d.orderId);
  if (filter === 'free') return drivers.filter((d) => !d.orderId);
  return drivers;
}

// ─── Driver list item ────────────────────────────────────────────────────────

interface DriverItemProps {
  driver: DriverLocation;
}

function DriverItem({ driver }: DriverItemProps) {
  const busy = !!driver.orderId;

  return (
    <div className="flex items-start gap-3 px-4 py-3 border-b border-slate-50 last:border-0 hover:bg-slate-50/50 transition-colors">
      <div
        className={cn(
          'mt-0.5 w-2 h-2 rounded-full shrink-0',
          busy ? 'bg-indigo-500' : 'bg-green-400',
        )}
        aria-label={busy ? 'W dostawie' : 'Wolny'}
      />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-slate-800 truncate">{driver.driverName}</p>
        {busy ? (
          <p className="text-xs text-indigo-500 font-mono truncate">W dostawie · #{driver.orderId?.slice(-4).toUpperCase()}</p>
        ) : (
          <p className="text-xs text-green-500">Wolny</p>
        )}
      </div>
      <span
        className={cn(
          'shrink-0 text-[10px] font-mono px-1.5 py-0.5 rounded-full border',
          busy
            ? 'bg-indigo-50 text-indigo-700 border-indigo-200'
            : 'bg-green-50 text-green-700 border-green-200',
        )}
      >
        {busy ? 'W dostawie' : 'Wolny'}
      </span>
    </div>
  );
}

// ─── Stats row ───────────────────────────────────────────────────────────────

interface StatsRowProps {
  drivers: DriverLocation[];
}

function StatsRow({ drivers }: StatsRowProps) {
  const totalActive = drivers.length;
  const inDelivery = drivers.filter((d) => !!d.orderId).length;
  const free = drivers.filter((d) => !d.orderId).length;

  return (
    <div className="grid grid-cols-3 gap-3 mb-5">
      <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm p-4">
        <p className="text-xs font-mono uppercase tracking-wide text-slate-400 mb-1">Aktywni kierowcy</p>
        <p className="font-mono text-2xl font-bold text-slate-800">{totalActive}</p>
      </div>
      <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm p-4">
        <p className="text-xs font-mono uppercase tracking-wide text-slate-400 mb-1">W dostawie</p>
        <p className="font-mono text-2xl font-bold text-indigo-600">{inDelivery}</p>
      </div>
      <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm p-4">
        <p className="text-xs font-mono uppercase tracking-wide text-slate-400 mb-1">Wolni</p>
        <p className="font-mono text-2xl font-bold text-green-600">{free}</p>
      </div>
    </div>
  );
}

// ─── Page ────────────────────────────────────────────────────────────────────

export default function MapPage() {
  const { data: drivers = [], isLoading } = useActiveDrivers();
  const [filter, setFilter] = useState<DriverFilter>('all');

  const filtered = filterDrivers(drivers, filter);

  const filterTabs: { id: DriverFilter; label: string }[] = [
    { id: 'all', label: 'Wszyscy' },
    { id: 'busy', label: 'W dostawie' },
    { id: 'free', label: 'Wolni' },
  ];

  return (
    <OpsLayout title="Mapa dostaw">
      <StatsRow drivers={drivers} />

      <div className="grid lg:grid-cols-[1fr_300px] gap-4">
        {/* Map panel */}
        <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm overflow-hidden min-h-[400px] flex flex-col">
          <div className="px-4 py-3 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700">Live mapa kurierów</h2>
            <p className="text-xs text-slate-400">Odświeżanie co 10 sekund</p>
          </div>
          <div className="flex-1 min-h-[360px]">
            <MapContainer
              center={WARSAW}
              zoom={12}
              scrollWheelZoom
              className="h-full w-full min-h-[360px]"
              attributionControl={false}
            >
              <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
              {drivers.map((driver) => (
                <Marker
                  key={driver.driverId}
                  position={[driver.lat, driver.lng]}
                  icon={makeDriverIcon(!!driver.orderId)}
                >
                  <Popup className="leaflet-popup-driver">
                    <div className="text-xs leading-snug min-w-[140px]">
                      <p className="font-semibold text-slate-800 mb-0.5">{driver.driverName}</p>
                      {driver.orderId ? (
                        <p className="text-indigo-600 font-mono">
                          #{driver.orderId.slice(-4).toUpperCase()} · W dostawie
                        </p>
                      ) : (
                        <p className="text-green-600">Wolny</p>
                      )}
                    </div>
                  </Popup>
                </Marker>
              ))}
            </MapContainer>
          </div>
        </div>

        {/* Drivers sidebar */}
        <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm flex flex-col overflow-hidden">
          <div className="px-4 py-3 border-b border-slate-100">
            <h2 className="text-sm font-semibold text-slate-700 mb-2">Kierowcy</h2>
            <div className="flex gap-1">
              {filterTabs.map((tab) => (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setFilter(tab.id)}
                  className={cn(
                    'flex-1 text-xs py-1 rounded-md transition-colors font-medium',
                    filter === tab.id
                      ? 'bg-slate-800 text-white'
                      : 'text-slate-500 hover:bg-slate-100',
                  )}
                >
                  {tab.label}
                </button>
              ))}
            </div>
          </div>

          <div className="flex-1 overflow-y-auto scrollbar-thin">
            {isLoading ? (
              <div className="p-4 space-y-3">
                {[1, 2, 3].map((n) => (
                  <div key={n} className="flex items-center gap-3 animate-pulse">
                    <div className="w-2 h-2 rounded-full bg-slate-100" />
                    <div className="flex-1 space-y-1">
                      <div className="h-3 w-24 bg-slate-100 rounded" />
                      <div className="h-2.5 w-16 bg-slate-100 rounded" />
                    </div>
                  </div>
                ))}
              </div>
            ) : filtered.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 text-slate-400">
                <p className="text-xs">Brak kierowców</p>
              </div>
            ) : (
              filtered.map((driver) => (
                <DriverItem key={driver.driverId} driver={driver} />
              ))
            )}
          </div>
        </div>
      </div>
    </OpsLayout>
  );
}
