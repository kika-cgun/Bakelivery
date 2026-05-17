import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
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

  const [form, setForm] = useState<FormValues>({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);

  function setField<K extends keyof FormValues>(key: K) {
    return (value: string) => {
      setForm((prev) => ({ ...prev, [key]: value }));
      if (errors[key]) {
        setErrors((prev) => ({ ...prev, [key]: undefined }));
      }
    };
  }

  function validate(): boolean {
    const newErrors: FormErrors = {};
    if (!form.name.trim()) newErrors.name = 'Imię jest wymagane';
    if (!form.email.trim()) {
      newErrors.email = 'Email jest wymagany';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      newErrors.email = 'Podaj poprawny adres e-mail';
    }
    if (!form.password) {
      newErrors.password = 'Hasło jest wymagane';
    } else if (form.password.length < 6) {
      newErrors.password = 'Hasło musi mieć co najmniej 6 znaków';
    }
    if (!form.confirmPassword) {
      newErrors.confirmPassword = 'Powtórz hasło';
    } else if (form.password !== form.confirmPassword) {
      newErrors.confirmPassword = 'Hasła nie są identyczne';
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitError(null);
    if (!validate()) return;

    try {
      const data = await register({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
      });
      setAuth(data.token, data.email, data.role);
      navigate('/', { replace: true });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Nie udało się utworzyć konta. Spróbuj ponownie.';
      setSubmitError(msg);
    }
  }

  return (
    <div className="min-h-dvh bg-[#FFF7ED] flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        {/* Brand */}
        <div className="text-center mb-8">
          <h1 className="font-display text-4xl text-amber-900 mb-2">Bakelivery</h1>
          <p className="text-sm text-[#64748B]">Utwórz nowe konto</p>
        </div>

        <div className="bg-white rounded-[14px] border border-[#FCEAE1] shadow-[0_1px_6px_rgba(0,0,0,.07)] p-6">
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
                  Tworzę konto…
                </>
              ) : (
                'Zarejestruj się'
              )}
            </button>
          </form>
        </div>

        <p className="text-center text-sm text-[#64748B] mt-5">
          Masz już konto?{' '}
          <Link
            to="/login"
            className="text-amber-700 font-medium hover:text-amber-900 underline underline-offset-2 focus-visible:ring-2 focus-visible:ring-amber-600 focus-visible:outline-none rounded"
          >
            Zaloguj się
          </Link>
        </p>
      </div>
    </div>
  );
}
