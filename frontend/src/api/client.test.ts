import { describe, expect, it, vi } from 'vitest'
import { mockApi } from '../test/render'
import { api, ApiError, buildUrl, configureApi } from './client'

describe('api client', () => {
  it('builds query strings and skips empty values', () => {
    expect(buildUrl('/api/products', { search: 'galaxy', page: 0, type: null, x: '' })).toBe('/api/products?search=galaxy&page=0')
    expect(buildUrl('/api/products')).toBe('/api/products')
  })

  it('sends the token and JSON body', async () => {
    configureApi({ getToken: () => 'abc', onUnauthorized: () => {} })
    const calls = mockApi({ 'POST /api/customers': [201, { id: 1, name: 'Ahmed' }] })

    const customer = await api<{ id: number }>('/api/customers', { method: 'POST', body: { name: 'Ahmed' } })

    expect(customer.id).toBe(1)
    expect(calls[0]).toMatchObject({ method: 'POST', auth: 'Bearer abc', body: { name: 'Ahmed' } })
  })

  it("turns the API's error body into an ApiError with its message", async () => {
    configureApi({ getToken: () => null, onUnauthorized: () => {} })
    mockApi({ 'POST /api/sale-items': [409, { error: 'Not enough stock' }] })

    await expect(api('/api/sale-items', { method: 'POST', body: {} })).rejects.toMatchObject({
      name: 'ApiError',
      status: 409,
      message: 'Not enough stock',
    })
  })

  it('keeps validation errors per field', async () => {
    configureApi({ getToken: () => null, onUnauthorized: () => {} })
    mockApi({ 'POST /api/products': [400, { errors: { price: 'must be greater than or equal to 0.00' } }] })

    const error = await api('/api/products', { method: 'POST', body: {} }).catch((e: ApiError) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).fieldErrors.price).toBe('must be greater than or equal to 0.00')
  })

  it('reports a 401 on a request made with a token (expired session)', async () => {
    const onUnauthorized = vi.fn()
    configureApi({ getToken: () => 'expired', onUnauthorized })
    mockApi({ 'GET /api/products': [401, { error: 'Invalid or expired token' }] })

    await expect(api('/api/products')).rejects.toBeInstanceOf(ApiError)
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('returns nothing for 204 No Content', async () => {
    configureApi({ getToken: () => 'abc', onUnauthorized: () => {} })
    mockApi({ 'DELETE /api/products/1': [204, null] })

    await expect(api('/api/products/1', { method: 'DELETE' })).resolves.toBeUndefined()
  })
})
