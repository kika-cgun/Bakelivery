import { Loader2, Truck, CheckCircle2 } from 'lucide-react';
import { OpsLayout } from '@/components/layout/OpsLayout';
import { useDriverDeliveries, useUpdateDeliveryStatus } from '@ui/api/driver';
import { cn } from '@ui/lib/utils';
import type { Delivery } from '@ui/types';

// ─── Status helpers ──────────────────────────────────────────────────────────

type DeliveryStatus = Delivery['status'];

const STATUS_LABELS: Record<DeliveryStatus, string> = {
  ASSIGNED: 'Przypisano',
  PICKED_UP: 'Odebrano',
  IN_PROGRESS: 'W drodze',
  DELIVERED: 'Dostarczone',
};

const STATUS_COLORS: Record<DeliveryStatus, string> = {
  ASSIGNED: 'bg-amber-50 text-amber-700 border-amber-200',
  PICKED_UP: 'bg-blue-50 text-blue-700 border-blue-200',
  IN_PROGRESS: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  DELIVERED: 'bg-green-50 text-green-700 border-green-200',
};

const NEXT_STATUS: Partial<Record<DeliveryStatus, { status: DeliveryStatus; label: string }>> = {
  ASSIGNED: { status: 'PICKED_UP', label: 'Odebrano od piekarni' },
  PICKED_UP: { status: 'IN_PROGRESS', label: 'Ruszam do klienta' },
  IN_PROGRESS: { status: 'DELIVERED', label: 'Dostarczone' },
};

// ─── Delivery card ───────────────────────────────────────────────────────────

interface DeliveryCardProps {
  delivery: Delivery;
}

function DeliveryCard({ delivery }: DeliveryCardProps) {
  const { mutate: updateStatus, isPending, variables } = useUpdateDeliveryStatus();

  const next = NEXT_STATUS[delivery.status];
  const isThisPending = isPending && variables?.id === delivery.id;

  return (
    <article className="bg-white border border-slate-100 rounded-[10px] shadow-sm p-4">
      {/* Address */}
      <div className="mb-3">
        <p className="text-base font-semibold text-slate-800">
          {delivery.address.street}
        </p>
        <p className="text-sm text-slate-500">
          {delivery.address.postalCode} {delivery.address.city}
          {delivery.address.notes && (
            <span className="ml-1 text-slate-400">· {delivery.address.notes}</span>
          )}
        </p>
      </div>

      {/* Customer & status */}
      <div className="flex items-center justify-between mb-4">
        <p className="text-sm text-slate-600 font-medium">{delivery.customerName}</p>
        <span
          className={cn(
            'text-[11px] font-mono px-2 py-0.5 rounded-full border',
            STATUS_COLORS[delivery.status],
          )}
        >
          {STATUS_LABELS[delivery.status]}
        </span>
      </div>

      {/* Next action button */}
      {next && delivery.status !== 'DELIVERED' && (
        <button
          type="button"
          onClick={() => updateStatus({ id: delivery.id, status: next.status })}
          disabled={isThisPending}
          className="w-full flex items-center justify-center gap-2 bg-amber-500 hover:bg-amber-400 disabled:opacity-60 disabled:cursor-not-allowed text-white text-sm font-medium py-2.5 rounded-lg transition-colors"
        >
          {isThisPending ? (
            <Loader2 size={14} className="animate-spin" />
          ) : (
            <CheckCircle2 size={14} />
          )}
          {isThisPending ? 'Aktualizuję…' : next.label}
        </button>
      )}

      {delivery.status === 'DELIVERED' && (
        <div className="flex items-center justify-center gap-1.5 py-2 text-green-600 text-sm font-medium">
          <CheckCircle2 size={15} />
          Dostawa zakończona
        </div>
      )}
    </article>
  );
}

// ─── Stats ───────────────────────────────────────────────────────────────────

interface StatsRowProps {
  deliveries: Delivery[];
}

function StatsRow({ deliveries }: StatsRowProps) {
  const active = deliveries.filter((d) => d.status !== 'DELIVERED').length;
  const delivered = deliveries.filter((d) => d.status === 'DELIVERED').length;

  return (
    <div className="grid grid-cols-2 gap-3 mb-5">
      <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm p-4">
        <p className="text-xs font-mono uppercase tracking-wide text-slate-400 mb-1">Aktywne</p>
        <p className="font-mono text-2xl font-bold text-slate-800">{active}</p>
      </div>
      <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm p-4">
        <p className="text-xs font-mono uppercase tracking-wide text-slate-400 mb-1">Dostarczone dziś</p>
        <p className="font-mono text-2xl font-bold text-green-600">{delivered}</p>
      </div>
    </div>
  );
}

// ─── Page ────────────────────────────────────────────────────────────────────

export default function DeliveriesPage() {
  const { data: deliveries, isLoading } = useDriverDeliveries();

  const activeDeliveries = (deliveries ?? []).filter((d) => d.status !== 'DELIVERED');
  const completedDeliveries = (deliveries ?? []).filter((d) => d.status === 'DELIVERED');

  return (
    <OpsLayout title="Moje dostawy">
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2].map((n) => (
            <div key={n} className="bg-white border border-slate-100 rounded-[10px] shadow-sm p-4 animate-pulse">
              <div className="h-5 w-48 bg-slate-100 rounded mb-2" />
              <div className="h-3 w-32 bg-slate-100 rounded mb-4" />
              <div className="h-10 bg-slate-100 rounded-lg" />
            </div>
          ))}
        </div>
      ) : (
        <>
          <StatsRow deliveries={deliveries ?? []} />

          {/* Map placeholder */}
          <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm p-6 mb-5 flex items-center justify-center min-h-[160px]">
            <div className="text-center text-slate-400">
              <Truck size={32} className="mx-auto mb-2 opacity-40" />
              <p className="text-sm">Mapa trasy · Leaflet załaduje się po instalacji node_modules</p>
            </div>
          </div>

          {/* Active deliveries */}
          {activeDeliveries.length > 0 && (
            <section className="mb-5">
              <h2 className="text-xs font-mono uppercase tracking-widest text-slate-400 mb-3">
                Aktywne dostawy
              </h2>
              <div className="space-y-3">
                {activeDeliveries.map((d) => (
                  <DeliveryCard key={d.id} delivery={d} />
                ))}
              </div>
            </section>
          )}

          {/* Completed deliveries */}
          {completedDeliveries.length > 0 && (
            <section>
              <h2 className="text-xs font-mono uppercase tracking-widest text-slate-400 mb-3">
                Dostarczone dziś
              </h2>
              <div className="space-y-3">
                {completedDeliveries.map((d) => (
                  <DeliveryCard key={d.id} delivery={d} />
                ))}
              </div>
            </section>
          )}

          {!deliveries?.length && (
            <div className="flex flex-col items-center justify-center py-20 text-slate-400">
              <Truck size={36} className="mb-3 opacity-40" />
              <p className="text-sm">Brak aktywnych dostaw. Czekaj na zlecenia.</p>
            </div>
          )}
        </>
      )}
    </OpsLayout>
  );
}
