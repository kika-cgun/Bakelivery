// ─── Auth ────────────────────────────────────────────────────────────────────

export type Role = 'CUSTOMER' | 'BAKERY' | 'DRIVER' | 'DISPATCHER' | 'ADMIN';

export interface AuthResponse {
  token: string;
  email: string;
  role: Role;
}

export interface User {
  id: string;
  email: string;
  role: Role;
  name?: string;
}

// ─── Catalog ─────────────────────────────────────────────────────────────────

export interface Category {
  id: string;
  name: string;
  slug: string;
}

export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;          // grosze (PLN * 100)
  imageUrl?: string;
  categoryId: string;
  available: boolean;
  weight?: string;        // np. "680g"
  bakeryName?: string;
}

// ─── Cart (frontend-only) ────────────────────────────────────────────────────

export interface CartItem {
  product: Product;
  quantity: number;
}

// ─── Orders ──────────────────────────────────────────────────────────────────

export type OrderStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'BAKING'
  | 'READY_FOR_PICKUP'
  | 'IN_DELIVERY'
  | 'DELIVERED'
  | 'REJECTED'
  | 'CANCELLED';

export interface OrderItem {
  product: Product;
  quantity: number;
  unitPrice: number;
}

export interface DeliveryAddress {
  street: string;
  city: string;
  postalCode: string;
  notes?: string;
}

export interface Order {
  id: string;
  customerId: string;
  customerName: string;
  items: OrderItem[];
  totalPrice: number;
  status: OrderStatus;
  address: DeliveryAddress;
  createdAt: string;      // ISO
  updatedAt: string;
  driverId?: string;
  driverName?: string;
  estimatedDelivery?: string;
}

// ─── Driver / Dispatching ────────────────────────────────────────────────────

export interface DriverLocation {
  driverId: string;
  driverName: string;
  lat: number;
  lng: number;
  orderId?: string;
  updatedAt: string;
}

export interface Delivery {
  id: string;
  orderId: string;
  driverId: string;
  status: 'ASSIGNED' | 'PICKED_UP' | 'IN_PROGRESS' | 'DELIVERED';
  address: DeliveryAddress;
  customerName: string;
  createdAt: string;
}

// ─── Real-time Events ─────────────────────────────────────────────────────────

export type RealtimeEventType =
  | 'ORDER_CREATED'
  | 'ORDER_STATUS_CHANGED'
  | 'DRIVER_LOCATION';

export interface RealtimeEvent {
  type: RealtimeEventType;
  payload: {
    orderId?: string;
    status?: OrderStatus;
    driverId?: string;
    lat?: number;
    lng?: number;
    order?: Order;
  };
  timestamp: string;
}
