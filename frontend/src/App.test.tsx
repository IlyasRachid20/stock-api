import { describe, expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { mockApi, renderApp } from './test/render'

const emptyPage = { content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } }

const shopApi = (roles: string[]) => ({
  'POST /api/auth/login': [200, { accessToken: 'token-123', tokenType: 'Bearer', expiresIn: 28800 }] as [number, unknown],
  'GET /api/auth/me': [200, { username: roles[0] === 'ADMIN' ? 'admin' : 'sara', roles }] as [number, unknown],
  'GET /api/products/low-stock': [200, {
    content: [{ id: 8, name: 'Phone Case', price: 129, quantity: 0, minQuantity: 5, lowStock: true }],
    page: { size: 10, number: 0, totalElements: 1, totalPages: 1 },
  }] as [number, unknown],
  'GET /api/reports/summary': [200, { from: '2026-09-01', to: '2026-09-30', salesCount: 4, itemsSold: 12, revenue: 26063.1, averageSale: 6515.78 }] as [number, unknown],
  'GET /api/reports/sales-by-day': [200, [{ date: '2026-09-30', salesCount: 4, itemsSold: 12, revenue: 26063.1 }]] as [number, unknown],
  'GET /api/reports/top-products': [200, [{ productId: 6, name: 'USB-C Cable 1m', quantitySold: 4, revenue: 199.6 }]] as [number, unknown],
  'GET /api/reports/sales-by-category': [200, [
    { categoryId: 1, name: 'Phones', quantitySold: 2, revenue: 25000 },
    { categoryId: null, name: 'Uncategorized', quantitySold: 10, revenue: 1063.1 },
  ]] as [number, unknown],
  'GET /api/products': [200, emptyPage] as [number, unknown],
})

async function logIn(username: string, password: string) {
  await userEvent.type(screen.getByLabelText('Username'), username)
  await userEvent.type(screen.getByLabelText('Password'), password)
  await userEvent.click(screen.getByRole('button', { name: 'Sign in' }))
}

describe('app', () => {
  it('sends visitors without a session to the login page', async () => {
    mockApi({})
    renderApp('/products')

    expect(await screen.findByRole('button', { name: 'Sign in' })).toBeInTheDocument()
  })

  it('logs an admin in and shows the sales reports and low stock', async () => {
    const calls = mockApi(shopApi(['ADMIN']))
    renderApp('/')

    await logIn('admin', 'secret-password')

    expect(await screen.findByText('MAD 26,063.10')).toBeInTheDocument()
    expect(screen.getByText('Phone Case')).toBeInTheDocument()
    expect(screen.getByText('USB-C Cable 1m')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Export CSV' })).toBeInTheDocument()
    // Each category's share of the revenue
    expect(await screen.findByRole('progressbar', { name: 'Phones: 96% of revenue' })).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Uncategorized: 4% of revenue' })).toBeInTheDocument()
    expect(screen.getByText('Admin')).toBeInTheDocument()
    // After login, every call carries the token
    expect(calls.filter((c) => c.url.startsWith('/api/reports')).every((c) => c.auth === 'Bearer token-123')).toBe(true)
  })

  it("doesn't show or load the reports for a cashier", async () => {
    const calls = mockApi(shopApi(['CASHIER']))
    renderApp('/')

    await logIn('sara', 'secret-password')

    expect(await screen.findByText('Phone Case')).toBeInTheDocument()
    expect(screen.getByText('Cashier')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Export CSV' })).not.toBeInTheDocument()
    expect(calls.some((c) => c.url.startsWith('/api/reports'))).toBe(false)
  })

  it("shows the API's message when the login fails", async () => {
    mockApi({ 'POST /api/auth/login': [401, { error: 'Invalid username or password' }] })
    renderApp('/login')

    await logIn('admin', 'wrong')

    expect(await screen.findByText('Invalid username or password')).toBeInTheDocument()
  })

  it('returns to the page asked for after logging in', async () => {
    mockApi(shopApi(['ADMIN']))
    const { router } = renderApp('/products')

    await logIn('admin', 'secret-password')

    await waitFor(() => expect(router.state.location.pathname).toBe('/products'))
    expect(await screen.findByRole('heading', { name: 'Products' })).toBeInTheDocument()
  })

  it('logs out and forgets the session', async () => {
    mockApi(shopApi(['ADMIN']))
    renderApp('/')
    await logIn('admin', 'secret-password')
    await screen.findByText('Phone Case')

    await userEvent.click(screen.getByRole('button', { name: 'Log out' }))

    expect(await screen.findByRole('button', { name: 'Sign in' })).toBeInTheDocument()
    expect(localStorage.getItem('stock-api.session')).toBeNull()
  })
})
