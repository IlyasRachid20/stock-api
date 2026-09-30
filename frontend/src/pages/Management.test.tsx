import { describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { mockApi, renderApp } from '../test/render'

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

describe('products', () => {
  it('lets an admin create a product, showing the API validation errors under the fields', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/products': [200, page([phone])],
      'POST /api/products': [400, { errors: { price: 'must be greater than or equal to 0.00' } }],
    })
    renderApp('/products')

    await userEvent.click(await screen.findByRole('button', { name: 'New product' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText('Name'), 'Charger')
    await userEvent.type(within(dialog).getByLabelText('Price (MAD)'), '199')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Create' }))

    expect(await within(dialog).findByText('must be greater than or equal to 0.00')).toBeInTheDocument()
    expect(calls.find((c) => c.method === 'POST')?.body).toEqual({ name: 'Charger', price: 199, minQuantity: 0, quantity: 0 })
  })

  it('lets an admin restock a product', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/products': [200, page([phone])],
      'POST /api/products/1/restock': [200, { ...phone, quantity: 13 }],
    })
    renderApp('/products')

    await userEvent.click(await screen.findByRole('button', { name: 'Actions for Galaxy S26' }))
    await userEvent.click(await screen.findByRole('menuitem', { name: 'Restock' }))
    const dialog = await screen.findByRole('dialog')
    await userEvent.type(within(dialog).getByLabelText('Quantity received'), '10')
    await userEvent.type(within(dialog).getByLabelText('Reason (optional)'), 'Delivery #42')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Add to stock' }))

    await waitFor(() => expect(calls.find((c) => c.url === '/api/products/1/restock')?.body)
      .toEqual({ quantity: 10, reason: 'Delivery #42' }))
  })

  it('shows no management actions to a cashier', async () => {
    loggedInAs('CASHIER')
    mockApi({ 'GET /api/products': [200, page([phone])] })
    renderApp('/products')

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
    renderApp('/sales/new')

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
    renderApp('/sales/new')

    await fillCart()
    await userEvent.click(screen.getByRole('button', { name: 'Complete sale' }))

    expect(await screen.findByText("Not enough stock for product 'Galaxy S26': 0 available, 1 requested")).toBeInTheDocument()
    expect(screen.getByLabelText('Quantity of USB-C Cable')).toBeInTheDocument()
  })

  it('says why the customer list is empty when the API fails', async () => {
    loggedInAs('CASHIER')
    mockApi({ ...shop, 'GET /api/customers': [500, { error: 'Internal server error' }] })
    renderApp('/sales/new')

    await userEvent.click(await screen.findByLabelText('Customer', { selector: 'input' }))

    expect(await screen.findByText('Could not load: Internal server error')).toBeInTheDocument()
  })

  it('highlights only "New sale" in the menu on the new sale page', async () => {
    loggedInAs('CASHIER')
    mockApi(shop)
    renderApp('/sales/new')

    expect(await screen.findByRole('link', { name: 'New sale' })).toHaveAttribute('data-active')
    const sales = screen.getByRole('link', { name: 'Sales' })
    expect(sales).not.toHaveAttribute('data-active')
    // React Router's NavLink would also mark /sales as current on /sales/new
    expect(sales).not.toHaveAttribute('aria-current')
  })

  it("can't be completed without a customer", async () => {
    loggedInAs('CASHIER')
    mockApi(shop)
    renderApp('/sales/new')

    await userEvent.click(await screen.findByLabelText('Add a product', { selector: 'input' }))
    await userEvent.click(await screen.findByRole('option', { name: /USB-C Cable/ }))

    expect(screen.getByRole('button', { name: 'Complete sale' })).toBeDisabled()
  })
})

describe('customers and users', () => {
  it('shows a duplicate email under the email field', async () => {
    loggedInAs('CASHIER')
    mockApi({
      'GET /api/customers': [200, page([ahmed])],
      'POST /api/customers': [409, { error: 'Email ahmed@test.com is already used by another customer' }],
    })
    renderApp('/customers')

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
    const { router } = renderApp('/users')

    await waitFor(() => expect(router.state.location.pathname).toBe('/'))
    expect(screen.queryByRole('link', { name: 'Users' })).not.toBeInTheDocument()
    expect(calls.some((c) => c.url.startsWith('/api/users'))).toBe(false)
  })

  it('lets an admin create a cashier account', async () => {
    loggedInAs('ADMIN')
    const calls = mockApi({
      'GET /api/users': [200, page([{ id: 1, username: 'admin', role: 'ADMIN', enabled: true }])],
      'POST /api/users': [201, { id: 2, username: 'sara', role: 'CASHIER', enabled: true }],
    })
    renderApp('/users')

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
