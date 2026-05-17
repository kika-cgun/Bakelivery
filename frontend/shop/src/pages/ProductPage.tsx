import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Plus, Minus, ShoppingCart } from 'lucide-react';
import { useProduct } from '@ui/api/catalog';
import { cn, formatPrice } from '@ui/lib/utils';
import { useCartStore } from '@/store/cart';

const GRADIENT_PAIRS = [
  ['#FFF7ED', '#FEF3C7'],
  ['#FFF7ED', '#FFEDD5'],
  ['#FEF9C3', '#FEF3C7'],
  ['#FFEDD5', '#FEF3C7'],
  ['#FFF7ED', '#FDE68A'],
];

function getGradientPair(id: string): [string, string] {
  const idx = id.charCodeAt(id.length - 1) % GRADIENT_PAIRS.length;
  return GRADIENT_PAIRS[idx] as [string, string];
}

export default function ProductPage() {
  const { id = '' } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [quantity, setQuantity] = useState(1);

  const { data: product, isLoading, isError } = useProduct(id);

  const addItem = useCartStore((s) => s.addItem);
  const items = useCartStore((s) => s.items);
  const cartQty = items.find((i) => i.product.id === id)?.quantity ?? 0;

  function handleAddToCart() {
    if (!product?.available) return;
    for (let i = 0; i < quantity; i++) {
      addItem(product);
    }
    navigate(-1);
  }

  const [from, to] = product ? getGradientPair(product.id) : ['#FFF7ED', '#FEF3C7'];

  return (
    <div className="min-h-dvh bg-[#FFF7ED] flex flex-col">
      {/* Sticky back button */}
      <div className="sticky top-0 z-30 h-14 flex items-center px-4 bg-[#FFF7ED]/90 backdrop-blur-sm">
        <button
          type="button"
          aria-label="Wróć do menu"
          onClick={() => navigate(-1)}
          className="flex items-center gap-1.5 text-sm font-medium text-amber-700 hover:text-amber-900 transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded-lg h-10 px-2 -ml-2"
        >
          <ArrowLeft size={18} />
          <span>Wróć do menu</span>
        </button>
      </div>

      {isLoading && (
        <div className="flex-1 animate-pulse">
          <div className="h-64 bg-amber-100" />
          <div className="p-5 space-y-3">
            <div className="h-8 bg-amber-100 rounded w-3/4" />
            <div className="h-4 bg-amber-50 rounded w-1/3" />
            <div className="h-4 bg-amber-50 rounded w-full mt-4" />
            <div className="h-4 bg-amber-50 rounded w-5/6" />
          </div>
        </div>
      )}

      {isError && (
        <div className="flex-1 flex flex-col items-center justify-center gap-3 px-6 text-center">
          <span className="text-5xl">😕</span>
          <p className="font-display text-xl text-amber-900">Nie znaleziono produktu</p>
          <button
            type="button"
            onClick={() => navigate('/')}
            className="text-sm text-amber-600 underline underline-offset-2 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded"
          >
            Wróć do katalogu
          </button>
        </div>
      )}

      {product && (
        <>
          {/* Hero image / gradient */}
          <div
            className="h-64 flex items-center justify-center relative overflow-hidden"
            style={{ background: `linear-gradient(135deg, ${from} 0%, ${to} 100%)` }}
          >
            {product.imageUrl ? (
              <img
                src={product.imageUrl}
                alt={product.name}
                className="w-full h-full object-cover"
              />
            ) : (
              <span className="text-8xl select-none drop-shadow-sm">🍞</span>
            )}
            {!product.available && (
              <div className="absolute inset-0 bg-white/50 flex items-center justify-center">
                <span className="bg-white text-slate-500 font-medium text-sm px-4 py-2 rounded-full border border-slate-200 shadow-sm">
                  Produkt niedostępny
                </span>
              </div>
            )}
          </div>

          {/* Details */}
          <div className="flex-1 flex flex-col px-5 pt-5 pb-32">
            {/* Header */}
            <div className="flex items-start justify-between gap-3 mb-1">
              <h1 className="font-display text-2xl text-amber-900 leading-tight flex-1">
                {product.name}
              </h1>
              <span className="font-mono text-xl font-bold text-amber-600 whitespace-nowrap pt-0.5">
                {formatPrice(product.price)}
              </span>
            </div>

            {product.bakeryName && (
              <p className="text-sm text-[#64748B] mb-1">
                Piekarnia: <span className="font-medium text-slate-700">{product.bakeryName}</span>
              </p>
            )}
            {product.weight && (
              <p className="font-mono text-xs text-[#64748B] mb-3">{product.weight}</p>
            )}

            {product.description && (
              <p className="text-sm text-slate-600 leading-relaxed mb-4">{product.description}</p>
            )}

            {cartQty > 0 && (
              <p className="text-xs text-amber-600 font-medium mb-3 flex items-center gap-1.5">
                <ShoppingCart size={14} />
                W koszyku: {cartQty} szt.
              </p>
            )}
          </div>

          {/* Fixed bottom CTA */}
          <div className="fixed bottom-0 inset-x-0 z-40 bg-[#FFF7ED]/90 backdrop-blur-sm p-4 border-t border-[#FCEAE1]">
            <div className="flex items-center gap-3 max-w-sm mx-auto">
              {/* Quantity stepper */}
              <div className="flex items-center gap-2 bg-white border border-[#FCEAE1] rounded-[8px] px-2 h-[52px]">
                <button
                  type="button"
                  aria-label="Zmniejsz ilość"
                  onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                  disabled={quantity <= 1 || !product.available}
                  className="w-8 h-8 flex items-center justify-center rounded text-amber-600 hover:bg-amber-50 disabled:text-slate-300 disabled:cursor-not-allowed transition-colors focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none"
                >
                  <Minus size={16} />
                </button>
                <span className="font-mono font-bold text-slate-800 w-5 text-center text-sm">
                  {quantity}
                </span>
                <button
                  type="button"
                  aria-label="Zwiększ ilość"
                  onClick={() => setQuantity((q) => q + 1)}
                  disabled={!product.available}
                  className="w-8 h-8 flex items-center justify-center rounded text-amber-600 hover:bg-amber-50 disabled:text-slate-300 disabled:cursor-not-allowed transition-colors focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none"
                >
                  <Plus size={16} />
                </button>
              </div>

              {/* Add to cart */}
              <button
                type="button"
                disabled={!product.available}
                onClick={handleAddToCart}
                className={cn(
                  'flex-1 h-[52px] rounded-[8px] font-semibold text-sm transition-all duration-150 focus-visible:ring-2 focus-visible:ring-amber-900 focus-visible:outline-none',
                  product.available
                    ? 'bg-amber-600 hover:bg-amber-700 text-white shadow-primary active:scale-[0.99]'
                    : 'bg-slate-100 text-slate-400 cursor-not-allowed',
                )}
              >
                {product.available
                  ? `Dodaj do koszyka · ${formatPrice(product.price * quantity)}`
                  : 'Niedostępny'}
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
