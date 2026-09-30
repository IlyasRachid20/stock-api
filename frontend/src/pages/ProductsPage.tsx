import { useState } from 'react'
import { Badge, Group, Pagination, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconSearch } from '@tabler/icons-react'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Page, Product } from '../api/types'
import { QueryState } from '../components/QueryState'
import { formatMoney } from '../utils/format'

const PAGE_SIZE = 20

export function ProductsPage() {
  const [search, setSearch] = useState('')
  const [debouncedSearch] = useDebouncedValue(search, 300)
  const [page, setPage] = useState(1)

  const products = useQuery({
    queryKey: ['products', { search: debouncedSearch, page }],
    queryFn: () =>
      api<Page<Product>>('/api/products', { query: { search: debouncedSearch, page: page - 1, size: PAGE_SIZE, sort: 'name,asc' } }),
    // Keep the current rows on screen while the next page loads
    placeholderData: keepPreviousData,
  })

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Products</Title>
        {products.data && <Text c="dimmed" size="sm">{products.data.page.totalElements} products</Text>}
      </Group>

      <TextInput
        placeholder="Search by name"
        leftSection={<IconSearch size={16} />}
        value={search}
        onChange={(e) => {
          setSearch(e.currentTarget.value)
          setPage(1)
        }}
        maw={360}
      />

      <QueryState isPending={products.isPending} error={products.error}>
        {products.data && (
          <>
            <Table striped highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Name</Table.Th>
                  <Table.Th ta="right">Price</Table.Th>
                  <Table.Th ta="right">In stock</Table.Th>
                  <Table.Th ta="right">Minimum</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {products.data.content.map((p) => (
                  <Table.Tr key={p.id}>
                    <Table.Td>{p.name}</Table.Td>
                    <Table.Td ta="right">{formatMoney(p.price)}</Table.Td>
                    <Table.Td ta="right">{p.quantity}</Table.Td>
                    <Table.Td ta="right">{p.minQuantity}</Table.Td>
                    <Table.Td>
                      {p.quantity === 0 ? <Badge color="red">Out of stock</Badge>
                        : p.lowStock ? <Badge color="orange">Low stock</Badge> : null}
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
            {products.data.content.length === 0 && <Text c="dimmed">No product matches "{debouncedSearch}"</Text>}
            {products.data.page.totalPages > 1 && (
              <Pagination total={products.data.page.totalPages} value={page} onChange={setPage} />
            )}
          </>
        )}
      </QueryState>
    </Stack>
  )
}
