import { Suspense, useState } from 'react'
import { ActionIcon, Anchor, Box, Burger, Center, Container, Drawer, Group, Indicator, Loader, SimpleGrid, Stack, Text, TextInput } from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { IconCash, IconMail, IconSearch, IconShoppingCart, IconTruckDelivery } from '@tabler/icons-react'
import { Link, Outlet, useLocation, useNavigate } from 'react-router'
import { Logo } from '../components/Logo'
import { SHOP } from '../shop'
import { CartProvider } from './cart/CartProvider'
import { useCart } from './cart/useCart'
import { CartDrawer } from './CartDrawer'
import { useShopCategories } from './queries'

const STORE_BACKGROUND = '#f5f8fd'
const YEAR = new Date().getFullYear()

// The online shop's frame: information bar, header (navigation, search, cart), footer
export function StoreLayout() {
  return (
    <CartProvider>
      <Box bg={STORE_BACKGROUND} mih="100vh" style={{ display: 'flex', flexDirection: 'column' }}>
        <Box bg="#0f1b33" c="white" py={8}>
          <Container size="lg">
            <Group justify="space-between" gap="xs">
              <Group gap={6}><IconCash size={16} /><Text size="xs">Pay cash on delivery</Text></Group>
              <Text size="xs" visibleFrom="sm">Welcome to {SHOP.name}: {SHOP.tagline.toLowerCase()}</Text>
              <Group gap={6} visibleFrom="xs"><IconTruckDelivery size={16} /><Text size="xs">Delivery across Morocco</Text></Group>
            </Group>
          </Container>
        </Box>
        <StoreHeader />
        <Box component="main" style={{ flex: 1 }}>
          <Suspense fallback={<Center py={80}><Loader /></Center>}>
            <Outlet />
          </Suspense>
        </Box>
        <StoreFooter />
        <CartDrawer />
      </Box>
    </CartProvider>
  )
}

function StoreHeader() {
  const cart = useCart()
  const navigate = useNavigate()
  const { pathname, search: query } = useLocation()
  const categories = useShopCategories()
  const [menuOpened, menu] = useDisclosure()
  const [search, setSearch] = useState('')

  const submitSearch = (event: React.FormEvent) => {
    event.preventDefault()
    menu.close()
    navigate(search.trim() ? `/shop?search=${encodeURIComponent(search.trim())}` : '/shop')
  }
  const links = [
    { to: '/', label: 'Home' },
    { to: '/shop', label: 'Shop' },
    ...(categories.data ?? []).slice(0, 4).map((c) => ({ to: `/shop?category=${c.slug}`, label: c.name })),
  ]
  const searchBox = (
    <form onSubmit={submitSearch} role="search">
      <TextInput radius="xl" placeholder="Search products…" aria-label="Search products" value={search}
        onChange={(e) => setSearch(e.currentTarget.value)}
        rightSection={<ActionIcon type="submit" variant="subtle" radius="xl" aria-label="Search"><IconSearch size={16} /></ActionIcon>} />
    </form>
  )

  return (
    <Box component="header" bg="white" style={{ position: 'sticky', top: 0, zIndex: 100, borderBottom: '1px solid var(--mantine-color-gray-2)' }}>
      <Container size="lg" py="sm">
        <Group justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger opened={menuOpened} onClick={menu.toggle} hiddenFrom="md" size="sm" aria-label="Open the menu" />
            <Anchor component={Link} to="/" underline="never" aria-label={`${SHOP.name} home`}><Logo size="sm" subtitle="Online store" /></Anchor>
          </Group>
          <Group gap="lg" visibleFrom="md" component="nav" aria-label="Shop">
            {links.map((link) => {
              // A category link is current on that category only; "Shop" on the shop without a category
              const active = link.to.includes('?') ? pathname + query === link.to : pathname === link.to && !query.includes('category=')
              return (
                <Anchor key={link.to} component={Link} to={link.to} fw={500} size="sm" underline="never"
                  c={active ? 'brand.6' : 'dark.7'} aria-current={active ? 'page' : undefined}>
                  {link.label}
                </Anchor>
              )
            })}
          </Group>
          <Group gap="sm" wrap="nowrap">
            <Box visibleFrom="sm" w={240}>{searchBox}</Box>
            <Indicator label={cart.count} size={18} disabled={cart.count === 0} offset={4}>
              <ActionIcon variant="subtle" size="lg" radius="xl" color="dark" onClick={cart.openDrawer}
                aria-label={`Cart, ${cart.count} item${cart.count === 1 ? '' : 's'}`}>
                <IconShoppingCart size={22} />
              </ActionIcon>
            </Indicator>
          </Group>
        </Group>
      </Container>

      <Drawer opened={menuOpened} onClose={menu.close} size="xs" title={<Logo size="sm" />}>
        <Stack>
          {searchBox}
          {links.map((link) => (
            <Anchor key={link.to} component={Link} to={link.to} onClick={menu.close} c="dark.7" fw={500}>{link.label}</Anchor>
          ))}
        </Stack>
      </Drawer>
    </Box>
  )
}

function StoreFooter() {
  const categories = useShopCategories()
  return (
    <Box component="footer" bg="white" mt={64} style={{ borderTop: '1px solid var(--mantine-color-gray-2)' }}>
      <Container size="lg" py={40}>
        <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="xl">
          <Stack gap="xs">
            <Logo size="sm" />
            <Text size="sm" c="dimmed" maw={280}>
              {SHOP.tagline} for everyday life. Order online and pay cash when your parcel arrives.
            </Text>
          </Stack>
          <Stack gap={6}>
            <Text fw={700} size="sm">Shop</Text>
            <Anchor component={Link} to="/shop" size="sm" c="dimmed">All products</Anchor>
            {(categories.data ?? []).map((c) => (
              <Anchor key={c.id} component={Link} to={`/shop?category=${c.slug}`} size="sm" c="dimmed">{c.name}</Anchor>
            ))}
          </Stack>
          <Stack gap={6}>
            <Text fw={700} size="sm">Help</Text>
            <Group gap={6}><IconCash size={16} color="var(--mantine-color-dimmed)" /><Text size="sm" c="dimmed">Cash on delivery</Text></Group>
            <Group gap={6}><IconMail size={16} color="var(--mantine-color-dimmed)" /><Text size="sm" c="dimmed">{SHOP.email}</Text></Group>
            <Anchor component={Link} to="/admin" size="sm" c="dimmed">Staff login</Anchor>
          </Stack>
        </SimpleGrid>
        <Text size="xs" c="dimmed" mt={32}>
          © {YEAR} {SHOP.name} · A demo shop: Spring Boot, PostgreSQL and React.
        </Text>
      </Container>
    </Box>
  )
}
