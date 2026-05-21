import { useState } from 'react';
import { Plus, Pencil, Trash2, Loader2, Package } from 'lucide-react';
import { OpsLayout } from '@/components/layout/OpsLayout';
import {
  useProducts,
  useCategories,
  useCreateProduct,
  useUpdateProduct,
  useDeleteProduct,
} from '@ui/api/catalog';
import { cn, formatPrice } from '@ui/lib/utils';
import type { Product, Category } from '@ui/types';

// ─── Product form modal ───────────────────────────────────────────────────────

interface ProductFormData {
  name: string;
  description: string;
  price: string; // string for input, convert to grosze on save
  categoryId: string;
  available: boolean;
}

const EMPTY_FORM: ProductFormData = {
  name: '',
  description: '',
  price: '',
  categoryId: '',
  available: true,
};

interface ProductModalProps {
  product: Product | null; // null = create
  categories: Category[];
  onClose: () => void;
}

function ProductModal({ product, categories, onClose }: ProductModalProps) {
  const createProduct = useCreateProduct();
  const updateProduct = useUpdateProduct();

  const isPending = createProduct.isPending || updateProduct.isPending;

  const [form, setForm] = useState<ProductFormData>(() =>
    product
      ? {
          name: product.name,
          description: product.description,
          price: (product.price / 100).toFixed(2),
          categoryId: product.categoryId,
          available: product.available,
        }
      : EMPTY_FORM,
  );

  const [errors, setErrors] = useState<Partial<Record<keyof ProductFormData, string>>>({});

  function validate(): boolean {
    const e: typeof errors = {};
    if (!form.name.trim()) e.name = 'Pole wymagane';
    if (!form.price || isNaN(Number(form.price)) || Number(form.price) <= 0)
      e.price = 'Podaj poprawną cenę';
    if (!form.categoryId) e.categoryId = 'Wybierz kategorię';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!validate()) return;

    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      price: Math.round(Number(form.price) * 100),
      categoryId: form.categoryId,
      available: form.available,
      imageUrl: product?.imageUrl,
      weight: product?.weight,
      bakeryName: product?.bakeryName,
    };

    if (product) {
      updateProduct.mutate(
        { id: product.id, data: payload },
        { onSuccess: onClose },
      );
    } else {
      createProduct.mutate(payload as Omit<Product, 'id'>, { onSuccess: onClose });
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md max-h-[90dvh] overflow-y-auto">
        <div className="px-5 py-4 border-b border-slate-100 flex items-center justify-between">
          <h2 className="font-semibold text-slate-800">
            {product ? 'Edytuj produkt' : 'Dodaj produkt'}
          </h2>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 transition-colors text-xl leading-none"
            aria-label="Zamknij"
          >
            ×
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4" noValidate>
          {/* Name */}
          <div className="space-y-1">
            <label htmlFor="prod-name" className="block text-sm font-medium text-slate-700">
              Nazwa <span className="text-red-500">*</span>
            </label>
            <input
              id="prod-name"
              type="text"
              required
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              className={cn(
                'w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 transition-colors',
                errors.name
                  ? 'border-red-300 focus:border-red-400 focus:ring-red-300/40'
                  : 'border-slate-200 focus:border-amber-400 focus:ring-amber-300/40',
              )}
            />
            {errors.name && <p className="text-xs text-red-500">{errors.name}</p>}
          </div>

          {/* Description */}
          <div className="space-y-1">
            <label htmlFor="prod-desc" className="block text-sm font-medium text-slate-700">
              Opis
            </label>
            <textarea
              id="prod-desc"
              rows={3}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              className="w-full border border-slate-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-amber-400 focus:ring-1 focus:ring-amber-300/40 resize-none transition-colors"
            />
          </div>

          {/* Price */}
          <div className="space-y-1">
            <label htmlFor="prod-price" className="block text-sm font-medium text-slate-700">
              Cena (PLN) <span className="text-red-500">*</span>
            </label>
            <input
              id="prod-price"
              type="number"
              min="0.01"
              step="0.01"
              required
              value={form.price}
              onChange={(e) => setForm((f) => ({ ...f, price: e.target.value }))}
              className={cn(
                'w-full border rounded-lg px-3 py-2 text-sm font-mono focus:outline-none focus:ring-1 transition-colors',
                errors.price
                  ? 'border-red-300 focus:border-red-400 focus:ring-red-300/40'
                  : 'border-slate-200 focus:border-amber-400 focus:ring-amber-300/40',
              )}
            />
            {errors.price && <p className="text-xs text-red-500">{errors.price}</p>}
          </div>

          {/* Category */}
          <div className="space-y-1">
            <label htmlFor="prod-cat" className="block text-sm font-medium text-slate-700">
              Kategoria <span className="text-red-500">*</span>
            </label>
            <select
              id="prod-cat"
              required
              value={form.categoryId}
              onChange={(e) => setForm((f) => ({ ...f, categoryId: e.target.value }))}
              className={cn(
                'w-full border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-1 transition-colors bg-white',
                errors.categoryId
                  ? 'border-red-300 focus:border-red-400 focus:ring-red-300/40'
                  : 'border-slate-200 focus:border-amber-400 focus:ring-amber-300/40',
              )}
            >
              <option value="">— wybierz —</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            {errors.categoryId && <p className="text-xs text-red-500">{errors.categoryId}</p>}
          </div>

          {/* Availability toggle */}
          <div className="flex items-center justify-between">
            <label htmlFor="prod-available" className="text-sm font-medium text-slate-700">
              Dostępność
            </label>
            <button
              id="prod-available"
              type="button"
              role="switch"
              aria-checked={form.available}
              onClick={() => setForm((f) => ({ ...f, available: !f.available }))}
              className={cn(
                'relative inline-flex h-5 w-9 rounded-full transition-colors',
                form.available ? 'bg-amber-500' : 'bg-slate-200',
              )}
            >
              <span
                className={cn(
                  'inline-block h-4 w-4 rounded-full bg-white shadow transform transition-transform mt-0.5',
                  form.available ? 'translate-x-4 ml-0.5' : 'translate-x-0.5',
                )}
              />
            </button>
          </div>

          {/* Actions */}
          <div className="flex gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2 text-sm text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors"
            >
              Anuluj
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="flex-1 flex items-center justify-center gap-1.5 py-2 text-sm text-white font-medium bg-amber-600 hover:bg-amber-500 disabled:opacity-60 disabled:cursor-not-allowed rounded-lg transition-colors"
            >
              {isPending && <Loader2 size={14} className="animate-spin" />}
              {isPending ? 'Zapisywanie…' : 'Zapisz'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Confirm dialog ───────────────────────────────────────────────────────────

interface ConfirmDeleteProps {
  product: Product;
  onConfirm: () => void;
  onCancel: () => void;
  isPending: boolean;
}

function ConfirmDelete({ product, onConfirm, onCancel, isPending }: ConfirmDeleteProps) {
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40"
      onClick={(e) => { if (e.target === e.currentTarget) onCancel(); }}
    >
      <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-5">
        <h2 className="font-semibold text-slate-800 mb-2">Usuń produkt</h2>
        <p className="text-sm text-slate-500 mb-5">
          Czy na pewno usunąć produkt{' '}
          <span className="font-medium text-slate-700">{product.name}</span>? Tej operacji nie
          można cofnąć.
        </p>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 py-2 text-sm text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors"
          >
            Anuluj
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isPending}
            className="flex-1 flex items-center justify-center gap-1.5 py-2 text-sm text-white bg-red-500 hover:bg-red-600 disabled:opacity-60 disabled:cursor-not-allowed rounded-lg transition-colors"
          >
            {isPending && <Loader2 size={14} className="animate-spin" />}
            Usuń
          </button>
        </div>
      </div>
    </div>
  );
}

// ─── Page ────────────────────────────────────────────────────────────────────

export default function CatalogPage() {
  const [filterCategoryId, setFilterCategoryId] = useState<string | undefined>(undefined);
  const [modalProduct, setModalProduct] = useState<Product | null | 'new'>(null);
  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null);

  const { data: products, isLoading: productsLoading } = useProducts(filterCategoryId);
  const { data: categories = [] } = useCategories();
  const deleteProduct = useDeleteProduct();
  const updateProduct = useUpdateProduct();

  function handleDelete() {
    if (!deleteTarget) return;
    deleteProduct.mutate(deleteTarget.id, { onSuccess: () => setDeleteTarget(null) });
  }

  function handleToggleAvailable(product: Product) {
    updateProduct.mutate({ id: product.id, data: { available: !product.available } });
  }

  return (
    <OpsLayout
      title="Katalog produktów"
      actions={
        <button
          type="button"
          onClick={() => setModalProduct('new')}
          className="flex items-center gap-1.5 bg-amber-600 hover:bg-amber-500 text-white text-sm font-medium px-3 py-1.5 rounded-lg transition-colors"
        >
          <Plus size={15} />
          Dodaj produkt
        </button>
      }
    >
      {/* Category filter chips */}
      <div className="flex gap-1.5 mb-5 flex-wrap">
        <button
          type="button"
          onClick={() => setFilterCategoryId(undefined)}
          className={cn(
            'px-3 py-1 text-sm rounded-full border transition-colors',
            !filterCategoryId
              ? 'bg-amber-500 text-white border-amber-500'
              : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50',
          )}
        >
          Wszystkie
        </button>
        {categories.map((cat) => (
          <button
            key={cat.id}
            type="button"
            onClick={() => setFilterCategoryId(cat.id)}
            className={cn(
              'px-3 py-1 text-sm rounded-full border transition-colors',
              filterCategoryId === cat.id
                ? 'bg-amber-500 text-white border-amber-500'
                : 'bg-white text-slate-600 border-slate-200 hover:bg-slate-50',
            )}
          >
            {cat.name}
          </button>
        ))}
      </div>

      {/* Products table */}
      {productsLoading ? (
        <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm overflow-hidden">
          {[1, 2, 3, 4].map((n) => (
            <div key={n} className="flex items-center gap-4 px-4 py-3 border-b border-slate-50 last:border-0 animate-pulse">
              <div className="h-4 w-40 bg-slate-100 rounded" />
              <div className="h-4 w-20 bg-slate-100 rounded" />
              <div className="ml-auto h-4 w-16 bg-slate-100 rounded" />
            </div>
          ))}
        </div>
      ) : !products?.length ? (
        <div className="flex flex-col items-center justify-center py-20 text-slate-400">
          <Package size={36} className="mb-3 opacity-30" />
          <p className="text-sm">Brak produktów w tej kategorii</p>
        </div>
      ) : (
        <div className="bg-white border border-slate-100 rounded-[10px] shadow-sm overflow-hidden">
          {/* Desktop header */}
          <div className="hidden sm:grid grid-cols-[1fr_auto_auto_auto_auto] gap-4 px-4 py-2 bg-slate-50 border-b border-slate-100 text-xs font-medium text-slate-500 uppercase tracking-wide">
            <span>Nazwa</span>
            <span>Kategoria</span>
            <span>Cena</span>
            <span>Dostępność</span>
            <span>Akcje</span>
          </div>

          {products.map((product) => {
            const categoryName =
              categories.find((c) => c.id === product.categoryId)?.name ?? '—';
            return (
              <div
                key={product.id}
                className="grid grid-cols-1 sm:grid-cols-[1fr_auto_auto_auto_auto] gap-2 sm:gap-4 items-center px-4 py-3 border-b border-slate-50 last:border-0 hover:bg-slate-50/50 transition-colors"
              >
                {/* Name */}
                <div>
                  <p className="text-sm font-medium text-slate-800">{product.name}</p>
                  {product.description && (
                    <p className="text-xs text-slate-400 truncate max-w-xs">{product.description}</p>
                  )}
                </div>

                {/* Category */}
                <span className="text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded-full">
                  {categoryName}
                </span>

                {/* Price */}
                <span className="font-mono text-sm font-semibold text-slate-700">
                  {formatPrice(product.price)}
                </span>

                {/* Availability toggle */}
                <button
                  type="button"
                  role="switch"
                  aria-checked={product.available}
                  aria-label={product.available ? 'Dostępny — kliknij aby wyłączyć' : 'Niedostępny — kliknij aby włączyć'}
                  onClick={() => handleToggleAvailable(product)}
                  className={cn(
                    'relative inline-flex h-5 w-9 rounded-full transition-colors',
                    product.available ? 'bg-amber-500' : 'bg-slate-200',
                  )}
                >
                  <span
                    className={cn(
                      'inline-block h-4 w-4 rounded-full bg-white shadow transform transition-transform mt-0.5',
                      product.available ? 'translate-x-4 ml-0.5' : 'translate-x-0.5',
                    )}
                  />
                </button>

                {/* Actions */}
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={() => setModalProduct(product)}
                    aria-label={`Edytuj ${product.name}`}
                    className="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded-md transition-colors"
                  >
                    <Pencil size={14} />
                  </button>
                  <button
                    type="button"
                    onClick={() => setDeleteTarget(product)}
                    aria-label={`Usuń ${product.name}`}
                    className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-md transition-colors"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Product modal */}
      {modalProduct !== null && (
        <ProductModal
          product={modalProduct === 'new' ? null : modalProduct}
          categories={categories}
          onClose={() => setModalProduct(null)}
        />
      )}

      {/* Delete confirm */}
      {deleteTarget && (
        <ConfirmDelete
          product={deleteTarget}
          onConfirm={handleDelete}
          onCancel={() => setDeleteTarget(null)}
          isPending={deleteProduct.isPending}
        />
      )}
    </OpsLayout>
  );
}
