import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useLogin } from '@ui/api/auth';
import { useAuthStore } from '@/store/auth';
import type { Role } from '@ui/types';

const OPS_ROLES: Role[] = ['BAKERY', 'DRIVER', 'DISPATCHER', 'ADMIN'];

function roleRedirect(role: Role): string {
  if (role === 'BAKERY') return '/bakery/orders';
  if (role === 'DRIVER') return '/driver/deliveries';
  if (role === 'DISPATCHER') return '/dispatcher/map';
  return '/';
}

export default function LoginPage() {
  const navigate = useNavigate();
  const { mutate: login, isPending } = useLogin();
  const setAuth = useAuthStore((s) => s.setAuth);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setErrorMsg(null);

    login(
      { email, password },
      {
        onSuccess(data) {
          if (!OPS_ROLES.includes(data.role)) {
            setErrorMsg(
              'To konto jest przeznaczone dla klientów. Skorzystaj z aplikacji sklepu Bakelivery.',
            );
            return;
          }
          setAuth(data.token, data.email, data.role);
          navigate(roleRedirect(data.role), { replace: true });
        },
        onError(err) {
          const msg =
            err instanceof Error ? err.message : 'Błąd logowania. Sprawdź dane i spróbuj ponownie.';
          setErrorMsg(msg);
        },
      },
    );
  }

  return (
    <div className="min-h-dvh flex items-center justify-center bg-slate-950 px-4">
      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="flex flex-col items-center mb-8 gap-2">
          <span className="w-12 h-12 rounded-xl bg-amber-600 flex items-center justify-center">
            <span className="font-display text-white text-2xl leading-none">B</span>
          </span>
          <h1 className="font-display text-2xl text-amber-400">Bakelivery</h1>
          <p className="text-slate-400 text-sm">Panel operacyjny</p>
        </div>

        {/* Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6">
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div className="space-y-1.5">
              <label htmlFor="email" className="block text-sm text-slate-300 font-medium">
                Email
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="jan@piekarnia.pl"
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2.5 text-slate-100 placeholder-slate-500 text-sm focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500/40 transition-colors"
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="password" className="block text-sm text-slate-300 font-medium">
                Hasło
              </label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2.5 text-slate-100 placeholder-slate-500 text-sm focus:outline-none focus:border-amber-500 focus:ring-1 focus:ring-amber-500/40 transition-colors"
              />
            </div>

            {errorMsg && (
              <p role="alert" className="text-sm text-red-400 bg-red-950/40 border border-red-900/50 rounded-lg px-3 py-2">
                {errorMsg}
              </p>
            )}

            <button
              type="submit"
              disabled={isPending}
              className="w-full flex items-center justify-center gap-2 bg-amber-600 hover:bg-amber-500 disabled:opacity-60 disabled:cursor-not-allowed text-white font-medium text-sm py-2.5 rounded-lg transition-colors"
            >
              {isPending && <Loader2 size={15} className="animate-spin" />}
              {isPending ? 'Logowanie…' : 'Zaloguj się'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
