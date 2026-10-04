import { expect, test, type Page } from '@playwright/test'

// The whole app as a user sees it: the dashboard in a real browser, served by the API, with the
// demo shop in PostgreSQL. Accounts come from the environment (see playwright.config.ts).
const admin = { username: 'admin', password: requiredEnv('E2E_ADMIN_PASSWORD') }
const cashier = { username: 'demo', password: requiredEnv('E2E_CASHIER_PASSWORD') }

function requiredEnv(name: string): string {
  const value = process.env[name]
  if (!value) throw new Error(`Set ${name} (see playwright.config.ts)`)
  return value
}

async function logIn(page: Page, user: { username: string; password: string }) {
  await page.goto('/login')
  await page.getByLabel('Username', { exact: true }).fill(user.username)
  await page.getByLabel('Password', { exact: true }).fill(user.password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
}

// Current stock of a product, read from the Products page
async function stockOf(page: Page, product: string): Promise<number> {
  await page.goto('/products')
  const row = page.getByRole('row').filter({ has: page.getByRole('cell', { name: product, exact: true }) })
  return Number(await row.getByRole('cell').nth(2).innerText())
}

async function pick(page: Page, field: string, option: RegExp) {
  await page.getByRole('combobox', { name: field }).click()
  await page.getByRole('option', { name: option }).click()
}

test('a cashier sells two products in one sale and the stock goes down', async ({ page }) => {
  await logIn(page, cashier)
  const phonesBefore = await stockOf(page, 'Galaxy S26')
  const cablesBefore = await stockOf(page, 'USB-C Cable 1m')

  await page.getByRole('link', { name: 'New sale' }).click()
  await pick(page, 'Customer', /Sara El Idrissi/)
  await pick(page, 'Add a product', /Galaxy S26/)
  await pick(page, 'Add a product', /USB-C Cable 1m/)
  await page.getByLabel('Quantity of USB-C Cable 1m').fill('2')
  await expect(page.getByText(/Total: MAD\s9,599\.80/)).toBeVisible()
  await page.getByRole('button', { name: 'Complete sale' }).click()

  // The confirmation box (the pop-up notification also says "Sale #N recorded: MAD ...")
  await expect(page.getByText(/^Sale #\d+ recorded$/)).toBeVisible()
  expect(await stockOf(page, 'Galaxy S26')).toBe(phonesBefore - 1)
  expect(await stockOf(page, 'USB-C Cable 1m')).toBe(cablesBefore - 2)

  // The stock history shows both lines, made by the cashier
  await page.goto('/stock-history')
  const latest = page.getByRole('row').nth(1)
  await expect(latest).toContainText('demo')
  await expect(latest).toContainText('Sale')
})

test('a cashier sees no revenue and cannot open user management', async ({ page }) => {
  await logIn(page, cashier)

  await expect(page.getByText('Low stock', { exact: true })).toBeVisible()
  // Revenue figures and the CSV export are for admins only
  await expect(page.getByText('Average sale', { exact: true })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Export CSV' })).toHaveCount(0)
  await expect(page.getByRole('link', { name: 'Users' })).toHaveCount(0)

  await page.goto('/users')
  await expect(page).toHaveURL(/\/$/)
})

test('an admin sees the reports and restocks a product', async ({ page }) => {
  await logIn(page, admin)
  await expect(page.getByText('Average sale', { exact: true })).toBeVisible()
  await expect(page.getByText('Revenue per day')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Export CSV' })).toBeVisible()

  // Unique per run, so the test finds its own restock even when run again on the same data
  const reason = `E2E delivery ${Date.now()}`
  const before = await stockOf(page, 'Phone Case')
  await page.getByRole('button', { name: 'Actions for Phone Case' }).click()
  await page.getByRole('menuitem', { name: 'Restock' }).click()
  await page.getByLabel('Quantity received').fill('12')
  await page.getByLabel('Reason (optional)').fill(reason)
  await page.getByRole('button', { name: 'Add to stock' }).click()

  await expect(page.getByText(`Phone Case: ${before + 12} in stock`)).toBeVisible()
  await page.goto('/stock-history')
  await expect(page.getByRole('row').filter({ hasText: reason })).toContainText('admin')
})

test('pages survive a browser refresh, and logging out ends the session', async ({ page }) => {
  await logIn(page, cashier)

  // Served by the API: a refresh on a dashboard page must not be a 404
  await page.goto('/sales/new')
  await page.reload()
  await expect(page.getByRole('heading', { name: 'New sale' })).toBeVisible()

  // The session is kept across reloads...
  await page.reload()
  await expect(page.getByRole('heading', { name: 'New sale' })).toBeVisible()

  // ...until logging out
  await page.getByRole('button', { name: 'Log out' }).click()
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()
  await page.goto('/products')
  await expect(page).toHaveURL(/\/login$/)
})
