import { useEffect, useState } from 'react'
import { Anchor, Breadcrumbs, Button, Card, Container, Divider, Grid, Group, Image, NumberInput, SimpleGrid, Stack, Text, Title, UnstyledButton } from '@mantine/core'
import { useDocumentTitle } from '@mantine/hooks'
import { IconCash, IconShoppingCartPlus, IconTruckDelivery } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router'
import { api } from '../api/client'
import type { Page } from '../api/types'
import { QueryState } from '../components/QueryState'
import { SHOP } from '../shop'
import { useCart } from './cart/useCart'
import { ProductGrid } from './ProductCard'
import { AvailabilityText, ProductPicture, ReductionBadge, StorePrice } from './ProductBits'
import { productPath, type ShopProduct, type ShopProductDetail } from './types'

// /p/1-galaxy-s26: the number finds the product, the rest is only there to be readable
export function ProductPage() {
  const { idSlug = '' } = useParams()
  const id = Number.parseInt(idSlug, 10)
  const navigate = useNavigate()

  const product = useQuery({
    queryKey: ['shop', 'product', id],
    queryFn: () => api<ShopProductDetail>(`/api/shop/products/${id}`),
    enabled: Number.isInteger(id),
  })
  useDocumentTitle(product.data ? `${product.data.name} · ${SHOP.name}` : SHOP.name)

  // A renamed product keeps working at its old address, which is then corrected
  useEffect(() => {
    if (product.data && `${product.data.id}-${product.data.slug}` !== idSlug) {
      navigate(productPath(product.data), { replace: true })
    }
  }, [product.data, idSlug, navigate])

  if (!Number.isInteger(id)) {
    return <Container size="lg" py="xl"><Text>This product doesn't exist.</Text></Container>
  }
  return (
    <Container size="lg" pt="xl">
      <QueryState isPending={product.isPending} error={product.error}>
        {product.data && <ProductDetails product={product.data} />}
      </QueryState>
    </Container>
  )
}

function ProductDetails({ product }: { product: ShopProductDetail }) {
  const cart = useCart()
  const [picture, setPicture] = useState(0)
  const [quantity, setQuantity] = useState<number | string>(1)
  const soldOut = product.availability === 'OUT_OF_STOCK'
  const main = product.images[picture] ?? product.images[0]

  return (
    <Stack gap="xl">
      <Breadcrumbs>
        <Anchor component={Link} to="/" size="sm">Home</Anchor>
        {product.category && (
          <Anchor component={Link} to={`/shop?category=${product.category.slug}`} size="sm">{product.category.name}</Anchor>
        )}
        <Text size="sm" c="dimmed">{product.name}</Text>
      </Breadcrumbs>

      <Grid gap={40}>
        <Grid.Col span={{ base: 12, md: 6 }}>
          <Card withBorder radius="lg" p={0} pos="relative" style={{ overflow: 'hidden' }}>
            <ProductPicture url={main?.url ?? null} alt={product.name} ratio={1} fit="contain" />
            <ReductionBadge price={product.price} previousPrice={product.previousPrice} pos="absolute" top={16} left={16} />
          </Card>
          {product.images.length > 1 && (
            <SimpleGrid cols={6} mt="sm" spacing="xs">
              {product.images.map((image, index) => (
                <UnstyledButton key={image.id} onClick={() => setPicture(index)} aria-label={`Show picture ${index + 1}`}
                  aria-pressed={index === picture}
                  style={{ borderRadius: 10, overflow: 'hidden', outline: index === picture ? '2px solid var(--mantine-color-brand-6)' : '1px solid var(--mantine-color-gray-3)' }}>
                  <Image src={image.url} alt="" h={64} fit="cover" />
                </UnstyledButton>
              ))}
            </SimpleGrid>
          )}
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 6 }}>
          <Stack gap="md">
            {product.category && <Text size="sm" c="brand.6" fw={600} tt="uppercase">{product.category.name}</Text>}
            <Title order={1} fz={{ base: 28, md: 36 }} lh={1.15}>{product.name}</Title>
            <StorePrice price={product.price} previousPrice={product.previousPrice} size={30} />
            <AvailabilityText availability={product.availability} onlyLeft={product.onlyLeft} />

            <Group align="flex-end">
              <NumberInput label="Quantity" w={110} min={1} max={Math.max(product.maxQuantity, 1)} allowDecimal={false}
                clampBehavior="strict" value={quantity} onChange={setQuantity} disabled={soldOut} />
              <Button size="md" style={{ flex: 1 }} leftSection={<IconShoppingCartPlus size={20} />} disabled={soldOut}
                onClick={() => cart.add(product, Number(quantity) || 1)}>
                {soldOut ? 'Out of stock' : 'Add to cart'}
              </Button>
            </Group>

            <Card withBorder radius="md" bg="gray.0">
              <Stack gap={6}>
                <Group gap="xs"><IconCash size={18} /><Text size="sm">Pay cash when your parcel arrives</Text></Group>
                <Group gap="xs"><IconTruckDelivery size={18} /><Text size="sm">Delivery across Morocco: we call you to confirm first</Text></Group>
              </Stack>
            </Card>

            {product.description && (
              <>
                <Divider label="Description" labelPosition="left" />
                <Text style={{ whiteSpace: 'pre-line' }}>{product.description}</Text>
              </>
            )}
          </Stack>
        </Grid.Col>
      </Grid>

      {product.category && <Related product={product} />}
    </Stack>
  )
}

// Other products of the same category
function Related({ product }: { product: ShopProductDetail }) {
  const related = useQuery({
    queryKey: ['shop', 'products', { category: product.category!.slug, related: true }],
    queryFn: () => api<Page<ShopProduct>>('/api/shop/products', { query: { category: product.category!.slug, size: 5 } }),
  })
  const others = (related.data?.content ?? []).filter((p) => p.id !== product.id).slice(0, 4)
  if (others.length === 0) return null
  return (
    <section aria-labelledby="related-title">
      <Title order={2} id="related-title" fz={24} mb="md">More in {product.category!.name}</Title>
      <ProductGrid products={others} />
    </section>
  )
}
