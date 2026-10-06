import { Anchor, Box, Button, Card, SimpleGrid, Stack, Text } from '@mantine/core'
import { IconShoppingCartPlus } from '@tabler/icons-react'
import { Link } from 'react-router'
import { useCart } from './cart/useCart'
import { AvailabilityText, ProductPicture, ReductionBadge, StorePrice } from './ProductBits'
import { productPath, type ShopProduct } from './types'

export function ProductCard({ product }: { product: ShopProduct }) {
  const cart = useCart()
  const soldOut = product.availability === 'OUT_OF_STOCK'

  return (
    <Card withBorder radius="lg" padding="md" h="100%" style={{ display: 'flex', flexDirection: 'column' }}>
      <Card.Section pos="relative">
        <Link to={productPath(product)} tabIndex={-1} aria-hidden>
          <ProductPicture url={product.imageUrl} alt="" />
        </Link>
        <ReductionBadge price={product.price} previousPrice={product.previousPrice} pos="absolute" top={12} left={12} />
      </Card.Section>
      <Stack gap={4} mt="md" style={{ flex: 1 }}>
        {product.category && <Text size="xs" c="dimmed" tt="uppercase" fw={600}>{product.category.name}</Text>}
        <Anchor component={Link} to={productPath(product)} c="dark.8" fw={600} lineClamp={2}>
          {product.name}
        </Anchor>
        <Box mt={4}>
          <StorePrice price={product.price} previousPrice={product.previousPrice} />
        </Box>
        <AvailabilityText availability={product.availability} onlyLeft={product.onlyLeft} />
      </Stack>
      <Button mt="md" fullWidth variant={soldOut ? 'default' : 'filled'} disabled={soldOut}
        leftSection={<IconShoppingCartPlus size={18} />} onClick={() => cart.add(product)}
        aria-label={soldOut ? `${product.name} is out of stock` : `Add ${product.name} to the cart`}>
        {soldOut ? 'Out of stock' : 'Add to cart'}
      </Button>
    </Card>
  )
}

export function ProductGrid({ products, columns = 4 }: { products: ShopProduct[]; columns?: 3 | 4 }) {
  return (
    <SimpleGrid cols={{ base: 2, sm: 3, md: columns }} spacing={{ base: 'sm', sm: 'lg' }}>
      {products.map((product) => <ProductCard key={product.id} product={product} />)}
    </SimpleGrid>
  )
}
