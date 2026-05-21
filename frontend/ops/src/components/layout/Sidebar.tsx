import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ClipboardList, Package, MapPin, Truck, LogOut } from 'lucide-react';
import { useAuthStore } from '@/store/auth';
import { useBakeryOrders } from '@ui/api/orders';
import { cn } from '@ui/lib/utils';
import type { OrderStatus } from '@ui/types';

interface NavItem {
  to: string;
  label: string;
  icon: React.ReactNode;
  badge?: number;
}

function SidebarNavItem({ to, label, icon, badge }: NavItem) {
  const { pathname } = useLocation();
  const active = pathname === to || pathname.startsWith(to + '/');

  return (
    <Link
      to={to}
      className={cn(
        'flex items-center gap-3 mx-2 px-3 py-2.5 text-sm rounded-xl transition-all duration-150',
        active
          ? 'bg-amber-500/15 text-amber-400 font-semibold'
          : 'text-slate-400 hover:bg-white/5 hover:text-slate-200',
      )}
    >
      <span className="shrink-0 opacity-80">{icon}</span>
      <span className="flex-1 truncate">{label}</span>
      {badge !== undefined && badge > 0 && (
        <span className="ml-auto bg-amber-500 text-slate-950 text-[10px] font-mono font-bold px-1.5 py-0.5 rounded-full min-w-[20px] text-center leading-tight">
          {badge > 99 ? '99+' : badge}
        </span>
      )}
    </Link>
  );
}

interface SidebarProps {
  mobileOpen: boolean;
  onClose: () => void;
}

const ROLE_LABELS: Record<string, string> = {
  BAKERY_ADMIN: 'PIEKARNIA',
  DRIVER:       'KIEROWCA',
  DISPATCHER:   'DYSPOZYTOR',
  SUPER_ADMIN:  'ADMIN',
};

const SECTION_LABELS: Record<string, string> = {
  BAKERY_ADMIN: 'Piekarnia',
  DRIVER:       'Kierowca',
  DISPATCHER:   'Dyspozytor',
  SUPER_ADMIN:  'Admin',
};

function BakeryNav() {
  const { data: orders } = useBakeryOrders();
  const pendingCount = orders?.filter((o) => o.status === ('PENDING' as OrderStatus)).length ?? 0;

  return (
    <>
      <SidebarNavItem to="/bakery/orders" label="Zamówienia" icon={<ClipboardList size={16} />} badge={pendingCount} />
      <SidebarNavItem to="/bakery/catalog" label="Katalog" icon={<Package size={16} />} />
    </>
  );
}

function DriverNav() {
  return <SidebarNavItem to="/driver/deliveries" label="Dostawy" icon={<Truck size={16} />} />;
}

function DispatcherNav() {
  return <SidebarNavItem to="/dispatcher/map" label="Mapa dostaw" icon={<MapPin size={16} />} />;
}

export function Sidebar({ mobileOpen, onClose }: SidebarProps) {
  const { role, email, logout } = useAuthStore();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/');
  }

  const sidebarContent = (
    <aside className="flex flex-col h-full w-[220px] bg-[#0D1424] shrink-0 border-r border-slate-800/50">
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-slate-800/50">
        <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-amber-500 to-amber-700 flex items-center justify-center shrink-0 shadow-[0_2px_8px_rgba(217,119,6,.4)]">
          <span className="font-display text-white text-base leading-none">B</span>
        </div>
        <div className="flex flex-col min-w-0">
          <span className="font-display text-white text-base leading-tight">Bakelivery</span>
          {role && (
            <span className="font-mono text-[9px] text-amber-500/70 uppercase tracking-[0.15em]">
              {ROLE_LABELS[role] ?? role}
            </span>
          )}
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto scrollbar-thin py-3">
        {role && (
          <>
            <p className="px-5 mb-2 text-[9px] font-mono uppercase tracking-[0.18em] text-slate-600">
              {SECTION_LABELS[role] ?? role}
            </p>
            {role === 'BAKERY_ADMIN' && <BakeryNav />}
            {role === 'DRIVER'       && <DriverNav />}
            {role === 'DISPATCHER'   && <DispatcherNav />}
          </>
        )}
      </nav>

      {/* Footer */}
      <div className="border-t border-slate-800/50 px-4 py-4 space-y-3">
        {email && (
          <p className="text-[10px] text-slate-600 truncate font-mono">{email}</p>
        )}
        <button
          type="button"
          onClick={handleLogout}
          className="flex items-center gap-2 text-sm text-slate-500 hover:text-red-400 transition-colors w-full"
        >
          <LogOut size={13} />
          <span>Wyloguj się</span>
        </button>
      </div>
    </aside>
  );

  return (
    <>
      {/* Desktop sidebar */}
      <div className="hidden lg:flex h-full">{sidebarContent}</div>

      {/* Mobile overlay */}
      {mobileOpen && (
        <div className="lg:hidden fixed inset-0 z-40 flex">
          <div className="fixed inset-0 bg-black/60 backdrop-blur-sm" onClick={onClose} aria-hidden="true" />
          <div className="relative z-50 flex">{sidebarContent}</div>
        </div>
      )}
    </>
  );
}
