import { useEffect, useCallback, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import { OpsLayout } from '@/components/layout/OpsLayout';
import { useBakeryOrders, useUpdateOrderStatus } from '@ui/api/orders';
import { useRealtimeStore } from '@/store/realtime';
import {
  cn,
  formatRelativeTime,
  formatPrice,
  orderStatusLabel,
  orderStatusClass,
  orderStatusBorderColor,
} from '@ui/lib/utils';
import type { Order, OrderStatus } from '@ui/types';

// ─── Filter tabs ────────────────────────────────────────────────────────────

type FilterTab = 'new' | 'active' | 'delivered' | 'all';

const NEW_STATUSES: OrderStatus[] = ['PENDING'];
const ACTIVE_STATUSES: OrderStatus[] = ['ACCEPTED', 'BAKING', 'READY_FOR_PICKUP', 'IN_DELIVERY'];
const DELIVERED_STATUSES: OrderStatus[] = ['DELIVERED', 'REJECTED', 'CANCELLED'];

function filterOrders(orders: Order[], tab: FilterTab): Order[] {
  switch (tab) {
    case 'new':
      return orders.filter((o) => NEW_STATUSES.includes(o.status));
    case 'active':
      return orders.filter((o) => ACTIVE_STATUSES.includes(o.status));
    case 'delivered':
      return orders.filter((o) => DELIVERED_STATUSES.includes(o.status));
    default:
      return orders;
  }
}

// ─── Order card ─────────────────────────────────────────────────────────────

interface OrderCardProps {
  order: Order;
}

function OrderCard({ order }: OrderCardProps) {
  const { mutate: updateStatus, isPending } = useUpdateOrderStatus();

  const borderColor = orderStatusBorderColor(order.status);
  const shortId = order.id.slice(-4).toUpperCase();
  const time = new Date(order.createdAt).toLocaleTimeString('pl-PL', {
    hour: '2-digit',
    minute: '2-digit',
  });

  function accept() {
    updateStatus({ id: order.id, status: 'ACCEPTED' });
  }

  function reject() {
    updateStatus({ id: order.id, status: 'REJECTED' });
  }

  return (
    <article
      className="bg-white border border-slate-100 rounded-[10px] shadow-sm overflow-hidden"
      style={{ borderLeft: `3px solid ${borderColor}` }}
    >
      <div className="p-4">
        {/* Header */}
        <div className="flex items-start justify-between gap-2 mb-3">
          <div>
            <span className="font-mono text-sm font-semibold text-slate-700">
              #{shortId} · {time}
            </span>
            <p className="text-sm text-slate-500 mt-0.5">{order.customerName}</p>
          </div>
          <div className="flex items-center gap-2 shrink-0">
            <span
              className={cn(
                'text-[11px] font-mono px-2 py-0.5 rounded-full border',
                orderStatusClass(order.status),
              )}
            >
              {orderStatusLabel(order.status)}
            </span>
          </div>
        </div>

        {/* Items */}
        <ul className="space-y-0.5 mb-3">
          {order.items.map((item) => (
            <li key={item.product.id} className="flex items-center justify-between text-sm text-slate-600">
              <span>
                {item.quantity}× {item.product.name}
              </span>
              <span className="font-mono text-xs text-slate-400">
                {formatPrice(item.unitPrice * item.quantity)}
              </span>
            </li>
          ))}
        </ul>

        {/* Footer */}
        <div className="flex items-center justify-between pt-2 border-t border-slate-50">
          <span className="font-mono text-sm font-bold text-slate-800">
            {formatPrice(order.totalPrice)}
          </span>
          <span className="font-mono text-xs text-slate-400">
            {formatRelativeTime(order.createdAt)}
          </span>
        </div>

        {/* PENDING actions */}
        {order.status === 'PENDING' && (
          <div className="flex gap-2 mt-3">
            <button
              type="button"
              onClick={accept}
              disabled={isPending}
              className="flex-1 flex items-center justify-center gap-1.5 bg-amber-500 hover:bg-amber-400 disabled:opacity-60 disabled:cursor-not-allowed text-white text-sm font-medium py-2 rounded-lg transition-colors"
            >
              {isPending ? (
                <Loader2 size={14} className="animate-spin" />
              ) : (
                <CheckCircle2 size={14} />
              )}
              Akceptuj
            </button>
            <button
              type="button"
              onClick={reject}
              disabled={isPending}
              className="flex-1 flex items-center justify-center gap-1.5 bg-red-50 hover:bg-red-100 disabled:opacity-60 disabled:cursor-not-allowed text-red-600 text-sm font-medium py-2 rounded-lg transition-colors border border-red-200"
            >
              {isPending ? (
                <Loader2 size={14} className="animate-spin" />
              ) : (
                <XCircle size={14} />
              )}
              Odrzuć
            </button>
          </div>
        )}
      </div>
    </article>
  );
}

// ─── Skeleton ────────────────────────────────────────────────────────────────

function OrderSkeleton() {
  return (
    <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm p-4 animate-pulse">
      <div className="flex justify-between mb-3">
        <div className="space-y-1.5">
          <div className="h-4 w-28 bg-slate-100 rounded" />
          <div className="h-3 w-20 bg-slate-100 rounded" />
        </div>
        <div className="h-5 w-20 bg-slate-100 rounded-full" />
      </div>
      <div className="space-y-1.5 mb-3">
        <div className="h-3 w-full bg-slate-100 rounded" />
        <div className="h-3 w-3/4 bg-slate-100 rounded" />
      </div>
      <div className="flex justify-between pt-2 border-t border-slate-50">
        <div className="h-4 w-16 bg-slate-100 rounded" />
        <div className="h-3 w-14 bg-slate-100 rounded" />
      </div>
    </div>
  );
}

// ─── Page ────────────────────────────────────────────────────────────────────

export default function OrdersPage() {
  const queryClient = useQueryClient();
  const { data: orders, isLoading } = useBakeryOrders();
  const { connect, disconnect } = useRealtimeStore();

  const handleRealtimeEvent = useCallback(() => {
    void queryClient.invalidateQueries({ queryKey: ['orders', 'bakery'] });
  }, [queryClient]);

  useEffect(() => {
    connect(handleRealtimeEvent);
    return () => disconnect();
  }, [connect, disconnect, handleRealtimeEvent]);

  const [activeTab, setActiveTab] = useState<FilterTab>('new');

  const allOrders = orders ?? [];
  const filtered = filterOrders(allOrders, activeTab);

  const counts = {
    new: filterOrders(allOrders, 'new').length,
    active: filterOrders(allOrders, 'active').length,
    delivered: filterOrders(allOrders, 'delivered').length,
    all: allOrders.length,
  };

  const tabs: { id: FilterTab; label: string }[] = [
    { id: 'new', label: `Nowe (${counts.new})` },
    { id: 'active', label: `W trakcie (${counts.active})` },
    { id: 'delivered', label: `Dostarczone (${counts.delivered})` },
    { id: 'all', label: `Wszystkie (${counts.all})` },
  ];

  return (
    <OpsLayout title="Zamówienia">
      {/* Filter tabs */}
      <div className="flex gap-1 mb-5 overflow-x-auto scrollbar-none">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setActiveTab(tab.id)}
            className={cn(
              'shrink-0 px-3 py-1.5 rounded-lg text-sm font-medium transition-colors whitespace-nowrap',
              activeTab === tab.id
                ? 'bg-amber-500 text-white'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50',
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      {isLoading ? (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <OrderSkeleton />
          <OrderSkeleton />
          <OrderSkeleton />
        </div>
      ) : filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-400">
          <span className="text-4xl mb-3">📋</span>
          <p className="text-sm">Brak zamówień w tej kategorii</p>
        </div>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {filtered.map((order) => (
            <OrderCard key={order.id} order={order} />
          ))}
        </div>
      )}
    </OpsLayout>
  );
}

