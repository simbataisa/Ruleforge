/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/v1/auth': { target: 'http://localhost:8081', changeOrigin: true },
      '/v1/users': { target: 'http://localhost:8081', changeOrigin: true },
      '/v1/organizations': { target: 'http://localhost:8081', changeOrigin: true },
      '/v1/spaces': { target: 'http://localhost:8081', changeOrigin: true },
      '/v1/api-keys': { target: 'http://localhost:8081', changeOrigin: true },
      '/v1/sso': { target: 'http://localhost:8081', changeOrigin: true },
      '/v1/rules': { target: 'http://localhost:8082', changeOrigin: true },
      '/v1/approval-requests': { target: 'http://localhost:8082', changeOrigin: true },
      '/v1/folders': { target: 'http://localhost:8082', changeOrigin: true },
      '/v1/tags': { target: 'http://localhost:8082', changeOrigin: true },
      '/v1/solve': { target: 'http://localhost:8083', changeOrigin: true },
      '/v1/analytics': { target: 'http://localhost:8085', changeOrigin: true },
      '/v1/schedules': { target: 'http://localhost:8086', changeOrigin: true },
      '/v1/batch': { target: 'http://localhost:8086', changeOrigin: true },
      '/v1/connectors': { target: 'http://localhost:8087', changeOrigin: true },
      '/v1/connections': { target: 'http://localhost:8087', changeOrigin: true },
      '/v1/audit': { target: 'http://localhost:8084', changeOrigin: true },
      '/v1/webhooks': { target: 'http://localhost:8084', changeOrigin: true },
      '/v1/events': { target: 'http://localhost:8084', changeOrigin: true },
      '/v1/ai': { target: 'http://localhost:8000', changeOrigin: true },
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: false,
  },
});
