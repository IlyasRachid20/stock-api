import { Suspense } from 'react'
import { AppShell, Badge, Burger, Button, Center, Group, Loader, NavLink, Text } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { useQuery } from '@tanstack/react-query'
import { IconBox, IconBuildingStore, IconCashRegister, IconCategory, IconGauge, IconTruckDelivery, IconHistory, IconLogout, IconReceipt, IconUserShield, IconUsers } from '@tabler/icons-react'
import { Link, Outlet, useLocation } from 'react-router'
import { api } from '../api/client'
import type { OrderStatus } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { Logo } from '../components/Logo'

const links = [
  { to: '/admin', label: 'Dashboard', icon: IconGauge },
  { to: '/admin/sales/new', label: 'New sale', icon: IconCashRegister },
  { to: '/admin/sales', label: 'Sales', icon: IconReceipt },
  { to: '/admin/orders', label: 'Online orders', icon: IconTruckDelivery },
  { to: '/admin/products', label: 'Products', icon: IconBox },
  { to: '/admin/categories', label: 'Categories', icon: IconCategory, adminOnly: true },
  { to: '/admin/customers', label: 'Customers', icon: IconUsers },
  { to: '/admin/stock-history', label: 'Stock history', icon: IconHistory },
  { to: '/admin/users', label: 'Users', icon: IconUserShield, adminOnly: true },
]

export function AppLayout() {
  const [opened, { toggle, close }] = useDisclosure()
  const { session, isAdmin, logout } = useAuth()
  const { pathname } = useLocation()
  // Online orders waiting for a call, checked every minute
  const counts = useQuery({
    queryKey: ['orders', 'counts'],
    queryFn: () => api<Partial<Record<OrderStatus, number>>>('/api/orders/counts'),
    refetchInterval: 60_000,
  })
  const toConfirm = counts.data?.NEW ?? 0

  return (
    <AppShell
      header={{ height: 60 }}
      navbar={{ width: 230, breakpoint: 'sm', collapsed: { mobile: !opened } }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between">
          <Group gap="sm">
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" aria-label="Toggle navigation" />
            <Logo size="sm" subtitle="Back-office" />
          </Group>
          <Group gap="sm">
            <Button component={Link} to="/" variant="subtle" size="xs" leftSection={<IconBuildingStore size={16} />} visibleFrom="sm">
              View shop
            </Button>
            <Text size="sm" visibleFrom="xs">{session?.username}</Text>
            <Badge color={isAdmin ? 'grape' : 'blue'} variant="light">{isAdmin ? 'Admin' : 'Cashier'}</Badge>
            <Button variant="subtle" size="xs" leftSection={<IconLogout size={16} />} onClick={logout}>
              Log out
            </Button>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="sm">
        {links.filter((link) => isAdmin || !link.adminOnly).map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            component={Link}
            to={to}
            label={label}
            leftSection={<Icon size={18} />}
            rightSection={to === '/admin/orders' && toConfirm > 0
              ? <Badge size="sm" color="orange" circle aria-label={`${toConfirm} to confirm`}>{toConfirm}</Badge> : null}
            active={to === '/admin' || to === '/admin/sales' ? pathname === to : pathname.startsWith(to)}
            onClick={close}
          />
        ))}
      </AppShell.Navbar>

      <AppShell.Main>
        <Suspense fallback={<Center py="xl"><Loader /></Center>}>
          <Outlet />
        </Suspense>
      </AppShell.Main>
    </AppShell>
  )
}
