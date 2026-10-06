import { Card, Container, Grid, Group, NavLink, Pagination, Select, Stack, Text, Title } from '@mantine/core'
import { useDocumentTitle } from '@mantine/hooks'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'
import { api } from '../api/client'
import type { Page } from '../api/types'
import { QueryState } from '../components/QueryState'
import { SHOP } from '../shop'
import { ProductGrid } from './ProductCard'
import { useShopCategories } from './queries'
import type { ShopProduct } from './types'

const PAGE_SIZE = 12
const SORTS = [
  { value: 'name,asc', label: 'Name' },
  { value: 'price,asc', label: 'Price: low to high' },
  { value: 'price,desc', label: 'Price: high to low' },
  { value: 'id,desc', label: 'Newest' },
]

// All products, or one category's, or search results. Everything is in the address:
// /shop?category=phones&sort=price,asc&page=2
export function ShopPage() {
  const [params, setParams] = useSearchParams()
  const category = params.get('category')
  const search = params.get('search') ?? ''
  const sort = params.get('sort') ?? 'name,asc'
  const page = Number(params.get('page') ?? '1') || 1
  const categories = useShopCategories()
  const current = categories.data?.find((c) => c.slug === category)
  const title = search ? `Results for "${search}"` : (current?.name ?? 'All products')
  useDocumentTitle(`${title} · ${SHOP.name}`)

  const products = useQuery({
    queryKey: ['shop', 'products', { category, search, sort, page }],
    queryFn: () => api<Page<ShopProduct>>('/api/shop/products', {
      query: { category, search, sort, page: page - 1, size: PAGE_SIZE },
    }),
    placeholderData: keepPreviousData,
  })

  // Changing the category or the order starts again at page 1
  const update = (changes: Record<string, string | null>) => {
    const next = new URLSearchParams(params)
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)))
    if (!('page' in changes)) next.delete('page')
    setParams(next)
  }

  return (
    <Container size="lg" pt="xl">
      <Grid gap="xl">
        <Grid.Col span={{ base: 12, md: 3 }} visibleFrom="md">
          <Card withBorder radius="lg" p="xs">
            <Text fw={700} size="sm" px="sm" py="xs">Categories</Text>
            <NavLink component={Link} to="/shop" label="All products" active={!category} />
            {(categories.data ?? []).map((c) => (
              <NavLink key={c.id} component={Link} to={`/shop?category=${c.slug}`} label={c.name}
                rightSection={<Text size="xs" c="dimmed">{c.productCount}</Text>} active={c.slug === category} />
            ))}
          </Card>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 9 }}>
          <Stack>
            <Group justify="space-between" align="flex-end">
              <div>
                <Title order={1} fz={30}>{title}</Title>
                {products.data && (
                  <Text c="dimmed" size="sm">{products.data.page.totalElements} {products.data.page.totalElements === 1 ? 'product' : 'products'}</Text>
                )}
              </div>
              <Group gap="sm">
                <Select hiddenFrom="md" aria-label="Category" placeholder="All products" clearable w={170}
                  data={(categories.data ?? []).map((c) => ({ value: c.slug, label: c.name }))}
                  value={category} onChange={(value) => update({ category: value })} />
                <Select aria-label="Sort by" w={190} data={SORTS} value={sort} allowDeselect={false}
                  onChange={(value) => update({ sort: value })} />
              </Group>
            </Group>

            <QueryState isPending={products.isPending} error={products.error}>
              {products.data && (
                products.data.content.length ? (
                  <>
                    <ProductGrid products={products.data.content} columns={3} />
                    {products.data.page.totalPages > 1 && (
                      <Group justify="center" mt="md">
                        <Pagination total={products.data.page.totalPages} value={page}
                          onChange={(value) => update({ page: String(value) })} />
                      </Group>
                    )}
                  </>
                ) : (
                  <Text c="dimmed" py="xl">No products found. Try another search or category.</Text>
                )
              )}
            </QueryState>
          </Stack>
        </Grid.Col>
      </Grid>
    </Container>
  )
}
