import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useLogin } from '@ui/api/auth';
import { useAuthStore } from '@/store/auth';
import type { Role } from '@ui/types';

const OPS_ROLES: Role[] = ['BAKERY_ADMIN', 'DRIVER', 'DISPATCHER', 'SUPER_ADMIN'];

function roleRedirect(role: Role): string {
  if (role === 'BAKERY_ADMIN') return '/bakery/orders';
  if (role === 'DRIVER')       return '/driver/deliveries';
  if (role === 'DISPATCHER')   return '/dispatcher/map';
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
            setErrorMsg('To konto jest przeznaczone dla klientów. Skorzystaj z aplikacji sklepu Bakelivery.');
            return;
          }
          setAuth(data.token, data.email, data.role);
          navigate(roleRedirect(data.role), { replace: true });
        },
        onError(err) {
          setErrorMsg(err instanceof Error ? err.message : 'Błąd logowania. Sprawdź dane i spróbuj ponownie.');
        },
      },
    );
  }

  return (
    <div className="min-h-dvh flex items-center justify-center bg-[#080E1A] px-4 relative overflow-hidden">
      {/* Subtle radial glow */}
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_80%_50%_at_50%_-10%,rgba(217,119,6,0.07)_0%,transparent_70%)] pointer-events-none" />

      <div className="w-full max-w-sm relative">
        {/* Logo */}
        <div className="flex flex-col items-center mb-8 gap-3">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-amber-500 to-amber-700 flex items-center justify-center shadow-[0_4px_16px_rgba(217,119,6,.40)]">
            <span className="font-display text-white text-2xl leading-none">B</span>
          </div>
          <div className="text-center">
            <h1 className="font-display text-2xl text-white tracking-tight">Bakelivery</h1>
            <p className="font-mono text-[10px] text-slate-500 uppercase tracking-[0.2em] mt-0.5">Panel operacyjny</p>
          </div>
        </div>

        {/* Card */}
        <div className="bg-[#111827] border border-slate-800/70 rounded-2xl overflow-hidden shadow-[0_20px_60px_rgba(0,0,0,0.5)]">
          {/* Top shimmer */}
          <div className="h-px bg-gradient-to-r from-transparent via-amber-600/40 to-transparent" />

          <form onSubmit={handleSubmit} className="p-6 space-y-4" noValidate>
            <div className="space-y-1.5">
              <label htmlFor="email" className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">
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
                className="w-full bg-slate-900/60 border border-slate-700/60 rounded-xl px-4 py-3 text-slate-100 placeholder-slate-600 text-sm focus:outline-none focus:border-amber-500/60 focus:ring-1 focus:ring-amber-500/30 transition-all"
              />
            </div>

            <div className="space-y-1.5">
              <label htmlFor="password" className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">
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
                className="w-full bg-slate-900/60 border border-slate-700/60 rounded-xl px-4 py-3 text-slate-100 placeholder-slate-600 text-sm focus:outline-none focus:border-amber-500/60 focus:ring-1 focus:ring-amber-500/30 transition-all"
              />
            </div>

            {errorMsg && (
              <p role="alert" className="text-xs text-red-400 bg-red-950/50 border border-red-900/50 rounded-xl px-4 py-3 leading-relaxed">
                {errorMsg}
              </p>
            )}

            <button
              type="submit"
              disabled={isPending}
              className="w-full flex items-center justify-center gap-2 bg-amber-600 hover:bg-amber-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold text-sm py-3 rounded-xl transition-all shadow-[0_4px_14px_rgba(217,119,6,.3)] active:scale-[0.99] mt-1"
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
