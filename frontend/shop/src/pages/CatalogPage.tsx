import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, Search, Plus } from 'lucide-react';
import { useProducts, useCategories } from '@ui/api/catalog';
import { cn, formatPrice } from '@ui/lib/utils';
import { useCartStore } from '@/store/cart';
import type { Product } from '@ui/types';

// ─── Skeleton ────────────────────────────────────────────────────────────────

function ProductCardSkeleton() {
  return (
    <div className="rounded-[14px] bg-white border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] overflow-hidden animate-pulse">
      <div className="h-36 bg-amber-100" />
      <div className="p-3 space-y-2">
        <div className="h-4 bg-amber-100 rounded w-3/4" />
        <div className="h-3 bg-amber-50 rounded w-1/2" />
        <div className="flex items-center justify-between mt-3">
          <div className="h-4 bg-amber-100 rounded w-1/3" />
          <div className="h-8 w-8 bg-amber-100 rounded-full" />
        </div>
      </div>
    </div>
  );
}

// ─── Product Card ─────────────────────────────────────────────────────────────

const CARD_GRADIENTS = [
  'from-amber-50 to-yellow-100',
  'from-orange-50 to-amber-100',
  'from-yellow-50 to-orange-100',
  'from-amber-100 to-yellow-50',
  'from-orange-100 to-amber-50',
  'from-yellow-100 to-orange-50',
];

function getGradient(id: string): string {
  const idx = id.charCodeAt(id.length - 1) % CARD_GRADIENTS.length;
  return CARD_GRADIENTS[idx];
}

interface ProductCardProps {
  product: Product;
  onAdd: (product: Product) => void;
}

function ProductCard({ product, onAdd }: ProductCardProps) {
  return (
    <Link
      to={`/products/${product.id}`}
      className="group rounded-[14px] bg-white border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] overflow-hidden flex flex-col transition-transform duration-150 hover:scale-[1.02] focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none"
    >
      {/* Image / Gradient placeholder */}
      <div className={cn('h-36 bg-gradient-to-br flex items-center justify-center relative', getGradient(product.id))}>
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-full h-full object-cover"
          />
        ) : (
          <span className="text-4xl select-none">🍞</span>
        )}
        {!product.available && (
          <div className="absolute inset-0 bg-white/60 flex items-center justify-center">
            <span className="text-xs font-medium text-slate-500 bg-white/90 px-2 py-1 rounded-full border border-slate-200">
              Niedostępny
            </span>
          </div>
        )}
      </div>

      {/* Info */}
      <div className="p-3 flex flex-col flex-1">
        <p className="font-display text-sm leading-snug text-slate-900 line-clamp-2">{product.name}</p>
        {product.bakeryName && (
          <p className="text-xs text-[#64748B] mt-0.5 truncate">{product.bakeryName}</p>
        )}

        <div className="flex items-center justify-between mt-auto pt-2">
          <span className="font-mono text-sm font-semibold text-amber-600">
            {formatPrice(product.price)}
          </span>
          <button
            type="button"
            aria-label={`Dodaj ${product.name} do koszyka`}
            disabled={!product.available}
            onClick={(e) => {
              e.preventDefault();
              if (product.available) onAdd(product);
            }}
            className={cn(
              'w-8 h-8 rounded-full flex items-center justify-center transition-all duration-150',
              'focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none',
              product.available
                ? 'bg-amber-600 text-white shadow-primary hover:bg-amber-700 active:scale-95'
                : 'bg-slate-100 text-slate-300 cursor-not-allowed',
            )}
          >
            <Plus size={16} strokeWidth={2.5} />
          </button>
        </div>
      </div>
    </Link>
  );
}

// ─── Navbar ───────────────────────────────────────────────────────────────────

interface NavbarProps {
  totalItems: number;
}

function Navbar({ totalItems }: NavbarProps) {
  const navigate = useNavigate();
  return (
    <header className="fixed top-0 inset-x-0 z-40 h-14 bg-white border-b border-[#FCEAE1] shadow-sm flex items-center px-4 gap-3">
      <span className="font-display text-xl text-amber-900 flex-1">Bakelivery</span>
      <button
        type="button"
        aria-label="Szukaj produktów"
        className="w-10 h-10 flex items-center justify-center rounded-lg text-slate-500 hover:text-amber-600 hover:bg-amber-50 transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none"
      >
        <Search size={20} />
      </button>
      <button
        type="button"
        aria-label={`Koszyk — ${totalItems} produktów`}
        onClick={() => navigate('/cart')}
        className="relative w-10 h-10 flex items-center justify-center rounded-lg text-slate-500 hover:text-amber-600 hover:bg-amber-50 transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none"
      >
        <ShoppingCart size={20} />
        {totalItems > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] bg-amber-600 text-white text-[10px] font-bold font-mono rounded-full flex items-center justify-center px-1 leading-none">
            {totalItems > 99 ? '99+' : totalItems}
          </span>
        )}
      </button>
    </header>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function CatalogPage() {
  const [activeCategoryId, setActiveCategoryId] = useState<string | undefined>(undefined);

  const { data: categories = [], isLoading: catsLoading } = useCategories();
  const { data: products = [], isLoading: prodsLoading } = useProducts(activeCategoryId);

  const addItem = useCartStore((s) => s.addItem);
  const totalItems = useCartStore((s) => s.totalItems());
  const totalPrice = useCartStore((s) => s.totalPrice());
  const navigate = useNavigate();

  const isLoading = prodsLoading;

  return (
    <div className="min-h-dvh bg-[#FFF7ED]">
      <Navbar totalItems={totalItems} />

      <main className="pt-14">
        {/* Hero */}
        <section className="px-4 pt-8 pb-6">
          <p className="font-mono text-xs text-amber-600 uppercase tracking-widest mb-2">
            Dostawa 30–45 min
          </p>
          <h1 className="font-display text-3xl sm:text-4xl text-amber-900 leading-tight mb-2">
            Świeże pieczywo<br />prosto do drzwi
          </h1>
          <p className="text-sm text-[#64748B]">
            Min. zamówienie <span className="font-mono font-semibold">30,00 zł</span> · Darmowa dostawa od <span className="font-mono font-semibold">60,00 zł</span>
          </p>
        </section>

        {/* Category chips */}
        <section className="px-4 mb-4">
          <div className="flex gap-2 overflow-x-auto scrollbar-none pb-1">
            <button
              type="button"
              onClick={() => setActiveCategoryId(undefined)}
              className={cn(
                'flex-none px-4 h-9 rounded-full text-sm font-medium transition-all duration-150 border',
                'focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none',
                activeCategoryId === undefined
                  ? 'bg-amber-600 text-white border-amber-600'
                  : 'bg-white text-slate-600 border-[#FCEAE1] hover:border-amber-300',
              )}
            >
              Wszystkie
            </button>

            {catsLoading
              ? Array.from({ length: 4 }).map((_, i) => (
                  <div key={i} className="flex-none h-9 w-24 rounded-full bg-amber-100 animate-pulse" />
                ))
              : categories.map((cat) => (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => setActiveCategoryId(cat.id)}
                    className={cn(
                      'flex-none px-4 h-9 rounded-full text-sm font-medium transition-all duration-150 border whitespace-nowrap',
                      'focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none',
                      activeCategoryId === cat.id
                        ? 'bg-amber-600 text-white border-amber-600'
                        : 'bg-white text-slate-600 border-[#FCEAE1] hover:border-amber-300',
                    )}
                  >
                    {cat.name}
                  </button>
                ))}
          </div>
        </section>

        {/* Product grid */}
        <section className="px-4 pb-32">
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
            {isLoading
              ? Array.from({ length: 6 }).map((_, i) => <ProductCardSkeleton key={i} />)
              : products.map((product) => (
                  <ProductCard key={product.id} product={product} onAdd={addItem} />
                ))}
          </div>

          {!isLoading && products.length === 0 && (
            <div className="flex flex-col items-center py-16 text-center">
              <span className="text-5xl mb-4">🍞</span>
              <p className="font-display text-lg text-amber-900 mb-1">Brak produktów</p>
              <p className="text-sm text-[#64748B]">Spróbuj wybrać inną kategorię</p>
            </div>
          )}
        </section>
      </main>

      {/* Cart bottom bar */}
      {totalItems > 0 && (
        <div className="fixed bottom-0 inset-x-0 z-50 p-3 bg-[#FFF7ED]/80 backdrop-blur-sm">
          <button
            type="button"
            onClick={() => navigate('/cart')}
            className="w-full h-14 bg-amber-600 hover:bg-amber-700 text-white rounded-[14px] flex items-center justify-between px-5 shadow-primary transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-amber-900 focus-visible:outline-none active:scale-[0.99]"
          >
            <span className="flex items-center gap-2">
              <span className="inline-flex items-center justify-center bg-white/20 rounded-lg min-w-[26px] h-[26px] font-mono text-sm font-bold px-1">
                {totalItems}
              </span>
              <span className="font-medium text-sm">
                Koszyk · {totalItems === 1 ? '1 produkt' : `${totalItems} produktów`}
              </span>
            </span>
            <span className="flex items-center gap-1.5 font-mono font-semibold text-sm">
              {formatPrice(totalPrice)}
              <span className="font-sans font-medium">→</span>
            </span>
          </button>
        </div>
      )}
    </div>
  );
}
