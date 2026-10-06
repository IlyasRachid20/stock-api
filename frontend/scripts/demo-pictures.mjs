// Renders the demo shop's product pictures (drawn in demo-pictures.html, one SVG per product)
// to the JPEGs the demo mode loads, in src/main/resources/demo/pictures/:
//
//   node scripts/demo-pictures.mjs
//
// Uses the Edge or Chrome already installed on the machine (playwright-core downloads no browser).
import { mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { chromium } from 'playwright-core'

const source = new URL('./demo-pictures.html', import.meta.url)
const outDir = fileURLToPath(new URL('../../src/main/resources/demo/pictures/', import.meta.url))
mkdirSync(outDir, { recursive: true })

const browser = await chromium.launch({ channel: process.env.BROWSER ?? 'msedge' })
const page = await browser.newPage({ viewport: { width: 1700, height: 900 }, deviceScaleFactor: 2 })
await page.goto(source.href)
for (const id of await page.$$eval('svg[id]', (svgs) => svgs.map((svg) => svg.id))) {
  await page.locator(`#${id}`).screenshot({ path: `${outDir}${id}.jpg`, type: 'jpeg', quality: 88 })
  console.log(`saved ${id}.jpg`)
}
await browser.close()
