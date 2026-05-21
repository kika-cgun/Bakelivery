import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useAuthStore } from '@/store/auth';
import type { ReactNode } from 'react';

// Pages (lazy-loaded)
import { lazy, Suspense } from 'react';

const CatalogPage    = lazy(() => import('@/pages/CatalogPage'));
const ProductPage    = lazy(() => import('@/pages/ProductPage'));
const CartPage       = lazy(() => import('@/pages/CartPage'));
const CheckoutPage   = lazy(() => import('@/pages/CheckoutPage'));
const TrackingPage   = lazy(() => import('@/pages/TrackingPage'));
const LoginPage      = lazy(() => import('@/pages/LoginPage'));
const RegisterPage   = lazy(() => import('@/pages/RegisterPage'));

function PageLoader() {
  return (
    <div className="min-h-dvh flex items-center justify-center bg-[#FDF6EC]">
      <div className="w-8 h-8 border-2 border-amber-600 border-t-transparent rounded-full animate-spin" />
    </div>
  );
}

function PrivateRoute({ children }: { children: ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          <Route path="/"                    element={<CatalogPage />} />
          <Route path="/products/:id"        element={<ProductPage />} />
          <Route path="/cart"                element={<CartPage />} />
          <Route path="/login"               element={<LoginPage />} />
          <Route path="/register"            element={<RegisterPage />} />
          <Route path="/checkout"            element={<PrivateRoute><CheckoutPage /></PrivateRoute>} />
          <Route path="/orders/:id/track"    element={<PrivateRoute><TrackingPage /></PrivateRoute>} />
          <Route path="*"                    element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
