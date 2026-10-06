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
const NewSalePage = lazy(() => import('./pages/NewSalePage').then((m) => ({ default: m.NewSalePage })))
const SalesPage = lazy(() => import('./pages/SalesPage').then((m) => ({ default: m.SalesPage })))
const CategoriesPage = lazy(() => import('./pages/CategoriesPage').then((m) => ({ default: m.CategoriesPage })))
const CustomersPage = lazy(() => import('./pages/CustomersPage').then((m) => ({ default: m.CustomersPage })))
const UsersPage = lazy(() => import('./pages/UsersPage').then((m) => ({ default: m.UsersPage })))
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
      { path: 'sales/new', element: <NewSalePage /> },
      { path: 'sales', element: <SalesPage /> },
      { path: 'products', element: <ProductsPage /> },
      { path: 'categories', element: <RequireAuth adminOnly><CategoriesPage /></RequireAuth> },
      { path: 'customers', element: <CustomersPage /> },
      { path: 'stock-history', element: <StockHistoryPage /> },
      { path: 'users', element: <RequireAuth adminOnly><UsersPage /></RequireAuth> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]

export const router = createBrowserRouter(routes)
