import { Anchor, Box, Button, Card, Container, Group, Image, SimpleGrid, Stack, Text, ThemeIcon, Title } from '@mantine/core'
import { useDocumentTitle } from '@mantine/hooks'
import { IconArrowRight, IconCash, IconMail, IconShieldCheck, IconTruckDelivery } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { api } from '../api/client'
import { QueryState } from '../components/QueryState'
import { SHOP } from '../shop'
import { ProductGrid } from './ProductCard'
import { ProductPicture } from './ProductBits'
import type { ShopHome, ShopProduct } from './types'

const FEATURES = [
  { icon: IconCash, title: 'Cash on delivery', text: 'Pay when your parcel arrives' },
  { icon: IconTruckDelivery, title: 'Delivery across Morocco', text: 'We call you to confirm first' },
  { icon: IconShieldCheck, title: 'Two-year warranty', text: 'On every phone we sell' },
  { icon: IconMail, title: 'Questions?', text: SHOP.email },
]

export function HomePage() {
  useDocumentTitle(`${SHOP.name}: ${SHOP.tagline.toLowerCase()}`)
  const home = useQuery({ queryKey: ['shop', 'home'], queryFn: () => api<ShopHome>('/api/shop/home') })
  // The hero shows the three most expensive products on the home page: usually phones
  const showcase = uniquePictures([...(home.data?.bestSellers ?? []), ...(home.data?.deals ?? [])]
    .sort((a, b) => b.price - a.price)).slice(0, 3)

  return (
    <>
      <Box bg="linear-gradient(135deg, #eaf2ff 0%, #f7faff 60%, #ffffff 100%)">
        <Container size="lg" py={{ base: 40, md: 64 }}>
          <SimpleGrid cols={{ base: 1, md: 2 }} spacing={48}>
            <Stack justify="center" gap="lg">
              <Text c="brand.6" fw={700} tt="uppercase" size="sm">New deals every week</Text>
              <Title order={1} fz={{ base: 34, md: 48 }} lh={1.1} style={{ letterSpacing: '-0.02em' }}>
                Phones and accessories, <Text span inherit c="brand.6">delivered to your door</Text>
              </Title>
              <Text c="dimmed" size="lg" maw={460}>
                Order in a minute, pay cash when your parcel arrives. Genuine products at fair prices.
              </Text>
              <Group>
                <Button size="md" component={Link} to="/shop" rightSection={<IconArrowRight size={18} />}>Shop now</Button>
                {home.data?.deals.length ? (
                  <Button size="md" variant="default" component="a" href="#deals">See the deals</Button>
                ) : null}
              </Group>
            </Stack>
            <Showcase pictures={showcase} />
          </SimpleGrid>
        </Container>
      </Box>

      <Container size="lg">
        <Card withBorder radius="lg" mt={-28} p="lg" shadow="sm">
          <SimpleGrid cols={{ base: 1, xs: 2, md: 4 }}>
            {FEATURES.map(({ icon: Icon, title, text }) => (
              <Group key={title} gap="sm" wrap="nowrap">
                <ThemeIcon size={44} radius="xl" variant="light"><Icon size={22} /></ThemeIcon>
                <div>
                  <Text fw={600} size="sm">{title}</Text>
                  <Text c="dimmed" size="xs">{text}</Text>
                </div>
              </Group>
            ))}
          </SimpleGrid>
        </Card>

        <QueryState isPending={home.isPending} error={home.error}>
          {home.data && (
            <Stack gap={56} mt={56}>
              <section aria-labelledby="categories-title">
                <SectionTitle id="categories-title" title="Shop by category" link={{ to: '/shop', label: 'All products' }} />
                <SimpleGrid cols={{ base: 2, sm: 4 }}>
                  {home.data.categories.map((category) => (
                    <Card key={category.id} component={Link} to={`/shop?category=${category.slug}`} withBorder radius="lg" padding="sm">
                      <Card.Section><ProductPicture url={category.imageUrl} alt="" /></Card.Section>
                      <Text fw={600} mt="sm">{category.name}</Text>
                      <Text size="xs" c="dimmed">{category.productCount} {category.productCount === 1 ? 'product' : 'products'}</Text>
                    </Card>
                  ))}
                </SimpleGrid>
              </section>

              {home.data.deals.length > 0 && (
                <section id="deals" aria-labelledby="deals-title" style={{ scrollMarginTop: 90 }}>
                  <SectionTitle id="deals-title" title="Deals" subtitle="Recent price drops: the old price is the lowest of the last 30 days" />
                  <ProductGrid products={home.data.deals} />
                </section>
              )}

              {home.data.bestSellers.length > 0 && (
                <section aria-labelledby="best-title">
                  <SectionTitle id="best-title" title="Best sellers" subtitle="What our customers bought most this month" />
                  <ProductGrid products={home.data.bestSellers} />
                </section>
              )}
            </Stack>
          )}
        </QueryState>
      </Container>
    </>
  )
}

function SectionTitle({ id, title, subtitle, link }: { id: string; title: string; subtitle?: string; link?: { to: string; label: string } }) {
  return (
    <Group justify="space-between" align="flex-end" mb="md">
      <div>
        <Title order={2} id={id} fz={26}>{title}</Title>
        {subtitle && <Text c="dimmed" size="sm">{subtitle}</Text>}
      </div>
      {link && <Anchor component={Link} to={link.to} fw={500} size="sm">{link.label} →</Anchor>}
    </Group>
  )
}

// Three product pictures arranged on a card, for the hero
function Showcase({ pictures }: { pictures: string[] }) {
  if (pictures.length === 0) return null
  return (
    <Box pos="relative" h={{ base: 260, md: 360 }}>
      <Card radius={28} shadow="md" pos="absolute" inset={0} p={0} style={{ overflow: 'hidden' }}>
        <Image src={pictures[0]} alt="" h="100%" fit="cover" />
      </Card>
      {pictures.slice(1).map((url, index) => (
        <Card key={url} radius={20} shadow="lg" p={0} pos="absolute" w={{ base: 110, md: 150 }} h={{ base: 110, md: 150 }}
          style={{ overflow: 'hidden', border: '4px solid white', bottom: -20, ...(index === 0 ? { left: 12 } : { right: 12 }) }}>
          <Image src={url} alt="" h="100%" fit="cover" />
        </Card>
      ))}
    </Box>
  )
}

function uniquePictures(products: ShopProduct[]): string[] {
  return [...new Set(products.map((p) => p.imageUrl).filter((url): url is string => url !== null))]
}
