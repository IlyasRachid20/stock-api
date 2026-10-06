import { describe, expect, it, vi } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { mockApi, renderApp } from '../test/render'
import { prepareImage } from '../utils/images'

// jsdom can't decode pictures: the browser-side shrinking is replaced by a pass-through
vi.mock('../utils/images', () => ({ prepareImage: vi.fn(async (file: File) => file) }))

// Starts the app already logged in, as the AuthProvider would after a login
function loggedInAs(role: 'ADMIN' | 'CASHIER') {
  localStorage.setItem('stock-api.session', JSON.stringify({
    token: 'token-123', expiresAt: Date.now() + 3_600_000,
    username: role === 'ADMIN' ? 'admin' : 'sara', roles: [role],
  }))
}

const page = <T,>(content: T[]) => ({ content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } })
const phone = { id: 1, name: 'Galaxy S26', price: 9500, quantity: 3, minQuantity: 1, lowStock: false }
const cable = { id: 2, name: 'USB-C Cable', price: 49.9, quantity: 10, minQuantity: 5, lowStock: false }
const ahmed = { id: 7, name: 'Ahmed', email: 'ahmed@test.com', phone: null }
const phones = { id: 3, name: 'Phones', productCount: 1 }
const audio = { id: 4, name: 'Audio', productCount: 0 }
const phoneInCategory = { ...phone, category: { id: 3, name: 'Phones' } }
const reducedPhone = { ...phone, previousPrice: 9999 }
const picture = (id: number) => ({ id, url: `/api/images/${id}`, width: 800, height: 800 })
const phoneWithPictures = { ...phone, images: [picture(8), picture(9)] }

describe('products', () => {
  it('lets an admin create a product, showing the API validation errors under the fields', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/products': [200, page([phone])],
      'GET /api/categories': [200, [phones]],
      'POST /api/products': [400, { errors: { price: 'must be greater than or equal to 0.00' } }],
    })
    renderApp('/admin/products')

    await userEvent.click(await screen.findByRole('button', { name: 'New product' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Charger')
    await userEvent.type(within(dialog).getByLabelText('Price (MAD)'), '199')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create' }))

    expect(await within(dialog).findByText('must be greater than or equal to 0.00')).toBeInTheDocument()
    expect(calls.find((c) => c.method === 'POST')?.body)
      .toEqual({ name: 'Charger', price: 199, minQuantity: 0, quantity: 0, categoryId: null, description: null, published: true })
  })

  it('files a new product in a category', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/products': [200, page([phone])],
      'GET /api/categories': [200, [phones, audio]],
      'POST /api/products': [201, { ...phoneInCategory, id: 5, name: 'Redmi Note 15' }],
    })
    renderApp('/admin/products')

    await userEvent.click(await screen.findByRole('button', { name: 'New product' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Redmi Note 15')
    await userEvent.click(within(dialog).getByLabelText('Category', { selector: 'input' }))
    await userEvent.click(await within(dialog).findByRole('option', { name: 'Phones' }))
    await userEvent.type(within(dialog).getByLabelText('Price (MAD)'), '2899')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create' }))

    await waitFor(() => expect(calls.find((c) => c.method === 'POST')?.body)
      .toEqual({ name: 'Redmi Note 15', price: 2899, minQuantity: 0, quantity: 0, categoryId: 3, description: null, published: true }))
  })

  it('shows each product category and filters the list by category, in the address too', async () => {
    loggedInAs('CASHIER')
    const calls = mockApi({
      'GET /api/products': [200, page([phoneInCategory])],
      'GET /api/categories': [200, [phones, audio]],
    })
    const { router } = renderApp('/admin/products')

    expect(await screen.findByRole('cell', { name: 'Phones' })).toBeInTheDocument()
    await userEvent.click(screen.getByLabelText('Category', { selector: 'input' }))
    await userEvent.click(await screen.findByRole('option', { name: 'Audio' }))

    await waitFor(() => expect(router.state.location.search).toBe('?category=4'))
    await waitFor(() => expect(calls.some((c) => c.url.startsWith('/api/products?') && c.url.includes('categoryId=4'))).toBe(true))
  })

  it('lets an admin restock a product', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/products': [200, page([phone])],
      'POST /api/products/1/restock': [200, { ...phone, quantity: 13 }],
    })
    renderApp('/admin/products')

    await userEvent.click(await screen.findByRole('button', { name: 'Actions for Galaxy S26' }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Restock' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText('Quantity received'), '10')
    await userEvent.type(within(dialog).getByLabelText('Reason (optional)'), 'Delivery #42')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add to stock' }))

    await waitFor(() => expect(calls.find((c) => c.url === '/api/products/1/restock')?.body)
      .toEqual({ quantity: 10, reason: 'Delivery #42' }))
  })

  it('shows a recent price drop with the old price struck through and the reduction', async () => {
    loggedInAs('CASHIER')
    mockApi({ 'GET /api/products': [200, page([reducedPhone, cable])], 'GET /api/categories': [200, []] })
    renderApp('/admin/products')

    const row = (await screen.findByText('Galaxy S26')).closest('tr')!
    expect(within(row).getByText('-5%')).toBeInTheDocument()
    // The old price is struck through, and screen readers hear "Was" before it
    expect(within(row).getByText(/^MAD.9,999\.00$/)).toHaveStyle({ textDecoration: 'line-through' })
    expect(within(row).getByText('Was')).toBeInTheDocument()
    // No reduction, no struck price
    const cableRow = screen.getByText('USB-C Cable').closest('tr')!
    expect(within(cableRow).queryByText(/Was/)).not.toBeInTheDocument()
  })

  it('shows the price history of a product to an admin', async () => {
    loggedInAs('ADMIN')
    mockApi({
      'GET /api/products': [200, page([reducedPhone])],
      'GET /api/categories': [200, []],
      'GET /api/products/1/price-history': [200, [
        { id: 4, oldPrice: 9999, newPrice: 9500, changedBy: 'admin', changedAt: '2026-09-20T08:00:00Z' },
      ]],
    })
    renderApp('/admin/products')

    await userEvent.click(await screen.findByRole('button', { name: 'Actions for Galaxy S26' }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Price history' }))
    const dialog = await screen.findByRole('dialog')

    expect(await within(dialog).findByText('MAD 9,500.00')).toBeInTheDocument()
    expect(within(dialog).getByText('MAD 9,999.00')).toBeInTheDocument()
    expect(within(dialog).getByText('admin')).toBeInTheDocument()
  })

  it("shows each product's cover picture next to its name", async () => {
    loggedInAs('CASHIER')
    mockApi({ 'GET /api/products': [200, page([phoneWithPictures, cable])], 'GET /api/categories': [200, []] })
    const { container } = renderApp('/admin/products')

    await screen.findByText('Galaxy S26')
    expect(container.querySelector('img[src="/api/images/8"]')).toBeInTheDocument()
    // Only the cover is shown in the list, and a product without pictures gets a placeholder
    expect(container.querySelector('img[src="/api/images/9"]')).not.toBeInTheDocument()
    expect(within(screen.getByText('USB-C Cable').closest('tr')!).queryByRole('presentation')).not.toBeInTheDocument()
  })

  it('lets an admin add pictures, shrunk before sending, and choose the cover', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/products': [200, page([phoneWithPictures])],
      'GET /api/categories': [200, []],
      'GET /api/products/1': [200, phoneWithPictures],
      'POST /api/products/1/images': [201, picture(10)],
      'PUT /api/products/1/images/9/cover': [200, [picture(9), picture(8)]],
    })
    renderApp('/admin/products')

    await userEvent.click(await screen.findByRole('button', { name: 'Actions for Galaxy S26' }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Pictures' }))
    const dialog = await screen.findByRole('dialog')
    expect(within(dialog).getByAltText('Picture 1 of Galaxy S26')).toHaveAttribute('src', '/api/images/8')
    expect(within(dialog).getByText('Cover')).toBeInTheDocument()

    const photo = new File(['photo'], 'photo.png', { type: 'image/png' })
    await userEvent.upload(dialog.querySelector('input[type="file"]')!, photo)
    await waitFor(() => expect(calls.some((c) => c.method === 'POST')).toBe(true))
    expect(prepareImage).toHaveBeenCalledWith(photo)
    const sent = calls.find((c) => c.method === 'POST')!.body as FormData
    expect(sent.get('file')).toBeInstanceOf(Blob)

    await userEvent.click(within(dialog).getByRole('button', { name: 'Use picture 2 as cover' }))
    await waitFor(() => expect(calls.some((c) => c.method === 'PUT' && c.url === '/api/products/1/images/9/cover')).toBe(true))
  })

  it('lets an admin hide a product from the online shop, and marks it in the list', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/products': [200, page([{ ...phone, published: true }, { ...cable, published: false }])],
      'GET /api/categories': [200, []],
      'PUT /api/products/1': [200, { ...phone, published: false }],
    })
    renderApp('/admin/products')

    expect(within((await screen.findByText('USB-C Cable')).closest('tr')!).getByText('Hidden from shop')).toBeInTheDocument()
    expect(within(screen.getByText('Galaxy S26').closest('tr')!).queryByText('Hidden from shop')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Actions for Galaxy S26' }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Edit' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.click(within(dialog).getByRole('switch', { name: /Show in the online shop/ }))
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }))

    await waitFor(() => expect(calls.find((c) => c.method === 'PUT')?.body).toMatchObject({ published: false }))
  })

  it('shows no management actions to a cashier', async () => {
    loggedInAs('CASHIER')
    mockApi({ 'GET /api/products': [200, page([phone])] })
    renderApp('/admin/products')

    expect(await screen.findByText('Galaxy S26')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'New product' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Actions for Galaxy S26' })).not.toBeInTheDocument()
  })
})

describe('new sale', () => {
  const shop = {
    'GET /api/customers': [200, page([ahmed])] as [number, unknown],
    'GET /api/products': [200, page([phone, cable])] as [number, unknown],
  }

  async function fillCart() {
    await userEvent.click(await screen.findByLabelText('Customer', { selector: 'input' }))
    await userEvent.click(await screen.findByRole('option', { name: 'Ahmed (ahmed@test.com)' }))
    await userEvent.click(screen.getByLabelText('Add a product', { selector: 'input' }))
    await userEvent.click(await screen.findByRole('option', { name: /Galaxy S26/ }))
    await userEvent.click(screen.getByLabelText('Add a product', { selector: 'input' }))
    await userEvent.click(await screen.findByRole('option', { name: /USB-C Cable/ }))
    await userEvent.clear(screen.getByLabelText('Quantity of USB-C Cable'))
    await userEvent.type(screen.getByLabelText('Quantity of USB-C Cable'), '2')
  }

  it('records the whole sale in one request and shows the total', async () => {
    loggedInAs('CASHIER')
    const calls = mockApi({
      ...shop,
      'POST /api/sales': [201, { id: 42, customer: { id: 7, name: 'Ahmed' }, saleDate: '2026-09-30T10:00:00Z', items: [{}, {}], total: 9599.8 }],
    })
    renderApp('/admin/sales/new')

    await fillCart()
    expect(screen.getByText(/Total: MAD\s9,599\.80/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Complete sale' }))

    expect(await screen.findByText('Sale #42 recorded')).toBeInTheDocument()
    const sales = calls.filter((c) => c.method === 'POST' && c.url === '/api/sales')
    expect(sales).toHaveLength(1)
    expect(sales[0].body).toEqual({ customerId: 7, items: [{ productId: 1, quantity: 1 }, { productId: 2, quantity: 2 }] })
    expect(calls.some((c) => c.url === '/api/sale-items')).toBe(false)
  })

  it('keeps the cart and explains why when the API refuses the sale', async () => {
    loggedInAs('CASHIER')
    mockApi({ ...shop, 'POST /api/sales': [409, { error: "Not enough stock for product 'Galaxy S26': 0 available, 1 requested" }] })
    renderApp('/admin/sales/new')

    await fillCart()
    await userEvent.click(screen.getByRole('button', { name: 'Complete sale' }))

    expect(await screen.findByText("Not enough stock for product 'Galaxy S26': 0 available, 1 requested")).toBeInTheDocument()
    expect(screen.getByLabelText('Quantity of USB-C Cable')).toBeInTheDocument()
  })

  it('says why the customer list is empty when the API fails', async () => {
    loggedInAs('CASHIER')
    mockApi({ ...shop, 'GET /api/customers': [500, { error: 'Internal server error' }] })
    renderApp('/admin/sales/new')

    await userEvent.click(await screen.findByLabelText('Customer', { selector: 'input' }))

    expect(await screen.findByText('Could not load: Internal server error')).toBeInTheDocument()
  })

  it('highlights only "New sale" in the menu on the new sale page', async () => {
    loggedInAs('CASHIER')
    mockApi(shop)
    renderApp('/admin/sales/new')

    expect(await screen.findByRole('link', { name: 'New sale' })).toHaveAttribute('data-active')
    const sales = screen.getByRole('link', { name: 'Sales' })
    expect(sales).not.toHaveAttribute('data-active')
    // React Router's NavLink would also mark /sales as current on /sales/new
    expect(sales).not.toHaveAttribute('aria-current')
  })

  it('tells the cashier when a product price was just reduced', async () => {
    loggedInAs('CASHIER')
    mockApi({ ...shop, 'GET /api/products': [200, page([reducedPhone])] })
    renderApp('/admin/sales/new')

    await userEvent.click(await screen.findByLabelText('Add a product', { selector: 'input' }))

    expect(await screen.findByRole('option', { name: /Galaxy S26 · MAD.9,500\.00 \(was MAD.9,999\.00\)/ })).toBeInTheDocument()
  })

  it("can't be completed without a customer", async () => {
    loggedInAs('CASHIER')
    mockApi(shop)
    renderApp('/admin/sales/new')

    await userEvent.click(await screen.findByLabelText('Add a product', { selector: 'input' }))
    await userEvent.click(await screen.findByRole('option', { name: /USB-C Cable/ }))

    expect(screen.getByRole('button', { name: 'Complete sale' })).toBeDisabled()
  })
})

describe('categories', () => {
  it('lets an admin add a category, showing a duplicate name under the field', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/categories': [200, [phones]],
      'POST /api/categories': [409, { error: "A category named 'phones' already exists" }],
    })
    renderApp('/admin/categories')

    await userEvent.click(await screen.findByRole('button', { name: 'New category' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText('Name'), 'phones')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add category' }))

    expect(await within(dialog).findByText("A category named 'phones' already exists")).toBeInTheDocument()
    expect(calls.find((c) => c.method === 'POST')?.body).toEqual({ name: 'phones' })
  })

  it('asks before deleting a category, and keeps it when the API refuses', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/categories': [200, [phones]],
      'DELETE /api/categories/3': [409, { error: "Category 'Phones' cannot be deleted: it has 1 product(s)" }],
    })
    renderApp('/admin/categories')

    await userEvent.click(await screen.findByRole('button', { name: 'Delete Phones' }))
    await userEvent.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Delete' }))

    await waitFor(() => expect(calls.some((c) => c.method === 'DELETE' && c.url === '/api/categories/3')).toBe(true))
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument())
    expect(screen.getByRole('cell', { name: 'Phones' })).toBeInTheDocument()
  })

  it('opens the products of a category from its product count', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/categories': [200, [phones]],
      'GET /api/products': [200, page([phoneInCategory])],
    })
    const { router } = renderApp('/admin/categories')

    await userEvent.click(await screen.findByRole('link', { name: '1 product' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/admin/products'))
    expect(router.state.location.search).toBe('?category=3')
    await waitFor(() => expect(calls.some((c) => c.url.startsWith('/api/products?') && c.url.includes('categoryId=3'))).toBe(true))
  })

  it('keeps cashiers out of category management', async () => {
    loggedInAs('CASHIER')
    const calls = mockApi({ 'GET /api/products/low-stock': [200, page([])] })
    const { router } = renderApp('/admin/categories')

    await waitFor(() => expect(router.state.location.pathname).toBe('/admin'))
    expect(screen.queryByRole('link', { name: 'Categories' })).not.toBeInTheDocument()
    expect(calls.some((c) => c.url.startsWith('/api/categories'))).toBe(false)
  })
})

describe('online orders', () => {
  const summary = {
    id: 21, orderNumber: 'TS-7K3F9Q', status: 'NEW', placedAt: '2026-10-06T19:30:00Z', customerName: 'Imane Alaoui',
    phone: '+212611223344', city: 'Rabat', itemCount: 2, total: 129.8,
  }
  const detail = {
    id: 21, orderNumber: 'TS-7K3F9Q', status: 'NEW', placedAt: '2026-10-06T19:30:00Z', customerId: 9,
    delivery: { name: 'Imane Alaoui', phone: '+212611223344', city: 'Rabat', address: '14 avenue Mohammed V', note: 'Call first' },
    items: [{ id: 5, saleId: 21, product: { id: 2, name: 'USB-C Cable' }, quantity: 2, unitPrice: 49.9, lineTotal: 99.8 }],
    subtotal: 99.8, deliveryFee: 30, total: 129.8,
    history: [{ status: 'NEW', note: 'Ordered online', changedBy: 'online shop', changedAt: '2026-10-06T19:30:00Z' }],
    nextStatuses: ['CONFIRMED', 'CANCELLED'],
  }

  it('lists the orders to confirm and confirms one after the call', async () => {
    loggedInAs('CASHIER')
    const calls = mockApi({
      'GET /api/orders': [200, page([summary])],
      'GET /api/orders/counts': [200, { NEW: 1, CONFIRMED: 0, SHIPPED: 0 }],
      'GET /api/orders/21': [200, detail],
      'POST /api/orders/21/status': [200, { ...detail, status: 'CONFIRMED', nextStatuses: ['SHIPPED', 'CANCELLED'] }],
    })
    renderApp('/admin/orders')

    // The menu says how many orders wait for a call
    expect(await screen.findByLabelText('1 to confirm')).toBeInTheDocument()
    await userEvent.click(await screen.findByText('TS-7K3F9Q'))
    const panel = await screen.findByRole('dialog')
    expect(within(panel).getByRole('link', { name: /\+212611223344/ })).toHaveAttribute('href', 'tel:+212611223344')
    expect(within(panel).getByText('Note: Call first')).toBeInTheDocument()

    await userEvent.click(within(panel).getByRole('button', { name: 'Confirm (customer called)' }))

    await waitFor(() => expect(calls.find((c) => c.method === 'POST')?.body).toEqual({ status: 'CONFIRMED', note: null }))
    expect(calls.some((c) => c.url.startsWith('/api/orders?') && c.url.includes('status=NEW'))).toBe(true)
    // Every call carries the token, the menu's count included (it's asked for before the page loads)
    expect(calls.every((c) => c.auth === 'Bearer token-123')).toBe(true)
  })

  it('cancels an order with a note', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/orders': [200, page([summary])],
      'GET /api/orders/counts': [200, { NEW: 1 }],
      'GET /api/orders/21': [200, detail],
      'POST /api/orders/21/status': [200, { ...detail, status: 'CANCELLED', nextStatuses: [] }],
    })
    renderApp('/admin/orders')

    await userEvent.click(await screen.findByText('TS-7K3F9Q'))
    const panel = await screen.findByRole('dialog')
    await userEvent.type(within(panel).getByLabelText('Note'), 'Wrong address')
    await userEvent.click(within(panel).getByRole('button', { name: 'Cancel the order' }))

    await waitFor(() => expect(calls.find((c) => c.method === 'POST')?.body).toEqual({ status: 'CANCELLED', note: 'Wrong address' }))
  })

  it('marks online orders in the sales list, which are cancelled from the orders page instead', async () => {
    loggedInAs('ADMIN')
    const online = { id: 21, customer: { id: 9, name: 'Imane Alaoui' }, saleDate: '2026-10-06T19:30:00Z', items: [], total: 99.8,
      channel: 'ONLINE', status: 'DELIVERED', orderNumber: 'TS-7K3F9Q' }
    const counter = { ...online, id: 20, customer: { id: 7, name: 'Ahmed' }, channel: 'STORE', status: 'COMPLETED', orderNumber: null }
    mockApi({ 'GET /api/sales': [200, page([online, counter])] })
    renderApp('/admin/sales')

    expect(await screen.findByText('Online · Delivered')).toBeInTheDocument()
    expect(screen.getByText('Counter')).toBeInTheDocument()
    await userEvent.click(screen.getByText('Imane Alaoui'))
    const drawer = await screen.findByRole('dialog')
    expect(within(drawer).getByText(/handled on the Online orders page/)).toBeInTheDocument()
    expect(within(drawer).queryByRole('button', { name: 'Cancel this sale' })).not.toBeInTheDocument()
  })
})

describe('customers and users', () => {
  it('shows a duplicate email under the email field', async () => {
    loggedInAs('CASHIER')
    mockApi({
      'GET /api/customers': [200, page([ahmed])],
      'POST /api/customers': [409, { error: 'Email ahmed@test.com is already used by another customer' }],
    })
    renderApp('/admin/customers')

    await userEvent.click(await screen.findByRole('button', { name: 'New customer' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Ahmed 2')
    await userEvent.type(within(dialog).getByLabelText('Email (optional)'), 'ahmed@test.com')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add customer' }))

    expect(await within(dialog).findByText('Email ahmed@test.com is already used by another customer')).toBeInTheDocument()
  })

  it('keeps cashiers out of user management', async () => {
    loggedInAs('CASHIER')
    const calls = mockApi({ 'GET /api/products/low-stock': [200, page([])] })
    const { router } = renderApp('/admin/users')

    await waitFor(() => expect(router.state.location.pathname).toBe('/admin'))
    expect(screen.queryByRole('link', { name: 'Users' })).not.toBeInTheDocument()
    expect(calls.some((c) => c.url.startsWith('/api/users'))).toBe(false)
  })

  it('lets an admin create a cashier account', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/users': [200, page([{ id: 1, username: 'admin', role: 'ADMIN', enabled: true }])],
      'POST /api/users': [201, { id: 2, username: 'sara', role: 'CASHIER', enabled: true }],
    })
    renderApp('/admin/users')

    expect(await screen.findByText('(you)')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'New user' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText('Username'), 'sara')
    await userEvent.type(within(dialog).getByLabelText('Password'), 'sara-password-1')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create user' }))

    await waitFor(() => expect(calls.find((c) => c.method === 'POST')?.body)
      .toEqual({ username: 'sara', password: 'sara-password-1', role: 'CASHIER' }))
  })
})
