import { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, MapPin, Clock, CheckCircle2, Loader2, Bike } from 'lucide-react';
import { MapContainer, TileLayer, Marker, useMap } from 'react-leaflet';
import L from 'leaflet';
import { useOrder } from '@ui/api/orders';
import { cn, formatPrice, formatDate, orderStatusLabel, orderStatusClass } from '@ui/lib/utils';
import type { OrderStatus, DeliveryAddress } from '@ui/types';

// ─── Leaflet icon fix (Vite bundler) ─────────────────────────────────────────

delete (L.Icon.Default.prototype as unknown as Record<string, unknown>)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

const destinationIcon = L.divIcon({
  className: '',
  html: `<div style="width:20px;height:20px;background:#b45309;border-radius:50% 50% 50% 0;transform:rotate(-45deg);border:3px solid white;box-shadow:0 2px 8px rgba(0,0,0,.35)"></div>`,
  iconSize:    [20, 20],
  iconAnchor:  [10, 20],
});

// ─── Geocoding ───────────────────────────────────────────────────────────────

const WARSAW: [number, number] = [52.2297, 21.0122];

async function geocodeAddress(address: DeliveryAddress): Promise<[number, number] | null> {
  const query = encodeURIComponent(
    `${address.street}, ${address.postalCode} ${address.city}, Poland`,
  );
  try {
    const res = await fetch(
      `https://nominatim.openstreetmap.org/search?q=${query}&format=json&limit=1`,
      { headers: { 'Accept-Language': 'pl' } },
    );
    const data = await res.json() as Array<{ lat: string; lon: string }>;
    if (data.length > 0) return [parseFloat(data[0].lat), parseFloat(data[0].lon)];
  } catch { /* fall through */ }
  return null;
}

// ─── Map recenter helper ──────────────────────────────────────────────────────

function RecenterMap({ center }: { center: [number, number] }) {
  const map = useMap();
  useEffect(() => { map.setView(center, 15); }, [map, center]);
  return null;
}

// ─── Delivery Map ─────────────────────────────────────────────────────────────

interface DeliveryMapProps {
  address: DeliveryAddress;
}

function DeliveryMap({ address }: DeliveryMapProps) {
  const [coords, setCoords] = useState<[number, number] | null>(null);
  const [geocoding, setGeocoding] = useState(true);

  useEffect(() => {
    let cancelled = false;
    setGeocoding(true);
    geocodeAddress(address).then((c) => {
      if (!cancelled) { setCoords(c); setGeocoding(false); }
    });
    return () => { cancelled = true; };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [address.street, address.postalCode, address.city]);

  const center: [number, number] = coords ?? WARSAW;

  return (
    <div className="relative h-44">
      {geocoding && (
        <div className="absolute inset-0 z-[500] flex items-center justify-center bg-white/60">
          <Loader2 size={20} className="animate-spin text-amber-500" />
        </div>
      )}
      <MapContainer
        center={center}
        zoom={coords ? 15 : 12}
        scrollWheelZoom={false}
        className="h-full w-full"
        attributionControl={false}
        zoomControl={false}
      >
        <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
        {coords && (
          <>
            <RecenterMap center={coords} />
            <Marker position={coords} icon={destinationIcon} />
          </>
        )}
      </MapContainer>
    </div>
  );
}

// ─── Progress Steps ───────────────────────────────────────────────────────────

type Step = {
  status: OrderStatus;
  label: string;
};

const STEPS: Step[] = [
  { status: 'PENDING', label: 'Złożone' },
  { status: 'ACCEPTED', label: 'Przyjęte' },
  { status: 'IN_DELIVERY', label: 'W drodze' },
  { status: 'DELIVERED', label: 'Dostarczone' },
];


function getStepIndex(status: OrderStatus): number {
  // Map all statuses to the 4-step UI
  if (status === 'PENDING') return 0;
  if (status === 'ACCEPTED' || status === 'BAKING') return 1;
  if (status === 'READY_FOR_PICKUP' || status === 'IN_DELIVERY') return 2;
  if (status === 'DELIVERED') return 3;
  return 0;
}

interface ProgressStepsProps {
  status: OrderStatus;
}

function ProgressSteps({ status }: ProgressStepsProps) {
  const activeIndex = getStepIndex(status);
  const isTerminal = status === 'REJECTED' || status === 'CANCELLED';

  if (isTerminal) {
    return (
      <div className="flex items-center gap-2 py-3 px-4 bg-red-50 rounded-[14px] border border-red-200">
        <span className="text-red-600 font-medium text-sm">
          Zamówienie {status === 'REJECTED' ? 'odrzucone' : 'anulowane'}
        </span>
      </div>
    );
  }

  return (
    <div className="relative">
      {/* Line */}
      <div className="absolute top-4 left-4 right-4 h-0.5 bg-[#FCEAE1]" />
      <div
        className="absolute top-4 left-4 h-0.5 bg-amber-500 transition-all duration-500"
        style={{ width: `${(activeIndex / (STEPS.length - 1)) * (100 - 8 / STEPS.length)}%` }}
      />

      <div className="relative flex justify-between">
        {STEPS.map((step, i) => {
          const done = i <= activeIndex;
          const active = i === activeIndex;
          return (
            <div key={step.status} className="flex flex-col items-center gap-1.5 flex-1">
              <div
                className={cn(
                  'w-8 h-8 rounded-full flex items-center justify-center transition-all duration-300 z-10',
                  done
                    ? 'bg-amber-600 text-white shadow-primary'
                    : 'bg-white border-2 border-[#FCEAE1] text-slate-300',
                  active && 'ring-4 ring-amber-100',
                )}
              >
                {done ? <CheckCircle2 size={16} strokeWidth={2.5} /> : <span className="text-xs font-mono">{i + 1}</span>}
              </div>
              <span
                className={cn(
                  'text-[10px] font-medium text-center leading-tight px-0.5',
                  done ? 'text-amber-700' : 'text-slate-400',
                  active && 'text-amber-900',
                )}
              >
                {step.label}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─── Delivered Celebration ────────────────────────────────────────────────────

function DeliveredBanner() {
  return (
    <div className="bg-emerald-50 border border-emerald-200 rounded-[14px] p-5 text-center animate-in fade-in duration-500">
      <div className="inline-flex items-center justify-center w-14 h-14 rounded-full bg-emerald-100 mb-3">
        <CheckCircle2 size={28} strokeWidth={1.75} className="text-emerald-600" />
      </div>
      <p className="font-display text-xl text-emerald-800 mb-1">Zamówienie dostarczone!</p>
      <p className="text-sm text-emerald-600">Smacznego! Dziękujemy za zamówienie.</p>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function TrackingPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [showDelivered, setShowDelivered] = useState(false);
  const prevStatus = useRef<OrderStatus | null>(null);

  const { data: order, isLoading, isError } = useOrder(id);

  // Show celebration when status transitions to DELIVERED
  useEffect(() => {
    if (!order) return;
    if (order.status === 'DELIVERED' && prevStatus.current !== 'DELIVERED') {
      setShowDelivered(true);
    }
    prevStatus.current = order.status;
  }, [order?.status]);

  return (
    <div className="min-h-dvh bg-[#FFF7ED]">
      {/* Header */}
      <header className="sticky top-0 z-30 h-14 flex items-center px-4 bg-[#FFF7ED]/90 backdrop-blur-sm border-b border-[#FCEAE1]">
        <button
          type="button"
          aria-label="Wróć do menu"
          onClick={() => navigate('/')}
          className="flex items-center gap-1.5 text-sm font-medium text-amber-700 hover:text-amber-900 transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded-lg h-10 px-2 -ml-2"
        >
          <ArrowLeft size={18} />
          <span>Menu</span>
        </button>
        <h1 className="font-display text-lg text-amber-900 mx-auto truncate px-2">Śledzenie zamówienia</h1>
        <div className="w-16" />
      </header>

      {isLoading && (
        <div className="flex items-center justify-center h-64">
          <Loader2 size={32} className="animate-spin text-amber-500" />
        </div>
      )}

      {isError && (
        <div className="flex flex-col items-center justify-center h-64 gap-3 px-6 text-center">
          <p className="font-display text-xl text-amber-900">Nie znaleziono zamówienia</p>
          <button
            type="button"
            onClick={() => navigate('/')}
            className="text-sm text-amber-600 underline underline-offset-2"
          >
            Wróć do menu
          </button>
        </div>
      )}

      {order && (
        <main className="px-4 pt-4 pb-12 max-w-lg mx-auto space-y-4">
          {/* Delivered celebration */}
          {showDelivered && <DeliveredBanner />}

          {/* Order header */}
          <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] p-4">
            <div className="flex items-start justify-between gap-2 mb-1">
              <div>
                <p className="font-mono text-xs text-[#64748B] mb-0.5">Zamówienie</p>
                <p className="font-mono text-sm font-bold text-slate-800 break-all">#{order.id.slice(0, 8).toUpperCase()}</p>
              </div>
              <span className={cn('text-xs font-medium px-2.5 py-1 rounded-full border', orderStatusClass(order.status))}>
                {orderStatusLabel(order.status)}
              </span>
            </div>
            <p className="text-xs text-[#64748B] flex items-center gap-1 mt-2">
              <Clock size={12} />
              {formatDate(order.createdAt)}
            </p>
            <p className="font-mono text-base font-bold text-amber-600 mt-1">
              {formatPrice(order.totalPrice)}
            </p>
          </div>

          {/* Progress */}
          <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] p-4">
            <h2 className="text-sm font-semibold text-slate-800 mb-4">Status zamówienia</h2>
            <ProgressSteps status={order.status} />
          </div>

          {/* Courier card */}
          {(order.driverName || order.estimatedDelivery) && (
            <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] p-4 flex items-center gap-4">
              {/* Avatar placeholder */}
              <div className="w-12 h-12 rounded-full bg-gradient-to-br from-amber-100 to-yellow-200 flex items-center justify-center flex-none">
                <Bike size={22} strokeWidth={1.4} className="text-amber-600" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs text-[#64748B] mb-0.5">Kurier</p>
                <p className="font-medium text-slate-800 text-sm">{order.driverName ?? 'Przypisywanie…'}</p>
                {order.estimatedDelivery && (
                  <p className="text-xs text-amber-600 flex items-center gap-1 mt-0.5">
                    <Clock size={11} />
                    ETA: {formatDate(order.estimatedDelivery)}
                  </p>
                )}
              </div>
            </div>
          )}

          {/* Map */}
          <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] overflow-hidden">
            <DeliveryMap address={order.address} />
            <div className="px-4 py-2.5 border-t border-[#FCEAE1]">
              <p className="text-xs text-[#64748B] flex items-start gap-1.5">
                <MapPin size={12} className="mt-0.5 flex-none text-amber-500" />
                {order.address.street}, {order.address.postalCode} {order.address.city}
              </p>
            </div>
          </div>

          {/* Order items */}
          <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] p-4">
            <h2 className="text-sm font-semibold text-slate-800 mb-3">Produkty</h2>
            <div className="space-y-2">
              {order.items.map((item, idx) => (
                <div key={idx} className="flex justify-between text-sm">
                  <span className="text-slate-700 truncate max-w-[220px]">
                    {item.product.name}
                    <span className="text-[#64748B] ml-1">×{item.quantity}</span>
                  </span>
                  <span className="font-mono text-slate-800 ml-2 flex-none">
                    {formatPrice(item.unitPrice * item.quantity)}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* Polling indicator */}
          <p className="text-center text-xs text-slate-400">
            Odświeżanie co 30 s
          </p>
        </main>
      )}
    </div>
  );
}
