// Small fetch wrapper: adds the JWT, turns the API's {"error"} / {"errors"} bodies into an ApiError,
// and reports 401s so the app can log the user out when the token expires.

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

type Query = Record<string, string | number | undefined | null>

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  query?: Query
}

let getToken: () => string | null = () => null
let onUnauthorized: () => void = () => {}

export function configureApi(options: { getToken: () => string | null; onUnauthorized: () => void }) {
  getToken = options.getToken
  onUnauthorized = options.onUnauthorized
}

export function buildUrl(path: string, query?: Query): string {
  const params = new URLSearchParams()
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') params.set(key, String(value))
  })
  const qs = params.toString()
  return qs ? `${path}?${qs}` : path
}

async function send(path: string, { method = 'GET', body, query }: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const response = await fetch(buildUrl(path, query), {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!response.ok) {
    throw await toApiError(response, Boolean(token))
  }
  return response
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await send(path, options)
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

// For file downloads such as the CSV export: saves the response under the server's file name
export async function download(path: string, query?: Query): Promise<void> {
  const response = await send(path, { query })
  const disposition = response.headers.get('Content-Disposition') ?? ''
  const fileName = /filename="([^"]+)"/.exec(disposition)?.[1] ?? 'download'
  const url = URL.createObjectURL(await response.blob())
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

export async function toApiError(response: Response, hadToken: boolean): Promise<ApiError> {
  if (response.status === 401 && hadToken) onUnauthorized()
  let data: { error?: string; errors?: Record<string, string> } = {}
  try {
    data = await response.json()
  } catch {
    // no JSON body
  }
  if (data.errors) {
    return new ApiError(response.status, 'Please fix the highlighted fields', data.errors)
  }
  return new ApiError(response.status, data.error ?? `Request failed (${response.status})`)
}
