import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Loader2, Wheat } from 'lucide-react';
import { useLogin } from '@ui/api/auth';
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

export default function LoginPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const { mutateAsync: login, isPending } = useLogin();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [wrongRole, setWrongRole] = useState(false);

  function validate(): boolean {
    const newErrors: typeof errors = {};
    if (!email.trim()) newErrors.email = 'Email jest wymagany';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) newErrors.email = 'Podaj poprawny adres e-mail';
    if (!password) newErrors.password = 'Hasło jest wymagane';
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError(null);
    setWrongRole(false);
    if (!validate()) return;

    try {
      const data = await login({ email, password });
      setAuth(data.token, data.email, data.role);
      if (data.role !== 'CUSTOMER') { setWrongRole(true); return; }
      navigate('/', { replace: true });
    } catch (err: unknown) {
      setSubmitError(err instanceof Error ? err.message : 'Nieprawidłowy email lub hasło.');
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
          <p className="text-sm text-amber-800/55 font-medium">Zaloguj się do swojego konta</p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl overflow-hidden shadow-login">
          <div className="h-1 bg-gradient-to-r from-amber-900 via-amber-600 to-amber-400" />

          <div className="p-6">
            {wrongRole ? (
              <div className="text-center py-2">
                <p className="text-sm text-amber-950/70 mb-4 leading-relaxed">
                  Ten panel jest dla klientów sklepu.<br />
                  Przejdź do panelu ops, aby zarządzać zamówieniami.
                </p>
                <button
                  type="button"
                  onClick={() => { setWrongRole(false); setEmail(''); setPassword(''); }}
                  className="text-sm text-amber-700 font-bold underline underline-offset-2 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded"
                >
                  Zaloguj się innym kontem
                </button>
              </div>
            ) : (
              <form onSubmit={handleSubmit} noValidate className="space-y-4">
                <Field
                  id="email"
                  label="Email"
                  type="email"
                  value={email}
                  onChange={(v) => { setEmail(v); if (errors.email) setErrors((p) => ({ ...p, email: undefined })); }}
                  error={errors.email}
                  placeholder="jan@example.com"
                  autoComplete="email"
                />
                <Field
                  id="password"
                  label="Hasło"
                  type="password"
                  value={password}
                  onChange={(v) => { setPassword(v); if (errors.password) setErrors((p) => ({ ...p, password: undefined })); }}
                  error={errors.password}
                  placeholder="••••••••"
                  autoComplete="current-password"
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
                      Logowanie…
                    </>
                  ) : (
                    'Zaloguj się'
                  )}
                </button>
              </form>
            )}
          </div>
        </div>

        <p className="text-center text-sm text-amber-800/55 mt-5">
          Nie masz konta?{' '}
          <Link
            to="/register"
            className="text-amber-700 font-bold hover:text-amber-900 underline underline-offset-2 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded"
          >
            Zarejestruj się
          </Link>
        </p>
      </div>
    </div>
  );
}
