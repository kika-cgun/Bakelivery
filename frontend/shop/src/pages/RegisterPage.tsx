import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Loader2, Wheat } from 'lucide-react';
import { useRegister } from '@ui/api/auth';
import { cn } from '@ui/lib/utils';
import { useAuthStore } from '@/store/auth';

// ─── Field ────────────────────────────────────────────────────────────────────

interface FieldProps {
  id: string;
  label: string;
  type: string;
  value: string;
  onChange: (v: string) => void;
  error?: string;
  placeholder?: string;
  autoComplete?: string;
}

function Field({ id, label, type, value, onChange, error, placeholder, autoComplete }: FieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-xs font-bold text-amber-950/70 uppercase tracking-wider">
        {label}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        autoComplete={autoComplete}
        className={cn(
          'w-full px-4 py-3 text-sm bg-[#FFFCF8] border rounded-xl text-amber-950 placeholder:text-amber-800/30 transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-amber-700 focus:border-transparent',
          error ? 'border-red-300' : 'border-[#EDD9B8] hover:border-amber-400',
        )}
      />
      {error && <p className="text-xs text-red-600 mt-0.5">{error}</p>}
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

interface FormValues {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}

interface FormErrors {
  name?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
}

export default function RegisterPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const { mutateAsync: register, isPending } = useRegister();

  const [form, setForm] = useState<FormValues>({ name: '', email: '', password: '', confirmPassword: '' });
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);

  function setField<K extends keyof FormValues>(key: K) {
    return (value: string) => {
      setForm((prev) => ({ ...prev, [key]: value }));
      if (errors[key]) setErrors((prev) => ({ ...prev, [key]: undefined }));
    };
  }

  function validate(): boolean {
    const newErrors: FormErrors = {};
    if (!form.name.trim()) newErrors.name = 'Imię jest wymagane';
    if (!form.email.trim()) newErrors.email = 'Email jest wymagany';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) newErrors.email = 'Podaj poprawny adres e-mail';
    if (!form.password) newErrors.password = 'Hasło jest wymagane';
    else if (form.password.length < 6) newErrors.password = 'Hasło musi mieć co najmniej 6 znaków';
    if (!form.confirmPassword) newErrors.confirmPassword = 'Powtórz hasło';
    else if (form.password !== form.confirmPassword) newErrors.confirmPassword = 'Hasła nie są identyczne';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError(null);
    if (!validate()) return;

    try {
      const data = await register({ name: form.name.trim(), email: form.email.trim(), password: form.password });
      setAuth(data.token, data.email, data.role);
      navigate('/', { replace: true });
    } catch (err: unknown) {
      setSubmitError(err instanceof Error ? err.message : 'Nie udało się utworzyć konta. Spróbuj ponownie.');
    }
  }

  return (
    <div className="min-h-dvh bg-[#FDF6EC] flex flex-col items-center justify-center px-4 py-12">
      <div className="w-full max-w-[360px]">

        {/* Brand mark */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2.5 mb-2">
            <Wheat size={22} strokeWidth={1.4} className="text-amber-600" />
            <h1 className="font-display text-[2.25rem] text-amber-950 leading-none">Bakelivery</h1>
          </div>
          <p className="text-sm text-amber-800/55 font-medium">Utwórz nowe konto</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl overflow-hidden shadow-login">
          <div className="h-1 bg-gradient-to-r from-amber-900 via-amber-600 to-amber-400" />

          <div className="p-6">
            <form onSubmit={handleSubmit} noValidate className="space-y-4">
              <Field
                id="name"
                label="Imię"
                type="text"
                value={form.name}
                onChange={setField('name')}
                error={errors.name}
                placeholder="Jan"
                autoComplete="given-name"
              />
              <Field
                id="email"
                label="Email"
                type="email"
                value={form.email}
                onChange={setField('email')}
                error={errors.email}
                placeholder="jan@example.com"
                autoComplete="email"
              />
              <Field
                id="password"
                label="Hasło"
                type="password"
                value={form.password}
                onChange={setField('password')}
                error={errors.password}
                placeholder="Min. 6 znaków"
                autoComplete="new-password"
              />
              <Field
                id="confirmPassword"
                label="Powtórz hasło"
                type="password"
                value={form.confirmPassword}
                onChange={setField('confirmPassword')}
                error={errors.confirmPassword}
                placeholder="••••••••"
                autoComplete="new-password"
              />

              {submitError && (
                <div className="px-4 py-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-600">
                  {submitError}
                </div>
              )}

              <button
                type="submit"
                disabled={isPending}
                className={cn(
                  'w-full h-12 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all duration-150 focus-visible:ring-2 focus-visible:ring-amber-900 focus-visible:outline-none mt-2',
                  isPending
                    ? 'bg-amber-400 text-white cursor-not-allowed'
                    : 'bg-amber-700 hover:bg-amber-800 text-white shadow-[0_4px_14px_rgba(180,83,9,.30)] active:scale-[0.99]',
                )}
              >
                {isPending ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    Tworzę konto…
                  </>
                ) : (
                  'Zarejestruj się'
                )}
              </button>
            </form>
          </div>
        </div>

        <p className="text-center text-sm text-amber-800/55 mt-5">
          Masz już konto?{' '}
          <Link
            to="/login"
            className="text-amber-700 font-bold hover:text-amber-900 underline underline-offset-2 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded"
          >
            Zaloguj się
          </Link>
        </p>
      </div>
    </div>
  );
}
