import { Link, useNavigate } from 'react-router-dom';
import { Minus, Plus, X, ShoppingCart, ArrowLeft } from 'lucide-react';
import { cn, formatPrice } from '@ui/lib/utils';
import { useCartStore } from '@/store/cart';
import type { CartItem } from '@ui/types';

const DELIVERY_THRESHOLD = 6000;  // 60 zł in grosze
const DELIVERY_FEE = 899;          // 8,99 zł in grosze
const ORDER_MINIMUM = 3000;        // 30 zł in grosze

// ─── Cart Item Row ────────────────────────────────────────────────────────────

interface CartItemRowProps {
  item: CartItem;
  onIncrease: () => void;
  onDecrease: () => void;
  onRemove: () => void;
}

function CartItemRow({ item, onIncrease, onDecrease, onRemove }: CartItemRowProps) {
  const subtotal = item.product.price * item.quantity;
  return (
    <div className="flex items-center gap-3 py-3 border-b border-[#FCEAE1] last:border-b-0">
      {/* Color swatch placeholder */}
      <div className="w-12 h-12 rounded-[8px] bg-gradient-to-br from-amber-50 to-yellow-100 flex items-center justify-center flex-none text-xl select-none">
        🍞
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-slate-800 truncate">{item.product.name}</p>
        <p className="font-mono text-xs text-[#64748B]">{formatPrice(item.product.price)} / szt.</p>
      </div>

      {/* Stepper */}
      <div className="flex items-center gap-1">
        <button
          type="button"
          aria-label="Zmniejsz ilość"
          onClick={onDecrease}
          className="w-8 h-8 flex items-center justify-center rounded-lg border border-[#FCEAE1] bg-white text-amber-600 hover:bg-amber-50 transition-colors focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none"
        >
          <Minus size={14} />
        </button>
        <span className="font-mono text-sm font-bold w-5 text-center text-slate-800">
          {item.quantity}
        </span>
        <button
          type="button"
          aria-label="Zwiększ ilość"
          onClick={onIncrease}
          className="w-8 h-8 flex items-center justify-center rounded-lg border border-[#FCEAE1] bg-white text-amber-600 hover:bg-amber-50 transition-colors focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none"
        >
          <Plus size={14} />
        </button>
      </div>

      {/* Subtotal + remove */}
      <div className="flex flex-col items-end gap-1 flex-none">
        <span className="font-mono text-sm font-semibold text-slate-800">{formatPrice(subtotal)}</span>
        <button
          type="button"
          aria-label={`Usuń ${item.product.name} z koszyka`}
          onClick={onRemove}
          className="text-slate-300 hover:text-red-500 transition-colors focus-visible:ring-2 focus-visible:ring-red-500 focus-visible:outline-none rounded"
        >
          <X size={14} />
        </button>
      </div>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function CartPage() {
  const navigate = useNavigate();
  const items = useCartStore((s) => s.items);
  const totalItems = useCartStore((s) => s.totalItems());
  const totalPrice = useCartStore((s) => s.totalPrice());
  const updateQuantity = useCartStore((s) => s.updateQuantity);
  const removeItem = useCartStore((s) => s.removeItem);

  const deliveryFee = totalPrice >= DELIVERY_THRESHOLD ? 0 : DELIVERY_FEE;
  const grandTotal = totalPrice + deliveryFee;
  const belowMinimum = totalPrice < ORDER_MINIMUM;
  const canCheckout = items.length > 0 && !belowMinimum;

  return (
    <div className="min-h-dvh bg-[#FFF7ED] flex flex-col">
      {/* Header */}
      <header className="sticky top-0 z-30 h-14 flex items-center px-4 bg-[#FFF7ED]/90 backdrop-blur-sm border-b border-[#FCEAE1]">
        <button
          type="button"
          aria-label="Wróć do menu"
          onClick={() => navigate(-1)}
          className="flex items-center gap-1.5 text-sm font-medium text-amber-700 hover:text-amber-900 transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded-lg h-10 px-2 -ml-2"
        >
          <ArrowLeft size={18} />
          <span>Menu</span>
        </button>
        <h1 className="font-display text-lg text-amber-900 mx-auto">
          {items.length > 0 ? `Koszyk (${totalItems})` : 'Koszyk'}
        </h1>
        {/* Spacer to center the title */}
        <div className="w-16" />
      </header>

      {/* Empty state */}
      {items.length === 0 && (
        <div className="flex-1 flex flex-col items-center justify-center gap-4 px-6 text-center">
          <div className="w-20 h-20 rounded-full bg-amber-100 flex items-center justify-center">
            <ShoppingCart size={36} className="text-amber-400" />
          </div>
          <div>
            <p className="font-display text-xl text-amber-900 mb-1">Koszyk jest pusty</p>
            <p className="text-sm text-[#64748B]">Dodaj produkty, żeby złożyć zamówienie</p>
          </div>
          <Link
            to="/"
            className="mt-2 inline-flex items-center justify-center h-11 px-6 bg-amber-600 text-white text-sm font-semibold rounded-[8px] shadow-primary hover:bg-amber-700 transition-colors focus-visible:ring-2 focus-visible:ring-amber-900 focus-visible:outline-none"
          >
            Przejdź do menu
          </Link>
        </div>
      )}

      {/* Items */}
      {items.length > 0 && (
        <main className="flex-1 px-4 pt-4 pb-48">
          <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] px-4">
            {items.map((item) => (
              <CartItemRow
                key={item.product.id}
                item={item}
                onIncrease={() => updateQuantity(item.product.id, item.quantity + 1)}
                onDecrease={() => updateQuantity(item.product.id, item.quantity - 1)}
                onRemove={() => removeItem(item.product.id)}
              />
            ))}
          </div>

          {/* Summary */}
          <div className="mt-4 bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] p-4 space-y-2">
            <div className="flex justify-between text-sm text-slate-600">
              <span>Suma produktów</span>
              <span className="font-mono font-medium text-slate-800">{formatPrice(totalPrice)}</span>
            </div>
            <div className="flex justify-between text-sm text-slate-600">
              <span>Dostawa</span>
              <span className={cn('font-mono font-medium', deliveryFee === 0 ? 'text-emerald-600' : 'text-slate-800')}>
                {deliveryFee === 0 ? 'Darmowa' : formatPrice(deliveryFee)}
              </span>
            </div>
            {deliveryFee > 0 && (
              <p className="text-xs text-[#64748B]">
                Dodaj jeszcze <span className="font-mono font-semibold text-amber-600">{formatPrice(DELIVERY_THRESHOLD - totalPrice)}</span> do darmowej dostawy
              </p>
            )}
            <div className="border-t border-[#FCEAE1] pt-2 flex justify-between">
              <span className="font-semibold text-slate-800">Łącznie</span>
              <span className="font-mono font-bold text-amber-600 text-base">{formatPrice(grandTotal)}</span>
            </div>
          </div>

          {belowMinimum && (
            <p className="mt-3 text-xs text-red-600 text-center">
              Minimalne zamówienie to <span className="font-mono font-semibold">{formatPrice(ORDER_MINIMUM)}</span>. Dodaj więcej produktów.
            </p>
          )}
        </main>
      )}

      {/* Bottom CTA */}
      {items.length > 0 && (
        <div className="fixed bottom-0 inset-x-0 z-40 p-4 bg-[#FFF7ED]/90 backdrop-blur-sm border-t border-[#FCEAE1]">
          <button
            type="button"
            disabled={!canCheckout}
            onClick={() => navigate('/checkout')}
            className={cn(
              'w-full h-14 rounded-[14px] font-semibold text-sm transition-all duration-150 focus-visible:ring-2 focus-visible:ring-amber-900 focus-visible:outline-none',
              canCheckout
                ? 'bg-amber-600 hover:bg-amber-700 text-white shadow-primary active:scale-[0.99]'
                : 'bg-slate-100 text-slate-400 cursor-not-allowed',
            )}
          >
            {belowMinimum
              ? `Minimum ${formatPrice(ORDER_MINIMUM)}`
              : `Przejdź do zamówienia · ${formatPrice(grandTotal)}`}
          </button>
        </div>
      )}
    </div>
  );
}
