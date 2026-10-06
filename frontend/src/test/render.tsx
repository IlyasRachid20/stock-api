import type { ReactElement } from 'react'
import { render } from '@testing-library/react'
import { MantineProvider } from '@mantine/core'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { AuthProvider } from '../auth/AuthContext'
import { routes } from '../router'
import { theme } from '../theme'

// Renders the real app (routes, auth, React Query) at a given URL, as in the browser.
// env="test" is Mantine's test mode: no animations or portals, so dropdowns and dialogs open at once.
export function renderApp(url = '/') {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [url] })
  const view = render(
    <MantineProvider theme={theme} env="test">
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </QueryClientProvider>
    </MantineProvider>,
  )
  return { ...view, router }
}

export function renderWithProviders(ui: ReactElement) {
  return render(<MantineProvider theme={theme} env="test">{ui}</MantineProvider>)
}

// Replaces fetch with a fake API: routes are "METHOD /path" -> [status, JSON body]
export function mockApi(routes: Record<string, [number, unknown]>) {
  const calls: { method: string; url: string; body: unknown; auth: string | null }[] = []
  globalThis.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input)
    const method = init?.method ?? 'GET'
    const headers = (init?.headers ?? {}) as Record<string, string>
    const sent = init?.body instanceof FormData ? init.body : init?.body ? JSON.parse(String(init.body)) : undefined
    calls.push({ method, url, body: sent, auth: headers.Authorization ?? null })
    const key = `${method} ${url.split('?')[0]}`
    const [status, body] = routes[key] ?? [404, { error: `No mock for ${key}` }]
    return new Response(status === 204 ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    })
  }) as typeof fetch
  return calls
}
