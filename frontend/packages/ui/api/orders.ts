import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from './client';
import type { Order, OrderStatus, DeliveryAddress, CartItem } from '../types';

interface CreateOrderPayload {
  items: Array<{ productId: string; quantity: number }>;
  address: DeliveryAddress;
}

export const ordersApi = {
  createOrder: (payload: CreateOrderPayload) =>
    api.post<Order>('/orders', payload),

  getOrder: (id: string) =>
    api.get<Order>(`/orders/${id}`),

  getMyOrders: () =>
    api.get<Order[]>('/orders/my'),

  getBakeryOrders: () =>
    api.get<Order[]>('/orders/bakery'),

  updateStatus: (id: string, status: OrderStatus) =>
    api.put<Order>(`/orders/${id}/status`, { status }),
};

export function useOrder(id: string) {
  return useQuery({
    queryKey: ['orders', id],
    queryFn: () => ordersApi.getOrder(id),
    enabled: !!id,
    staleTime: 10_000,
    refetchInterval: 30_000, // fallback polling if WS disconnects
  });
}

export function useMyOrders() {
  return useQuery({
    queryKey: ['orders', 'my'],
    queryFn: ordersApi.getMyOrders,
    staleTime: 30_000,
  });
}

export function useBakeryOrders() {
  return useQuery({
    queryKey: ['orders', 'bakery'],
    queryFn: ordersApi.getBakeryOrders,
    staleTime: 5_000,
  });
}

export function useCreateOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateOrderPayload) => ordersApi.createOrder(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['orders'] }),
  });
}

export function useUpdateOrderStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: OrderStatus }) =>
      ordersApi.updateStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['orders'] }),
  });
}

/** Helper: convert CartItem[] to CreateOrderPayload items */
export function cartToOrderItems(
  items: CartItem[],
): CreateOrderPayload['items'] {
  return items.map((i) => ({
    productId: i.product.id,
    quantity: i.quantity,
  }));
}
