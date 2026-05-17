import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from './client';
import type { Delivery, DriverLocation } from '../types';

export const driverApi = {
  getDeliveries: () => api.get<Delivery[]>('/driver-ops/deliveries'),
  updateDeliveryStatus: (id: string, status: Delivery['status']) =>
    api.put<Delivery>(`/driver-ops/deliveries/${id}/status`, { status }),
  getActiveDrivers: () => api.get<DriverLocation[]>('/dispatching/drivers/active'),
};

export function useDriverDeliveries() {
  return useQuery({
    queryKey: ['driver', 'deliveries'],
    queryFn: driverApi.getDeliveries,
    staleTime: 10_000,
    refetchInterval: 20_000,
  });
}

export function useUpdateDeliveryStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: Delivery['status'] }) =>
      driverApi.updateDeliveryStatus(id, status),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['driver'] }),
  });
}

export function useActiveDrivers() {
  return useQuery({
    queryKey: ['dispatcher', 'drivers'],
    queryFn: driverApi.getActiveDrivers,
    staleTime: 5_000,
    refetchInterval: 10_000,
  });
}
