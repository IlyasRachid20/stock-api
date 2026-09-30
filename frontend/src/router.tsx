// This file holds the route configuration, not components meant for hot reload
/* oxlint-disable react/only-export-components */
import { lazy } from 'react'
import { createBrowserRouter } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { AppLayout } from './layout/AppLayout'
import { LoginPage } from './pages/LoginPage'

// Each page is downloaded the first time it's opened, so the login page stays small
// and the charts library only loads with the dashboard
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const ProductsPage = lazy(() => import('./pages/ProductsPage').then((m) => ({ default: m.ProductsPage })))
const StockHistoryPage = lazy(() => import('./pages/StockHistoryPage').then((m) => ({ default: m.StockHistoryPage })))
const NotFoundPage = lazy(() => import('./pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })))

export const routes = [
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <AppLayout />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'products', element: <ProductsPage /> },
      { path: 'stock-history', element: <StockHistoryPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
