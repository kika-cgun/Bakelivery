import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
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
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-slate-700">
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
          'w-full px-3.5 py-3 text-sm bg-white border rounded-[8px] text-slate-800 placeholder:text-slate-400 transition-colors duration-150 focus:outline-none focus:ring-2 focus:ring-amber-600 focus:border-transparent',
          error ? 'border-red-400' : 'border-[#FCEAE1] hover:border-amber-300',
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

      if (data.role !== 'CUSTOMER') {
        setWrongRole(true);
        return;
      }

      navigate('/', { replace: true });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Nieprawidłowy email lub hasło.';
      setSubmitError(msg);
    }
  }

  return (
    <div className="min-h-dvh bg-[#FFF7ED] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="text-center mb-8">
          <h1 className="font-display text-4xl text-amber-900 mb-2">Bakelivery</h1>
          <p className="text-sm text-[#64748B]">Zaloguj się do swojego konta</p>
        </div>

        <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] p-6">
          {wrongRole ? (
            <div className="text-center py-2">
              <p className="text-sm text-slate-700 mb-4">
                Ten panel jest dla klientów sklepu.<br />
                Przejdź do panelu ops, aby zarządzać zamówieniami.
              </p>
              <button
                type="button"
                onClick={() => { setWrongRole(false); setEmail(''); setPassword(''); }}
                className="text-sm text-amber-600 underline underline-offset-2 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded"
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
                <div className="px-3.5 py-2.5 bg-red-50 border border-red-200 rounded-[8px] text-xs text-red-600">
                  {submitError}
                </div>
              )}

              <button
                type="submit"
                disabled={isPending}
                className={cn(
                  'w-full h-12 rounded-[8px] font-semibold text-sm flex items-center justify-center gap-2 transition-all duration-150 focus-visible:ring-2 focus-visible:ring-amber-900 focus-visible:outline-none mt-2',
                  isPending
                    ? 'bg-amber-400 text-white cursor-not-allowed'
                    : 'bg-amber-600 hover:bg-amber-700 text-white shadow-primary active:scale-[0.99]',
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

        <p className="text-center text-sm text-[#64748B] mt-5">
          Nie masz konta?{' '}
          <Link
            to="/register"
            className="text-amber-700 font-medium hover:text-amber-900 underline underline-offset-2 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded"
          >
            Zarejestruj się
          </Link>
        </p>
      </div>
    </div>
  );
}
