import { useEffect } from 'react'
import { ActionIcon, Alert, Anchor, Button, Card, Container, Divider, Grid, Group, NumberInput, Stack, Text, Title } from '@mantine/core'
import { useDocumentTitle } from '@mantine/hooks'
import { IconShoppingBag, IconTrash } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { api } from '../api/client'
import type { Page } from '../api/types'
import { SHOP } from '../shop'
import { formatMoney } from '../utils/format'
import { useCart } from './cart/useCart'
import { AvailabilityText, ProductPicture } from './ProductBits'
import { productPath, type ShopProduct } from './types'

// The cart, checked against the shop's latest prices and stock
export function CartPage() {
  useDocumentTitle(`Your cart · ${SHOP.name}`)
  const cart = useCart()
  const ids = cart.lines.map((line) => line.productId)
  const latest = useQuery({
    queryKey: ['shop', 'cart', ids],
    queryFn: () => api<Page<ShopProduct>>('/api/shop/products', { query: { ids: ids.join(','), size: 100 } }),
    enabled: ids.length > 0,
  })
  const { refresh } = cart
  useEffect(() => {
    if (latest.data) refresh(latest.data.content)
  }, [latest.data, refresh])

  const productOf = (id: number) => latest.data?.content.find((p) => p.id === id)
  // Gone from the shop (hidden or deleted) or sold out since it was added
  const unavailable = latest.data
    ? cart.lines.filter((line) => (productOf(line.productId)?.availability ?? 'OUT_OF_STOCK') === 'OUT_OF_STOCK')
    : []

  if (cart.lines.length === 0) {
    return (
      <Container size="sm" py={80}>
        <Stack align="center">
          <IconShoppingBag size={56} color="var(--mantine-color-gray-4)" />
          <Title order={1} fz={28}>Your cart is empty</Title>
          <Text c="dimmed">Find something you like in the shop.</Text>
          <Button component={Link} to="/shop" size="md">Start shopping</Button>
        </Stack>
      </Container>
    )
  }

  return (
    <Container size="lg" pt="xl">
      <Title order={1} fz={30} mb="lg">Your cart</Title>
      <Grid gap="xl">
        <Grid.Col span={{ base: 12, md: 8 }}>
          <Card withBorder radius="lg">
            <Stack>
              {cart.lines.map((line, index) => {
                const product = productOf(line.productId)
                return (
                  <div key={line.productId}>
                    {index > 0 && <Divider mb="md" />}
                    <Group wrap="nowrap" align="flex-start">
                      <div style={{ width: 88, flex: 'none', borderRadius: 10, overflow: 'hidden' }}>
                        <ProductPicture url={line.imageUrl} alt="" ratio={1} />
                      </div>
                      <Stack gap={6} style={{ flex: 1 }}>
                        <Anchor component={Link} to={productPath({ id: line.productId, slug: line.slug })} c="dark.8" fw={600}>
                          {line.name}
                        </Anchor>
                        <Text size="sm" c="dimmed">{formatMoney(line.price)} each</Text>
                        {product
                          ? <AvailabilityText availability={product.availability} onlyLeft={product.onlyLeft} />
                          : latest.data && <Text size="sm" c="red.7">No longer available</Text>}
                        <Group gap="xs">
                          <NumberInput size="xs" w={90} min={1} max={Math.max(line.maxQuantity, 1)} value={line.quantity}
                            allowDecimal={false} clampBehavior="strict" aria-label={`Quantity of ${line.name}`}
                            onChange={(value) => cart.setQuantity(line.productId, Number(value) || 1)} />
                          <ActionIcon variant="subtle" color="red" aria-label={`Remove ${line.name}`} onClick={() => cart.remove(line.productId)}>
                            <IconTrash size={16} />
                          </ActionIcon>
                        </Group>
                      </Stack>
                      <Text fw={700}>{formatMoney(line.price * line.quantity)}</Text>
                    </Group>
                  </div>
                )
              })}
            </Stack>
          </Card>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 4 }}>
          <Card withBorder radius="lg">
            <Stack>
              <Text fw={700}>Summary</Text>
              <Group justify="space-between">
                <Text c="dimmed">Items ({cart.count})</Text>
                <Text>{formatMoney(cart.subtotal)}</Text>
              </Group>
              <Group justify="space-between">
                <Text c="dimmed">Delivery</Text>
                <Text size="sm" c="dimmed">At checkout</Text>
              </Group>
              <Divider />
              <Group justify="space-between">
                <Text fw={700}>Subtotal</Text>
                <Text fw={800} fz="xl">{formatMoney(cart.subtotal)}</Text>
              </Group>
              {unavailable.length > 0 && (
                <Alert color="orange" variant="light">
                  Remove the products that are no longer available to continue.
                </Alert>
              )}
              <Button size="md" disabled title="Ordering opens soon">Checkout</Button>
              <Text size="xs" c="dimmed" ta="center">You pay cash when your parcel arrives.</Text>
              <Button variant="subtle" component={Link} to="/shop">Continue shopping</Button>
            </Stack>
          </Card>
        </Grid.Col>
      </Grid>
    </Container>
  )
}
