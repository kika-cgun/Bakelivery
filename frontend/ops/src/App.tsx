import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useAuthStore } from '@/store/auth';
import type { ReactNode } from 'react';
import { lazy, Suspense } from 'react';
import type { Role } from '@ui/types';

const LoginPage        = lazy(() => import('@/pages/LoginPage'));
const OrdersPage       = lazy(() => import('@/pages/bakery/OrdersPage'));
const BakeryCatalog    = lazy(() => import('@/pages/bakery/CatalogPage'));
const DeliveriesPage   = lazy(() => import('@/pages/driver/DeliveriesPage'));
const MapPage          = lazy(() => import('@/pages/dispatcher/MapPage'));

function PageLoader() {
  return (
    <div className="min-h-dvh flex items-center justify-center bg-slate-950">
      <div className="w-8 h-8 border-2 border-amber-500 border-t-transparent rounded-full animate-spin" />
    </div>
  );
}

function PrivateRoute({
  children,
  allowedRoles,
}: {
  children: ReactNode;
  allowedRoles?: Role[];
}) {
  const { isAuthenticated, role } = useAuthStore();

  if (!isAuthenticated) return <Navigate to="/" replace />;

  if (allowedRoles && role && !allowedRoles.includes(role)) {
    // Redirect to the user's correct view
    if (role === 'BAKERY_ADMIN')      return <Navigate to="/bakery/orders" replace />;
    if (role === 'DRIVER')      return <Navigate to="/driver/deliveries" replace />;
    if (role === 'DISPATCHER')  return <Navigate to="/dispatcher/map" replace />;
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="/" element={<LoginPage />} />

          {/* Bakery */}
          <Route path="/bakery/orders"
            element={<PrivateRoute allowedRoles={['BAKERY_ADMIN']}><OrdersPage /></PrivateRoute>} />
          <Route path="/bakery/catalog"
            element={<PrivateRoute allowedRoles={['BAKERY_ADMIN']}><BakeryCatalog /></PrivateRoute>} />

          {/* Driver */}
          <Route path="/driver/deliveries"
            element={<PrivateRoute allowedRoles={['DRIVER']}><DeliveriesPage /></PrivateRoute>} />

          {/* Dispatcher */}
          <Route path="/dispatcher/map"
            element={<PrivateRoute allowedRoles={['DISPATCHER']}><MapPage /></PrivateRoute>} />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
