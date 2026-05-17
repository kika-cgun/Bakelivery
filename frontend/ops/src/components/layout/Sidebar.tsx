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
        'flex items-center gap-3 px-4 py-2.5 text-sm rounded-r-md transition-colors relative',
        active
          ? 'border-l-[3px] border-amber-500 bg-amber-500/10 text-amber-400 font-medium pl-[13px]'
          : 'text-slate-400 hover:bg-white/5 hover:text-slate-200 border-l-[3px] border-transparent',
      )}
    >
      <span className="shrink-0">{icon}</span>
      <span className="flex-1 truncate">{label}</span>
      {badge !== undefined && badge > 0 && (
        <span className="ml-auto bg-amber-500 text-slate-950 text-xs font-mono font-bold px-1.5 py-0.5 rounded-full min-w-[20px] text-center leading-tight">
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
  BAKERY: 'PIEKARNIA',
  DRIVER: 'KIEROWCA',
  DISPATCHER: 'DYSPOZYTOR',
};

const SECTION_LABELS: Record<string, string> = {
  BAKERY: 'Piekarnia',
  DRIVER: 'Kierowca',
  DISPATCHER: 'Dyspozytor',
};

function BakeryNav() {
  const { data: orders } = useBakeryOrders();
  const pendingCount = orders?.filter((o) => o.status === ('PENDING' as OrderStatus)).length ?? 0;

  return (
    <>
      <SidebarNavItem
        to="/bakery/orders"
        label="Zamówienia"
        icon={<ClipboardList size={16} />}
        badge={pendingCount}
      />
      <SidebarNavItem
        to="/bakery/catalog"
        label="Katalog"
        icon={<Package size={16} />}
      />
    </>
  );
}

function DriverNav() {
  return (
    <SidebarNavItem
      to="/driver/deliveries"
      label="Dostawy"
      icon={<Truck size={16} />}
    />
  );
}

function DispatcherNav() {
  return (
    <SidebarNavItem
      to="/dispatcher/map"
      label="Mapa dostaw"
      icon={<MapPin size={16} />}
    />
  );
}

export function Sidebar({ mobileOpen, onClose }: SidebarProps) {
  const { role, email, logout } = useAuthStore();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/');
  }

  const sidebarContent = (
    <aside className="flex flex-col h-full w-[220px] bg-[#0f172a] shrink-0">
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 py-5 border-b border-slate-800">
        <span className="w-8 h-8 rounded-lg bg-amber-600 flex items-center justify-center shrink-0">
          <span className="font-display text-white text-lg leading-none">B</span>
        </span>
        <div className="flex flex-col min-w-0">
          <span className="font-display text-white text-base leading-tight">Bakelivery</span>
          {role && (
            <span className="font-mono text-[10px] text-amber-400 uppercase tracking-widest">
              {ROLE_LABELS[role] ?? role}
            </span>
          )}
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto scrollbar-thin py-3 px-2">
        {role && (
          <>
            <p className="px-3 mb-1.5 text-[10px] font-mono uppercase tracking-widest text-slate-600">
              {SECTION_LABELS[role] ?? role}
            </p>
            {role === 'BAKERY' && <BakeryNav />}
            {role === 'DRIVER' && <DriverNav />}
            {role === 'DISPATCHER' && <DispatcherNav />}
          </>
        )}
      </nav>

      {/* Footer */}
      <div className="border-t border-slate-800 px-4 py-4 space-y-3">
        {email && (
          <p className="text-[11px] text-slate-500 truncate font-mono">{email}</p>
        )}
        <button
          type="button"
          onClick={handleLogout}
          className="flex items-center gap-2 text-sm text-slate-400 hover:text-red-400 transition-colors w-full"
        >
          <LogOut size={14} />
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
          <div
            className="fixed inset-0 bg-black/50"
            onClick={onClose}
            aria-hidden="true"
          />
          <div className="relative z-50 flex">
            {sidebarContent}
          </div>
        </div>
      )}
    </>
  );
}
