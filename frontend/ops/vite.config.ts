import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@ui': path.resolve(__dirname, '../packages/ui'),
      '@':   path.resolve(__dirname, 'src'),
      // packages/ui has no node_modules of its own — pin its deps to this
      // app's copies so both dev server and build can resolve them.
      'clsx':                  path.resolve(__dirname, 'node_modules/clsx'),
      'tailwind-merge':        path.resolve(__dirname, 'node_modules/tailwind-merge'),
      '@tanstack/react-query': path.resolve(__dirname, 'node_modules/@tanstack/react-query'),
    },
  },
  server: {
    port: 5174,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/ws':  { target: 'ws://localhost:8092',  ws: true },
    },
  },
});
