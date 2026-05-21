import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ShoppingCart, Search, Wheat, ArrowRight } from 'lucide-react';
import { useProducts, useCategories } from '@ui/api/catalog';
import { cn, formatPrice } from '@ui/lib/utils';
import { useCartStore } from '@/store/cart';
import type { Product } from '@ui/types';

// ─── Skeleton ────────────────────────────────────────────────────────────────

function ProductCardSkeleton() {
  return (
    <div className="rounded-2xl bg-[#FFFCF8] overflow-hidden animate-pulse shadow-card">
      <div className="h-36 bg-amber-100/50" />
      <div className="p-3 space-y-2">
        <div className="h-4 bg-amber-100/80 rounded w-3/4" />
        <div className="h-3 bg-amber-50 rounded w-1/2" />
        <div className="flex items-center justify-between mt-3">
          <div className="h-4 bg-amber-100/70 rounded w-1/4" />
          <div className="h-7 w-14 bg-amber-100/60 rounded-full" />
        </div>
      </div>
    </div>
  );
}

// ─── Product Card ─────────────────────────────────────────────────────────────

const CARD_GRADIENTS = [
  'from-amber-50 to-yellow-100',
  'from-orange-50 to-amber-100',
  'from-yellow-50 to-orange-50',
  'from-amber-100 to-yellow-50',
  'from-orange-50 to-amber-50',
  'from-yellow-100 to-orange-50',
];

function getGradient(id: string): string {
  return CARD_GRADIENTS[id.charCodeAt(id.length - 1) % CARD_GRADIENTS.length];
}

interface ProductCardProps {
  product: Product;
  onAdd: (product: Product) => void;
}

function ProductCard({ product, onAdd }: ProductCardProps) {
  return (
    <Link
      to={`/products/${product.id}`}
      className={cn(
        'group rounded-2xl bg-[#FFFCF8] overflow-hidden flex flex-col',
        'shadow-card hover:shadow-lifted hover:-translate-y-0.5',
        'transition-all duration-200',
        'focus-visible:ring-2 focus-visible:ring-amber-700 focus-visible:outline-none',
      )}
    >
      {/* Image / gradient placeholder */}
      <div className={cn('h-36 bg-gradient-to-br flex items-center justify-center relative', getGradient(product.id))}>
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="w-full h-full object-cover"
          />
        ) : (
          <Wheat size={40} strokeWidth={0.9} className="text-amber-300/60" />
        )}
        {!product.available && (
          <div className="absolute inset-0 bg-white/70 flex items-center justify-center">
            <span className="text-xs font-semibold text-amber-900/70 bg-white/90 px-3 py-1 rounded-full border border-amber-200/60">
              Niedostępny
            </span>
          </div>
        )}
      </div>

      {/* Info */}
      <div className="p-3 flex flex-col flex-1">
        <p className="font-display text-sm leading-snug text-amber-950 line-clamp-2">{product.name}</p>
        {product.bakeryName && (
          <p className="text-xs text-amber-700/50 mt-0.5 truncate">{product.bakeryName}</p>
        )}

        <div className="flex items-center justify-between mt-auto pt-2 gap-2">
          <span className="font-mono text-sm font-bold text-amber-800">
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
              'flex-none h-7 px-3 rounded-full text-[11px] font-bold transition-all duration-150',
              'focus-visible:ring-2 focus-visible:ring-amber-700 focus-visible:outline-none',
              product.available
                ? 'bg-amber-700 text-white hover:bg-amber-800 active:scale-95'
                : 'bg-slate-100 text-slate-300 cursor-not-allowed',
            )}
          >
            {product.available ? 'Dodaj' : '—'}
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
    <header className="fixed top-0 inset-x-0 z-40 bg-white border-b border-[#EDD9B8]">
      {/* Amber accent stripe */}
      <div className="absolute top-0 inset-x-0 h-0.5 bg-gradient-to-r from-amber-900 via-amber-600 to-yellow-500" />
      <div className="h-14 flex items-center px-4 gap-2">
        <span className="font-display text-[1.35rem] text-amber-950 flex-1 tracking-tight">Bakelivery</span>
        <button
          type="button"
          aria-label="Szukaj produktów"
          className="w-9 h-9 flex items-center justify-center rounded-xl text-amber-700/60 hover:text-amber-700 hover:bg-amber-50 transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-amber-700 focus-visible:outline-none"
        >
          <Search size={18} strokeWidth={1.75} />
        </button>
        <button
          type="button"
          aria-label={`Koszyk — ${totalItems} produktów`}
          onClick={() => navigate('/cart')}
          className="relative w-9 h-9 flex items-center justify-center rounded-xl text-amber-700/60 hover:text-amber-700 hover:bg-amber-50 transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-amber-700 focus-visible:outline-none"
        >
          <ShoppingCart size={18} strokeWidth={1.75} />
          {totalItems > 0 && (
            <span className="absolute -top-0.5 -right-0.5 min-w-[17px] h-[17px] bg-amber-700 text-white text-[9px] font-bold font-mono rounded-full flex items-center justify-center px-1 leading-none">
              {totalItems > 99 ? '99+' : totalItems}
            </span>
          )}
        </button>
      </div>
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

  return (
    <div className="min-h-dvh bg-[#FDF6EC]">
      <Navbar totalItems={totalItems} />

      <main className="pt-14">
        {/* Hero */}
        <section className="relative px-4 pt-8 pb-5 overflow-hidden">
          <div className="absolute right-0 top-1/2 -translate-y-1/2 pointer-events-none" aria-hidden="true">
            <Wheat size={160} strokeWidth={0.5} className="text-amber-400/12" />
          </div>

          <p className="font-mono text-[10px] text-amber-700 uppercase tracking-[0.22em] mb-3 flex items-center gap-2">
            <span className="inline-block w-5 h-px bg-amber-400/60" />
            30–45 min · dostawa
          </p>
          <h1 className="font-display text-[2.5rem] sm:text-5xl text-amber-950 leading-[1.05] mb-3">
            Świeże<br />pieczywo
          </h1>
          <p className="text-sm text-amber-800/55 leading-relaxed">
            Min.{' '}
            <span className="font-mono font-semibold text-amber-800">30,00 zł</span>
            {' · '}
            Darmowa dostawa od{' '}
            <span className="font-mono font-semibold text-amber-800">60,00 zł</span>
          </p>
        </section>

        {/* Divider */}
        <div className="mx-4 h-px bg-gradient-to-r from-amber-200/80 via-amber-200/40 to-transparent mb-5" />

        {/* Category chips */}
        <section className="px-4 mb-5">
          <div className="flex gap-2 overflow-x-auto scrollbar-none pb-1">
            <button
              type="button"
              onClick={() => setActiveCategoryId(undefined)}
              className={cn(
                'flex-none px-4 h-8 rounded-full text-xs font-bold transition-all duration-150 border',
                'focus-visible:ring-2 focus-visible:ring-amber-700 focus-visible:outline-none',
                activeCategoryId === undefined
                  ? 'bg-amber-700 text-white border-amber-700'
                  : 'bg-white text-amber-800 border-[#EDD9B8] hover:border-amber-400',
              )}
            >
              Wszystkie
            </button>

            {catsLoading
              ? Array.from({ length: 4 }).map((_, i) => (
                  <div key={i} className="flex-none h-8 w-24 rounded-full bg-amber-100/60 animate-pulse" />
                ))
              : categories.map((cat) => (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => setActiveCategoryId(cat.id)}
                    className={cn(
                      'flex-none px-4 h-8 rounded-full text-xs font-bold transition-all duration-150 border whitespace-nowrap',
                      'focus-visible:ring-2 focus-visible:ring-amber-700 focus-visible:outline-none',
                      activeCategoryId === cat.id
                        ? 'bg-amber-700 text-white border-amber-700'
                        : 'bg-white text-amber-800 border-[#EDD9B8] hover:border-amber-400',
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
            {prodsLoading
              ? Array.from({ length: 6 }).map((_, i) => <ProductCardSkeleton key={i} />)
              : products.map((product) => (
                  <ProductCard key={product.id} product={product} onAdd={addItem} />
                ))}
          </div>

          {!prodsLoading && products.length === 0 && (
            <div className="flex flex-col items-center py-16 text-center">
              <Wheat size={48} strokeWidth={0.9} className="text-amber-300 mb-4" />
              <p className="font-display text-lg text-amber-950 mb-1">Brak produktów</p>
              <p className="text-sm text-amber-800/55">Spróbuj wybrać inną kategorię</p>
            </div>
          )}
        </section>
      </main>

      {/* Cart bottom bar */}
      {totalItems > 0 && (
        <div className="fixed bottom-0 inset-x-0 z-50 p-3 bg-[#FDF6EC]/90 backdrop-blur-sm">
          <button
            type="button"
            onClick={() => navigate('/cart')}
            className="w-full h-14 bg-amber-700 hover:bg-amber-800 text-white rounded-2xl flex items-center justify-between px-5 shadow-[0_4px_14px_rgba(180,83,9,.40)] transition-all duration-150 focus-visible:ring-2 focus-visible:ring-amber-900 focus-visible:outline-none active:scale-[0.99]"
          >
            <span className="flex items-center gap-2.5">
              <span className="inline-flex items-center justify-center bg-white/20 rounded-lg min-w-[26px] h-[26px] font-mono text-sm font-bold px-1.5">
                {totalItems}
              </span>
              <span className="font-semibold text-sm">
                {totalItems === 1 ? '1 produkt' : `${totalItems} produktów`}
              </span>
            </span>
            <span className="flex items-center gap-2 font-mono font-bold text-sm">
              {formatPrice(totalPrice)}
              <ArrowRight size={16} strokeWidth={2.5} />
            </span>
          </button>
        </div>
      )}
    </div>
  );
}
