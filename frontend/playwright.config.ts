import { defineConfig } from '@playwright/test'

// End-to-end tests: a real browser against the whole app (API + dashboard + PostgreSQL) started with
// Docker Compose in demo mode. They change data (sales, restocks), so they run one at a time, in order.
//
//   BASE_URL=http://localhost:8080 E2E_ADMIN_PASSWORD=... E2E_CASHIER_PASSWORD=... npm run e2e
//
// The browser is the Chrome (CI) or Edge (E2E_BROWSER=msedge) already installed: nothing is downloaded.
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:8080',
    channel: process.env.E2E_BROWSER ?? 'chrome',
    locale: 'en-GB',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
})
