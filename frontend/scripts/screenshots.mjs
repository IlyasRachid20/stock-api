// Takes the README screenshots from a running demo (API in demo mode + the shop and back-office).
//
//   BASE_URL=http://localhost:5173 ADMIN_PASSWORD=... node scripts/screenshots.mjs
//
// Uses the Edge or Chrome already installed on the machine (playwright-core downloads no browser).
import { mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright-core'

const baseUrl = process.env.BASE_URL ?? 'http://localhost:5173'
const adminPassword = process.env.ADMIN_PASSWORD
const outDir = fileURLToPath(new URL('../../docs/screenshots/', import.meta.url))
if (!adminPassword) throw new Error('Set ADMIN_PASSWORD (the demo admin password)')
mkdirSync(outDir, { recursive: true })

const browser = await chromium.launch({ channel: process.env.BROWSER ?? 'msedge' })
const page = await browser.newPage({ viewport: { width: 1280, height: 800 }, locale: 'en-GB' })
const shot = async (name) => {
  await page.waitForLoadState('networkidle')
  await page.mouse.move(0, 0) // no hover effects or tooltips in the picture
  await page.waitForTimeout(600) // let charts finish their animation
  await page.screenshot({ path: `${outDir}${name}.png` })
  console.log(`saved ${name}.png`)
}

// The online shop, as a visitor sees it
await page.goto(`${baseUrl}/`)
await page.getByText('Shop by category').waitFor()
await shot('shop-home')

await page.goto(`${baseUrl}/p/1`)
await page.getByRole('heading', { name: 'Galaxy S26' }).waitFor()
await shot('shop-product')

// The back-office
await page.goto(`${baseUrl}/admin/login`)
await shot('login')

await page.getByLabel('Username', { exact: true }).fill('admin')
await page.getByLabel('Password', { exact: true }).fill(adminPassword)
await page.getByRole('button', { name: 'Sign in' }).click()
await page.getByText('Revenue per day').waitFor()
await shot('dashboard')

await page.goto(`${baseUrl}/admin/sales/new`)
await page.getByRole('combobox', { name: 'Customer' }).click()
await page.getByRole('option', { name: /Ahmed Benali/ }).click()
for (const product of ['Galaxy S26', 'USB-C Cable 1m', 'Screen Protector']) {
  await page.getByRole('combobox', { name: 'Add a product' }).click()
  await page.getByRole('option', { name: new RegExp(product) }).click()
}
await page.getByLabel('Quantity of USB-C Cable 1m').fill('2')
await page.getByText('Total:').click()
await shot('new-sale')

await page.goto(`${baseUrl}/admin/products`)
await page.getByText('Galaxy S26').waitFor()
await shot('products')

await page.goto(`${baseUrl}/admin/stock-history`)
await page.getByText('Stock history').first().waitFor()
await shot('stock-history')

await browser.close()
