import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from './client';
import type { Product, Category } from '../types';

export const catalogApi = {
  getProducts: (categoryId?: string) => {
    const params = categoryId ? `?categoryId=${categoryId}` : '';
    return api.get<Product[]>(`/catalog/products${params}`);
  },
  getProduct: (id: string) => api.get<Product>(`/catalog/products/${id}`),
  getCategories: () => api.get<Category[]>('/catalog/categories'),

  // Bakery ops
  createProduct: (data: Omit<Product, 'id'>) =>
    api.post<Product>('/catalog/products', data),
  updateProduct: (id: string, data: Partial<Product>) =>
    api.put<Product>(`/catalog/products/${id}`, data),
  deleteProduct: (id: string) =>
    api.delete<void>(`/catalog/products/${id}`),
  toggleAvailability: (id: string, available: boolean) =>
    api.patch<Product>(`/catalog/products/${id}/availability`, { available }),
};

export function useProducts(categoryId?: string) {
  return useQuery({
    queryKey: ['products', categoryId ?? 'all'],
    queryFn: () => catalogApi.getProducts(categoryId),
    staleTime: 30_000,
  });
}

export function useProduct(id: string) {
  return useQuery({
    queryKey: ['products', id],
    queryFn: () => catalogApi.getProduct(id),
    enabled: !!id,
    staleTime: 30_000,
  });
}

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: catalogApi.getCategories,
    staleTime: 5 * 60_000,
  });
}

export function useCreateProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Omit<Product, 'id'>) => catalogApi.createProduct(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  });
}

export function useUpdateProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<Product> }) =>
      catalogApi.updateProduct(id, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  });
}

export function useDeleteProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => catalogApi.deleteProduct(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['products'] }),
  });
}
