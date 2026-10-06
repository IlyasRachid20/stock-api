import { describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { mockApi, renderApp } from '../test/render'
import { CART_KEY, type CartLine } from './cart/useCart'

const page = <T,>(content: T[]) => ({ content, page: { size: 12, number: 0, totalElements: content.length, totalPages: 1 } })
const phones = { id: 1, slug: 'phones', name: 'Phones' }
const protection = { id: 4, slug: 'protection', name: 'Protection' }
const galaxy = {
  id: 1, slug: 'galaxy-s26', name: 'Galaxy S26', category: phones, price: 9500, previousPrice: 9999,
  availability: 'IN_STOCK', onlyLeft: null, maxQuantity: 10, imageUrl: '/api/images/1',
}
const phoneCase = {
  id: 8, slug: 'phone-case', name: 'Phone Case', category: protection, price: 129, previousPrice: null,
  availability: 'FEW_LEFT', onlyLeft: 2, maxQuantity: 2, imageUrl: null,
}
const iphone = {
  id: 2, slug: 'iphone-17', name: 'iPhone 17', category: phones, price: 12900, previousPrice: null,
  availability: 'OUT_OF_STOCK', onlyLeft: null, maxQuantity: 0, imageUrl: '/api/images/2',
}
const categories = [
  { ...phones, productCount: 3, imageUrl: '/api/images/1' },
  { ...protection, productCount: 2, imageUrl: null },
]
const shopApi = {
  'GET /api/shop/categories': [200, categories] as [number, unknown],
  'GET /api/shop/home': [200, { categories, deals: [galaxy], bestSellers: [phoneCase, iphone] }] as [number, unknown],
  'GET /api/shop/products': [200, page([galaxy, iphone])] as [number, unknown],
}
const savedCart = (): CartLine[] => JSON.parse(localStorage.getItem(CART_KEY) ?? '[]')

describe('online shop', () => {
  it('shows the home page to any visitor, without logging in', async () => {
    const calls = mockApi(shopApi)
    renderApp('/')

    expect(await screen.findByRole('heading', { name: /Phones and accessories/ })).toBeInTheDocument()
    const deals = (await screen.findByRole('heading', { name: 'Deals' })).closest('section')!
    expect(within(deals).getByText('Galaxy S26')).toBeInTheDocument()
    expect(within(deals).getByText('-5%')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Protection 2 products/ })).toHaveAttribute('href', '/shop?category=protection')
    // Only the public catalog is called, and without a token
    expect(calls.every((c) => c.url.startsWith('/api/shop/') && c.auth === null)).toBe(true)
  })

  it('adds a product to the cart, and keeps the cart after a reload', async () => {
    mockApi(shopApi)
    const { unmount } = renderApp('/')

    await userEvent.click(await screen.findByRole('button', { name: 'Add Galaxy S26 to the cart' }))

    const drawer = await screen.findByRole('dialog', { name: 'Your cart (1)' })
    expect(within(drawer).getByText('Galaxy S26')).toBeInTheDocument()
    expect(within(drawer).getAllByText(/MAD.9,500\.00/).length).toBeGreaterThan(0)
    expect(savedCart()).toMatchObject([{ productId: 1, quantity: 1, price: 9500 }])

    unmount()
    renderApp('/')
    expect(await screen.findByRole('button', { name: 'Cart, 1 item' })).toBeInTheDocument()
  })

  it("can't add a product that's out of stock, or more than the shop allows", async () => {
    mockApi(shopApi)
    renderApp('/')

    expect(await screen.findByRole('button', { name: 'iPhone 17 is out of stock' })).toBeDisabled()
    for (let i = 0; i < 3; i++) {
      await userEvent.click(screen.getByRole('button', { name: 'Add Phone Case to the cart' }))
    }
    // Only 2 left in the shop
    expect(savedCart()).toMatchObject([{ productId: 8, quantity: 2 }])
  })

  it('filters by category and sorts, keeping both in the address', async () => {
    const calls = mockApi(shopApi)
    const { router } = renderApp('/shop?category=phones')

    expect(await screen.findByRole('heading', { name: 'Phones' })).toBeInTheDocument()
    await waitFor(() => expect(calls.some((c) => c.url.startsWith('/api/shop/products?') && c.url.includes('category=phones'))).toBe(true))

    await userEvent.click(screen.getByLabelText('Sort by', { selector: 'input' }))
    await userEvent.click(await screen.findByRole('option', { name: 'Price: low to high' }))

    await waitFor(() => expect(router.state.location.search).toBe('?category=phones&sort=price%2Casc'))
    await waitFor(() => expect(calls.some((c) => c.url.includes('sort=price%2Casc'))).toBe(true))
  })

  it('opens a product at its readable address, with its pictures and a quantity to add', async () => {
    mockApi({
      ...shopApi,
      'GET /api/shop/products/1': [200, {
        ...galaxy, imageUrl: undefined, description: 'Triple camera',
        images: [{ id: 1, url: '/api/images/1', width: 800, height: 800 }, { id: 5, url: '/api/images/5', width: 800, height: 800 }],
      }],
    })
    const { router } = renderApp('/p/1')

    expect(await screen.findByRole('heading', { name: 'Galaxy S26' })).toBeInTheDocument()
    await waitFor(() => expect(router.state.location.pathname).toBe('/p/1-galaxy-s26'))
    expect(screen.getByText('Triple camera')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Show picture 2' }))
    expect(screen.getByRole('img', { name: 'Galaxy S26' })).toHaveAttribute('src', '/api/images/5')

    await userEvent.clear(screen.getByRole('textbox', { name: 'Quantity' }))
    await userEvent.type(screen.getByRole('textbox', { name: 'Quantity' }), '2')
    await userEvent.click(screen.getByRole('button', { name: 'Add to cart' }))
    expect(savedCart()).toMatchObject([{ productId: 1, quantity: 2 }])
  })

  it("checks the cart against the shop's latest prices and stock", async () => {
    localStorage.setItem(CART_KEY, JSON.stringify([
      { productId: 1, quantity: 1, name: 'Galaxy S26', slug: 'galaxy-s26', price: 9999, imageUrl: null, maxQuantity: 10 },
      { productId: 99, quantity: 1, name: 'Old Charger', slug: 'old-charger', price: 99, imageUrl: null, maxQuantity: 5 },
    ]))
    const calls = mockApi({ ...shopApi, 'GET /api/shop/products': [200, page([galaxy])] })
    renderApp('/cart')

    // The price dropped since it was added, and one product left the shop
    expect(await screen.findByText(/MAD.9,500\.00 each/)).toBeInTheDocument()
    expect(screen.getByText('No longer available')).toBeInTheDocument()
    expect(screen.getByText('Remove the products that are no longer available to continue.')).toBeInTheDocument()
    expect(calls.some((c) => c.url.includes('ids=1%2C99'))).toBe(true)

    await userEvent.click(screen.getByRole('button', { name: 'Remove Old Charger' }))
    expect(screen.queryByText('No longer available')).not.toBeInTheDocument()
  })

  it('places an order paid cash on delivery, shows the confirmation and empties the cart', async () => {
    localStorage.setItem(CART_KEY, JSON.stringify([
      { productId: 8, quantity: 2, name: 'Phone Case', slug: 'phone-case', price: 129, imageUrl: null, maxQuantity: 2 },
    ]))
    const order = {
      orderNumber: 'TS-7K3F9Q', status: 'NEW', placedAt: '2026-10-06T10:00:00Z',
      items: [{ productId: 8, name: 'Phone Case', quantity: 2, unitPrice: 129, lineTotal: 258 }],
      subtotal: 258, deliveryFee: 30, total: 288,
      delivery: { name: 'Sara El Idrissi', phone: '+212612345678', city: 'Casablanca', address: '12 rue des Fleurs', note: null },
      history: [{ status: 'NEW', at: '2026-10-06T10:00:00Z' }],
    }
    const calls = mockApi({
      ...shopApi,
      'GET /api/shop/info': [200, { currency: 'MAD', deliveryFee: 30, freeDeliveryFrom: 500 }],
      'POST /api/shop/orders': [201, order],
    })
    const { router } = renderApp('/checkout')

    // MAD 258 of products: delivery is paid, and the page says how much more makes it free
    expect(await screen.findByText(/add MAD.242\.00 more/)).toBeInTheDocument()
    expect(screen.getByText(/MAD.288\.00/)).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Full name'), 'Sara El Idrissi')
    await userEvent.type(screen.getByLabelText(/^Phone/), '06 12 34 56 78')
    await userEvent.type(screen.getByLabelText('City'), 'Casablanca')
    await userEvent.type(screen.getByLabelText('Address'), '12 rue des Fleurs')
    await userEvent.click(screen.getByRole('button', { name: 'Place order' }))

    expect(await screen.findByRole('heading', { name: 'Thank you, Sara!' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/order/TS-7K3F9Q')
    expect(savedCart()).toEqual([])
    // Only product ids and quantities are sent: prices come from the database
    expect(calls.find((c) => c.method === 'POST')?.body).toEqual({
      name: 'Sara El Idrissi', phone: '06 12 34 56 78', city: 'Casablanca', address: '12 rue des Fleurs', note: '',
      items: [{ productId: 8, quantity: 2 }],
    })
  })

  it('keeps the cart and explains why when an order is refused', async () => {
    localStorage.setItem(CART_KEY, JSON.stringify([
      { productId: 8, quantity: 2, name: 'Phone Case', slug: 'phone-case', price: 129, imageUrl: null, maxQuantity: 2 },
    ]))
    mockApi({
      ...shopApi,
      'GET /api/shop/info': [200, { currency: 'MAD', deliveryFee: 30, freeDeliveryFrom: 500 }],
      'POST /api/shop/orders': [409, { error: 'Not enough stock for Phone Case: lower the quantity or remove it from your cart' }],
    })
    renderApp('/checkout')

    await userEvent.type(await screen.findByLabelText('Full name'), 'Sara')
    await userEvent.type(screen.getByLabelText(/^Phone/), '0612345678')
    await userEvent.type(screen.getByLabelText('City'), 'Rabat')
    await userEvent.type(screen.getByLabelText('Address'), '1 avenue X')
    await userEvent.click(screen.getByRole('button', { name: 'Place order' }))

    expect(await screen.findByText('Not enough stock for Phone Case: lower the quantity or remove it from your cart')).toBeInTheDocument()
    expect(savedCart()).toHaveLength(1)
  })

  it('tracks an order with its number and the phone used', async () => {
    const calls = mockApi({
      ...shopApi,
      'POST /api/shop/orders/track': [200, {
        orderNumber: 'TS-7K3F9Q', status: 'SHIPPED', placedAt: '2026-10-06T10:00:00Z',
        items: [{ productId: 8, name: 'Phone Case', quantity: 1, unitPrice: 129, lineTotal: 129 }],
        subtotal: 129, deliveryFee: 30, total: 159,
        delivery: { name: 'Sara', phone: '+212612345678', city: 'Rabat', address: '1 avenue X', note: null },
        history: [{ status: 'NEW', at: '2026-10-06T10:00:00Z' }, { status: 'CONFIRMED', at: '2026-10-06T11:00:00Z' },
          { status: 'SHIPPED', at: '2026-10-07T09:00:00Z' }],
      }],
    })
    // Opened from a saved link: the order isn't shown before the phone is given
    renderApp('/order/TS-7K3F9Q')

    expect(await screen.findByLabelText('Order number')).toHaveValue('TS-7K3F9Q')
    await userEvent.type(screen.getByLabelText('Phone'), '0612345678')
    await userEvent.click(screen.getByRole('button', { name: 'Show my order' }))

    expect(await screen.findByText('On its way')).toBeInTheDocument()
    expect(screen.getByText(/MAD.159\.00/)).toBeInTheDocument()
    expect(calls.find((c) => c.method === 'POST')?.body).toEqual({ orderNumber: 'TS-7K3F9Q', phone: '0612345678' })
  })

  it('shows a not-found page for unknown addresses', async () => {
    mockApi(shopApi)
    renderApp('/nothing-here')

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the shop' })).toHaveAttribute('href', '/shop')
  })
})
