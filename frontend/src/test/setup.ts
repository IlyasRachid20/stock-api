import '@testing-library/jest-dom/vitest'
import { afterEach, vi } from 'vitest'
import { cleanup, configure } from '@testing-library/react'

// Pages are loaded on first use (see router.tsx); the dashboard brings the charts library,
// which takes more than the default 1 s to load the first time in the test environment
// (App.test.tsx also loads it before its tests). Waiting longer costs nothing when the element is there.
configure({ asyncUtilTimeout: 10_000 })

afterEach(() => {
  cleanup()
  localStorage.clear()
  vi.restoreAllMocks()
})

// jsdom doesn't implement these browser APIs, which Mantine uses
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }),
})

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver
window.HTMLElement.prototype.scrollIntoView = () => {}
