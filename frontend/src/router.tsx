// This file holds the route configuration, not components meant for hot reload
/* oxlint-disable react/only-export-components */
import { lazy } from 'react'
import { createBrowserRouter } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { AppLayout } from './layout/AppLayout'
import { LoginPage } from './pages/LoginPage'
import { StoreLayout } from './store/StoreLayout'

// Each page is downloaded the first time it's opened, so the login page stays small
// and the charts library only loads with the dashboard
// The online shop's pages
const HomePage = lazy(() => import('./store/HomePage').then((m) => ({ default: m.HomePage })))
const ShopPage = lazy(() => import('./store/ShopPage').then((m) => ({ default: m.ShopPage })))
const ProductPage = lazy(() => import('./store/ProductPage').then((m) => ({ default: m.ProductPage })))
const CartPage = lazy(() => import('./store/CartPage').then((m) => ({ default: m.CartPage })))
const CheckoutPage = lazy(() => import('./store/CheckoutPage').then((m) => ({ default: m.CheckoutPage })))
const OrderPage = lazy(() => import('./store/OrderPage').then((m) => ({ default: m.OrderPage })))
const TrackPage = lazy(() => import('./store/OrderPage').then((m) => ({ default: m.TrackPage })))
const StoreNotFoundPage = lazy(() => import('./store/StoreNotFoundPage').then((m) => ({ default: m.StoreNotFoundPage })))

// The staff area
const DashboardPage = lazy(() => import('./pages/DashboardPage').then((m) => ({ default: m.DashboardPage })))
const ProductsPage = lazy(() => import('./pages/ProductsPage').then((m) => ({ default: m.ProductsPage })))
const StockHistoryPage = lazy(() => import('./pages/StockHistoryPage').then((m) => ({ default: m.StockHistoryPage })))
const NewSalePage = lazy(() => import('./pages/NewSalePage').then((m) => ({ default: m.NewSalePage })))
const SalesPage = lazy(() => import('./pages/SalesPage').then((m) => ({ default: m.SalesPage })))
const CategoriesPage = lazy(() => import('./pages/CategoriesPage').then((m) => ({ default: m.CategoriesPage })))
const CustomersPage = lazy(() => import('./pages/CustomersPage').then((m) => ({ default: m.CustomersPage })))
const UsersPage = lazy(() => import('./pages/UsersPage').then((m) => ({ default: m.UsersPage })))
const NotFoundPage = lazy(() => import('./pages/NotFoundPage').then((m) => ({ default: m.NotFoundPage })))

// The online shop at the root of the site, the staff area (back-office) under /admin
export const routes = [
  {
    path: '/',
    element: <StoreLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'shop', element: <ShopPage /> },
      { path: 'p/:idSlug', element: <ProductPage /> },
      { path: 'cart', element: <CartPage /> },
      { path: 'checkout', element: <CheckoutPage /> },
      { path: 'order/:number', element: <OrderPage /> },
      { path: 'track', element: <TrackPage /> },
      { path: '*', element: <StoreNotFoundPage /> },
    ],
  },
  { path: '/admin/login', element: <LoginPage /> },
  {
    path: '/admin',
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
