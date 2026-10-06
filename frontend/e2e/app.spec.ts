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
  await page.goto('/admin/login')
  await page.getByLabel('Username', { exact: true }).fill(user.username)
  await page.getByLabel('Password', { exact: true }).fill(user.password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible()
}

// Current stock of a product, read from the "In stock" column of the Products page
async function stockOf(page: Page, product: string): Promise<number> {
  await page.goto('/admin/products')
  const row = page.getByRole('row').filter({ has: page.getByRole('cell', { name: product, exact: true }) })
  await row.waitFor() // the table is loaded, headers included
  const column = (await page.getByRole('columnheader').allInnerTexts()).indexOf('In stock')
  return Number(await row.getByRole('cell').nth(column).innerText())
}

// An 8 x 8 blue PNG
const TINY_PNG = 'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAIAAABLbSncAAAAEUlEQVR4nGNQSLiAFTEMLQkApqhUAUiRg/kAAAAASUVORK5CYII='

async function showInShop(page: Page, product: string, shown: boolean) {
  await page.goto('/admin/products')
  await page.getByRole('button', { name: `Actions for ${product}` }).click()
  await page.getByRole('menuitem', { name: 'Edit' }).click()
  await page.getByRole('switch', { name: /Show in the online shop/ }).setChecked(shown)
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByRole('dialog')).toHaveCount(0)
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
  await page.goto('/admin/stock-history')
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

  await page.goto('/admin/users')
  await expect(page).toHaveURL(/\/admin$/)
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
  await page.goto('/admin/stock-history')
  await expect(page.getByRole('row').filter({ hasText: reason })).toContainText('admin')
})

test('the products of the demo shop are filed in categories', async ({ page }) => {
  await logIn(page, cashier)
  await page.goto('/admin/products')
  await expect(page.getByRole('row').filter({ hasText: 'Galaxy S26' })).toContainText('Phones')

  await pick(page, 'Category', /^Protection$/)

  await expect(page).toHaveURL(/category=\d+/)
  await expect(page.getByRole('cell', { name: 'Phone Case', exact: true })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'Screen Protector', exact: true })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'Galaxy S26', exact: true })).toHaveCount(0)
})

test('recent price drops show the old price struck through', async ({ page }) => {
  await logIn(page, cashier)
  await page.goto('/admin/products')

  // In the demo month the phone went from 9,999 to 9,500 and the case from 149 to 129
  const phone = page.getByRole('row').filter({ has: page.getByRole('cell', { name: 'Galaxy S26', exact: true }) })
  await expect(phone).toContainText('9,999.00')
  await expect(phone).toContainText('-5%')
  const phoneCase = page.getByRole('row').filter({ has: page.getByRole('cell', { name: 'Phone Case', exact: true }) })
  await expect(phoneCase).toContainText('-13%')
})

test('demo products have pictures, and an admin adds one and deletes it', async ({ page }) => {
  await logIn(page, admin)
  await page.goto('/admin/products')

  // The cover picture comes from the API and really loads
  const row = page.getByRole('row').filter({ has: page.getByRole('cell', { name: 'Redmi Note 15', exact: true }) })
  const thumb = row.locator('img')
  await expect(thumb).toHaveAttribute('src', /^\/api\/images\/\d+$/)
  await expect.poll(() => thumb.evaluate((img) => Reflect.get(img, 'naturalWidth') as number)).toBeGreaterThan(0)

  await page.getByRole('button', { name: 'Actions for Redmi Note 15' }).click()
  await page.getByRole('menuitem', { name: 'Pictures' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByAltText(/^Picture \d+ of Redmi Note 15$/)).toHaveCount(1)

  // A small PNG, shrunk and converted by the browser, then saved by the API
  await dialog.locator('input[type="file"]').setInputFiles({ name: 'back.png', mimeType: 'image/png', buffer: Buffer.from(TINY_PNG, 'base64') })
  await expect(dialog.getByAltText(/^Picture \d+ of Redmi Note 15$/)).toHaveCount(2)

  // Deleted again, so the test can run again on the same data
  await dialog.getByRole('button', { name: 'Delete picture 2' }).click()
  await expect(dialog.getByAltText(/^Picture \d+ of Redmi Note 15$/)).toHaveCount(1)
})

test('a visitor browses the shop and fills a cart without logging in', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /Phones and accessories/ })).toBeVisible()

  await page.getByRole('link', { name: /Phones \d+ products/ }).click()
  await expect(page.getByRole('heading', { name: 'Phones', level: 1 })).toBeVisible()
  await page.getByRole('link', { name: 'Redmi Note 15' }).click()
  await expect(page).toHaveURL(/\/p\/\d+-redmi-note-15$/)

  await page.getByRole('textbox', { name: 'Quantity' }).fill('2')
  await page.getByRole('button', { name: 'Add to cart' }).click()
  await expect(page.getByRole('dialog', { name: 'Your cart (2)' })).toBeVisible()
  await page.getByRole('link', { name: 'View cart' }).click()
  await expect(page.getByText(/MAD\s5,798\.00/).first()).toBeVisible()

  // The cart is kept in the browser
  await page.reload()
  await expect(page.getByRole('button', { name: 'Cart, 2 items' })).toBeVisible()
})

test('a visitor orders with cash on delivery, then tracks the order', async ({ page }) => {
  // A phone number of its own on each run: one phone may only have 3 orders waiting for a call
  const phone = `06${String(Date.now()).slice(-8)}`
  await page.goto('/shop?category=chargers-cables')
  await page.getByRole('button', { name: 'Add USB-C Cable 1m to the cart' }).click()
  await page.getByRole('link', { name: 'View cart' }).click()
  await page.getByRole('link', { name: 'Checkout' }).click()

  await page.getByRole('textbox', { name: 'Full name' }).fill('E2E Visitor')
  await page.getByRole('textbox', { name: 'Phone' }).fill(phone)
  await page.getByRole('textbox', { name: 'City' }).fill('Rabat')
  await page.getByRole('textbox', { name: 'Address' }).fill('1 avenue des Tests')
  await page.getByRole('button', { name: 'Place order' }).click()

  await expect(page.getByRole('heading', { name: 'Thank you, E2E!' })).toBeVisible()
  const number = await page.getByText(/^TS-[A-Z0-9]{6}$/).first().innerText()
  await expect(page).toHaveURL(new RegExp(`/order/${number}$`))
  await expect(page.getByRole('button', { name: 'Cart, 0 items' })).toBeVisible()

  await page.goto('/track')
  await page.getByRole('textbox', { name: 'Order number' }).fill(number)
  await page.getByRole('textbox', { name: 'Phone' }).fill(phone)
  await page.getByRole('button', { name: 'Show my order' }).click()
  await expect(page.getByText(`Order ${number}`)).toBeVisible()
  await expect(page.getByText('Received')).toBeVisible()
})

test('a product hidden by an admin disappears from the shop', async ({ page }) => {
  await logIn(page, admin)
  await showInShop(page, 'Screen Protector', false)
  await page.goto('/shop?search=screen')
  await expect(page.getByText('No products found. Try another search or category.')).toBeVisible()

  // Shown again, so the test can run again on the same data
  await showInShop(page, 'Screen Protector', true)
  await page.goto('/shop?search=screen')
  await expect(page.getByRole('link', { name: 'Screen Protector' })).toBeVisible()
})

test('pages survive a browser refresh, and logging out ends the session', async ({ page }) => {
  await logIn(page, cashier)

  // Served by the API: a refresh on a dashboard page must not be a 404
  await page.goto('/admin/sales/new')
  await page.reload()
  await expect(page.getByRole('heading', { name: 'New sale' })).toBeVisible()

  // The session is kept across reloads...
  await page.reload()
  await expect(page.getByRole('heading', { name: 'New sale' })).toBeVisible()

  // ...until logging out
  await page.getByRole('button', { name: 'Log out' }).click()
  await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible()
  await page.goto('/admin/products')
  await expect(page).toHaveURL(/\/admin\/login$/)
})
