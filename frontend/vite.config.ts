/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // In development the API runs on its own (Eclipse or Docker) on port 8080.
    // The browser only talks to Vite, which forwards /api, so there is no CORS to configure.
    // API_URL=http://localhost:8081 npm run dev  to use an API on another port
    proxy: {
      '/api': process.env.API_URL ?? 'http://localhost:8080',
      '/actuator': process.env.API_URL ?? 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    // Threads start reliably on Windows, where forked workers can time out while starting
    pool: 'threads',
    setupFiles: './src/test/setup.ts',
    css: false,
  },
})
