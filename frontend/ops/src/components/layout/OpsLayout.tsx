import type { ReactNode } from 'react';
import { useState } from 'react';
import { Menu } from 'lucide-react';
import { Sidebar } from './Sidebar';
import { useRealtimeStore } from '@/store/realtime';
import { cn } from '@ui/lib/utils';

interface OpsLayoutProps {
  title: string;
  children: ReactNode;
  actions?: ReactNode;
}

export function OpsLayout({ title, children, actions }: OpsLayoutProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const connected = useRealtimeStore((s) => s.connected);

  return (
    <div className="flex h-dvh overflow-hidden">
      <Sidebar mobileOpen={mobileOpen} onClose={() => setMobileOpen(false)} />

      <main className="flex-1 flex flex-col overflow-hidden bg-slate-50">
        {/* Topbar */}
        <header className="shrink-0 flex items-center gap-3 px-4 lg:px-6 h-14 bg-white shadow-[0_1px_0_0_rgba(0,0,0,0.06)]">
          {/* Mobile hamburger */}
          <button
            type="button"
            className="lg:hidden p-1.5 rounded-lg text-slate-500 hover:text-slate-700 hover:bg-slate-100 transition-colors"
            onClick={() => setMobileOpen(true)}
            aria-label="Otwórz menu"
          >
            <Menu size={18} />
          </button>

          <h1 className="flex-1 text-[15px] font-semibold text-slate-700 truncate">{title}</h1>

          {/* Live indicator */}
          <div
            className={cn(
              'flex items-center gap-1.5 transition-opacity',
              connected ? 'opacity-100' : 'opacity-0 pointer-events-none',
            )}
            aria-live="polite"
            aria-label={connected ? 'Połączono z serwerem na żywo' : undefined}
          >
            <span className="relative flex h-1.5 w-1.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-60" />
              <span className="relative inline-flex rounded-full h-1.5 w-1.5 bg-emerald-500" />
            </span>
            <span className="font-mono text-[10px] font-semibold text-emerald-600 tracking-widest uppercase">Live</span>
          </div>

          {/* Actions slot */}
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </header>

        {/* Page content */}
        <div className="flex-1 overflow-y-auto p-4 lg:p-6">
          {children}
        </div>
      </main>
    </div>
  );
}
