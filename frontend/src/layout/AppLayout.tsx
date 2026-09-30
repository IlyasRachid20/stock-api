import { Suspense } from 'react'
import { AppShell, Badge, Burger, Button, Center, Group, Loader, NavLink, Text, Title } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { IconBox, IconCashRegister, IconGauge, IconHistory, IconLogout, IconReceipt, IconUserShield, IconUsers } from '@tabler/icons-react'
import { NavLink as RouterLink, Outlet, useLocation } from 'react-router'
import { useAuth } from '../auth/useAuth'

const links = [
  { to: '/', label: 'Dashboard', icon: IconGauge },
  { to: '/sales/new', label: 'New sale', icon: IconCashRegister },
  { to: '/sales', label: 'Sales', icon: IconReceipt },
  { to: '/products', label: 'Products', icon: IconBox },
  { to: '/customers', label: 'Customers', icon: IconUsers },
  { to: '/stock-history', label: 'Stock history', icon: IconHistory },
  { to: '/users', label: 'Users', icon: IconUserShield, adminOnly: true },
]

export function AppLayout() {
  const [opened, { toggle, close }] = useDisclosure()
  const { session, isAdmin, logout } = useAuth()
  const { pathname } = useLocation()

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
            <Title order={3}>Stock API</Title>
          </Group>
          <Group gap="sm">
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
            component={RouterLink}
            to={to}
            label={label}
            leftSection={<Icon size={18} />}
            active={to === '/' || to === '/sales' ? pathname === to : pathname.startsWith(to)}
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
