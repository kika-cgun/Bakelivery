import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { OrderStatus } from '../types';

/** shadcn/ui utility */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** Format price from grosze to PLN string */
export function formatPrice(grosze: number): string {
  return new Intl.NumberFormat('pl-PL', {
    style: 'currency',
    currency: 'PLN',
  }).format(grosze / 100);
}

/** Format ISO date to Polish locale string */
export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat('pl-PL', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso));
}

/** Format relative time (e.g. "2 min temu") */
export function formatRelativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 1) return 'przed chwilą';
  if (minutes === 1) return '1 min temu';
  if (minutes < 60) return `${minutes} min temu`;
  const hours = Math.floor(minutes / 60);
  return `${hours} godz. temu`;
}

/** Human-readable order status label */
export function orderStatusLabel(status: OrderStatus): string {
  const labels: Record<OrderStatus, string> = {
    PENDING:          'Oczekuje',
    ACCEPTED:         'Przyjęte',
    BAKING:           'W pieczeniu',
    READY_FOR_PICKUP: 'Gotowe',
    IN_DELIVERY:      'W dostawie',
    DELIVERED:        'Dostarczone',
    REJECTED:         'Odrzucone',
    CANCELLED:        'Anulowane',
  };
  return labels[status] ?? status;
}

/** CSS class for order status badge */
export function orderStatusClass(status: OrderStatus): string {
  const map: Record<OrderStatus, string> = {
    PENDING:          'bg-amber-50 text-amber-700 border-amber-200',
    ACCEPTED:         'bg-blue-50 text-blue-700 border-blue-200',
    BAKING:           'bg-orange-50 text-orange-700 border-orange-200',
    READY_FOR_PICKUP: 'bg-yellow-50 text-yellow-700 border-yellow-200',
    IN_DELIVERY:      'bg-indigo-50 text-indigo-700 border-indigo-200',
    DELIVERED:        'bg-green-50 text-green-700 border-green-200',
    REJECTED:         'bg-red-50 text-red-700 border-red-200',
    CANCELLED:        'bg-slate-50 text-slate-500 border-slate-200',
  };
  return map[status] ?? 'bg-slate-50 text-slate-500';
}

/** Left border colour for order card */
export function orderStatusBorderColor(status: OrderStatus): string {
  const map: Record<OrderStatus, string> = {
    PENDING:          '#d97706',
    ACCEPTED:         '#3b82f6',
    BAKING:           '#f97316',
    READY_FOR_PICKUP: '#eab308',
    IN_DELIVERY:      '#6366f1',
    DELIVERED:        '#10b981',
    REJECTED:         '#ef4444',
    CANCELLED:        '#94a3b8',
  };
  return map[status] ?? '#94a3b8';
}
