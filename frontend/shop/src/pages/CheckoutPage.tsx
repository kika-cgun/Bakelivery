import { useEffect, useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Loader2, ArrowLeft, MapPin } from 'lucide-react';
import { useCreateOrder, cartToOrderItems } from '@ui/api/orders';
import { cn, formatPrice } from '@ui/lib/utils';
import { useCartStore } from '@/store/cart';
import { useAuthStore } from '@/store/auth';
import type { DeliveryAddress } from '@ui/types';

const DELIVERY_THRESHOLD = 6000;
const DELIVERY_FEE = 899;

// ─── Field ────────────────────────────────────────────────────────────────────

interface FieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (v: string) => void;
  error?: string;
  placeholder?: string;
  required?: boolean;
  as?: 'input' | 'textarea';
}

function Field({ id, label, value, onChange, error, placeholder, required = false, as: As = 'input' }: FieldProps) {
  const base =
    'w-full px-3.5 py-3 text-sm bg-white border rounded-[8px] text-slate-800 placeholder:text-slate-400 transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-amber-600 focus:border-transparent';
  const borderClass = error ? 'border-red-400' : 'border-[#FCEAE1] hover:border-amber-300';

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
        {label}
        {required && <span className="text-red-500 ml-0.5">*</span>}
      </label>
      {As === 'textarea' ? (
        <textarea
          id={id}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          rows={3}
          className={cn(base, borderClass, 'resize-none')}
        />
      ) : (
        <input
          id={id}
          type="text"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          className={cn(base, borderClass)}
        />
      )}
      {error && <p className="text-xs text-red-600 mt-0.5">{error}</p>}
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

interface FormValues {
  street: string;
  houseNumber: string;
  city: string;
  postalCode: string;
  notes: string;
}

interface FormErrors {
  street?: string;
  houseNumber?: string;
  city?: string;
  postalCode?: string;
}

export default function CheckoutPage() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  const items = useCartStore((s) => s.items);
  const totalPrice = useCartStore((s) => s.totalPrice());
  const clearCart = useCartStore((s) => s.clear);

  const { mutateAsync: createOrder, isPending } = useCreateOrder();

  const [form, setForm] = useState<FormValues>({
    street: '',
    houseNumber: '',
    city: '',
    postalCode: '',
    notes: '',
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Redirect if not authenticated
  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  const deliveryFee = totalPrice >= DELIVERY_THRESHOLD ? 0 : DELIVERY_FEE;
  const grandTotal = totalPrice + deliveryFee;

  function validate(): boolean {
    const newErrors: FormErrors = {};
    if (!form.street.trim()) newErrors.street = 'Ulica jest wymagana';
    if (!form.houseNumber.trim()) newErrors.houseNumber = 'Numer domu jest wymagany';
    if (!form.city.trim()) newErrors.city = 'Miasto jest wymagane';
    if (!form.postalCode.trim()) {
      newErrors.postalCode = 'Kod pocztowy jest wymagany';
    } else if (!/^\d{2}-\d{3}$/.test(form.postalCode)) {
      newErrors.postalCode = 'Podaj kod w formacie 00-000';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError(null);
    if (!validate()) return;

    const address: DeliveryAddress = {
      street: `${form.street} ${form.houseNumber}`.trim(),
      city: form.city,
      postalCode: form.postalCode,
      notes: form.notes || undefined,
    };

    try {
      const order = await createOrder({
        items: cartToOrderItems(items),
        address,
      });
      clearCart();
      navigate(`/orders/${order.id}/track`, { replace: true });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Wystąpił błąd. Spróbuj ponownie.';
      setSubmitError(msg);
    }
  }

  function setField<K extends keyof FormValues>(key: K) {
    return (value: string) => {
      setForm((prev) => ({ ...prev, [key]: value }));
      if (errors[key as keyof FormErrors]) {
        setErrors((prev) => ({ ...prev, [key]: undefined }));
      }
    };
  }

  return (
    <div className="min-h-dvh bg-[#FFF7ED]">
      {/* Header */}
      <header className="sticky top-0 z-30 h-14 flex items-center px-4 bg-[#FFF7ED]/90 backdrop-blur-sm border-b border-[#FCEAE1]">
        <Link
          to="/cart"
          className="flex items-center gap-1.5 text-sm font-medium text-amber-700 hover:text-amber-900 transition-colors duration-150 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded-lg h-10 px-2 -ml-2"
        >
          <ArrowLeft size={18} />
          <span>Koszyk</span>
        </Link>
        <h1 className="font-display text-lg text-amber-900 mx-auto">Zamówienie</h1>
        <div className="w-16" />
      </header>

      <main className="px-4 pt-5 pb-40 max-w-lg mx-auto">
        <form id="checkout-form" onSubmit={handleSubmit} noValidate>
          {/* Delivery section */}
          <section className="mb-5">
            <div className="flex items-center gap-2 mb-3">
              <MapPin size={16} className="text-amber-600" />
              <h2 className="font-semibold text-slate-800 text-sm">Adres dostawy</h2>
            </div>
            <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] p-4 space-y-4">
              <div className="grid grid-cols-[1fr_auto] gap-3">
                <Field
                  id="street"
                  label="Ulica"
                  value={form.street}
                  onChange={setField('street')}
                  error={errors.street}
                  placeholder="ul. Piekarska"
                  required
                />
                <Field
                  id="houseNumber"
                  label="Nr domu"
                  value={form.houseNumber}
                  onChange={setField('houseNumber')}
                  error={errors.houseNumber}
                  placeholder="12A"
                  required
                />
              </div>
              <div className="grid grid-cols-[auto_1fr] gap-3">
                <div className="w-28">
                  <Field
                    id="postalCode"
                    label="Kod pocztowy"
                    value={form.postalCode}
                    onChange={(v) => {
                      // Auto-format: insert dash after 2 digits
                      const digits = v.replace(/\D/g, '').slice(0, 5);
                      const formatted = digits.length > 2 ? `${digits.slice(0, 2)}-${digits.slice(2)}` : digits;
                      setField('postalCode')(formatted);
                    }}
                    error={errors.postalCode}
                    placeholder="00-000"
                    required
                  />
                </div>
                <Field
                  id="city"
                  label="Miasto"
                  value={form.city}
                  onChange={setField('city')}
                  error={errors.city}
                  placeholder="Warszawa"
                  required
                />
              </div>
              <Field
                id="notes"
                label="Notatka dla kuriera"
                value={form.notes}
                onChange={setField('notes')}
                placeholder="Kod domofonu, piętro, inne informacje..."
                as="textarea"
              />
            </div>
          </section>

          {/* Order summary */}
          <section className="mb-5">
            <h2 className="font-semibold text-slate-800 text-sm mb-3">Podsumowanie</h2>
            <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] p-4">
              <div className="space-y-2 mb-3">
                {items.map((item) => (
                  <div key={item.product.id} className="flex justify-between text-sm">
                    <span className="text-slate-600 truncate max-w-[200px]">
                      {item.product.name}
                      <span className="text-[#64748B] ml-1">×{item.quantity}</span>
                    </span>
                    <span className="font-mono text-slate-800 ml-2 flex-none">
                      {formatPrice(item.product.price * item.quantity)}
                    </span>
                  </div>
                ))}
              </div>
              <div className="border-t border-[#FCEAE1] pt-2 space-y-1.5">
                <div className="flex justify-between text-sm text-slate-600">
                  <span>Produkty</span>
                  <span className="font-mono">{formatPrice(totalPrice)}</span>
                </div>
                <div className="flex justify-between text-sm text-slate-600">
                  <span>Dostawa</span>
                  <span className={cn('font-mono', deliveryFee === 0 ? 'text-emerald-600' : '')}>
                    {deliveryFee === 0 ? 'Darmowa' : formatPrice(deliveryFee)}
                  </span>
                </div>
                <div className="flex justify-between font-bold pt-1 border-t border-[#FCEAE1]">
                  <span className="text-slate-800">Łącznie</span>
                  <span className="font-mono text-amber-600 text-base">{formatPrice(grandTotal)}</span>
                </div>
              </div>
            </div>
          </section>

          {submitError && (
            <div className="mb-4 px-4 py-3 bg-red-50 border border-red-200 rounded-[8px] text-sm text-red-600">
              {submitError}
            </div>
          )}
        </form>
      </main>

      {/* Fixed CTA */}
      <div className="fixed bottom-0 inset-x-0 z-40 p-4 bg-[#FFF7ED]/90 backdrop-blur-sm border-t border-[#FCEAE1]">
        <button
          type="submit"
          form="checkout-form"
          disabled={isPending || items.length === 0}
          className={cn(
            'w-full h-14 rounded-[14px] font-semibold text-sm flex items-center justify-center gap-2 transition-all duration-150 focus-visible:ring-2 focus-visible:ring-blue-700 focus-visible:outline-none max-w-lg mx-auto',
            isPending || items.length === 0
              ? 'bg-slate-100 text-slate-400 cursor-not-allowed'
              : 'bg-amber-700 hover:bg-amber-800 text-white shadow-[0_4px_14px_rgba(180,83,9,.35)] active:scale-[0.99]',
          )}
        >
          {isPending ? (
            <>
              <Loader2 size={18} className="animate-spin" />
              Składam zamówienie…
            </>
          ) : (
            `Złóż zamówienie · ${formatPrice(grandTotal)}`
          )}
        </button>
      </div>
    </div>
  );
}
