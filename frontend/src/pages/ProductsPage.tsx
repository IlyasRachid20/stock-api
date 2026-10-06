import { useState } from 'react'
import { ActionIcon, Badge, Button, Group, Menu, Pagination, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconAdjustments, IconDots, IconEdit, IconPlus, IconSearch, IconTag, IconTrash, IconTruckDelivery } from '@tabler/icons-react'
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'
import { useCategories } from '../api/categories'
import { api } from '../api/client'
import type { Page, Product } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { ConfirmModal } from '../components/ConfirmModal'
import { Price } from '../components/Price'
import { PriceHistoryModal, ProductFormModal, StockChangeModal } from '../components/ProductForms'
import { QueryState } from '../components/QueryState'
import { notifyError, notifySuccess } from '../utils/notify'
import { useRefreshStock } from '../utils/refresh'

const PAGE_SIZE = 20

// Which dialog is open, and for which product
type Dialog =
  | { kind: 'create' }
  | { kind: 'edit' | 'restock' | 'adjust' | 'prices' | 'delete'; product: Product }
  | null

export function ProductsPage() {
  const { isAdmin } = useAuth()
  const [search, setSearch] = useState('')
  const [debouncedSearch] = useDebouncedValue(search, 300)
  const [page, setPage] = useState(1)
  const [dialog, setDialog] = useState<Dialog>(null)
  // The category filter is in the address, so the Categories page can link to it
  const [params, setParams] = useSearchParams()
  const categoryId = params.get('category')
  const categories = useCategories()
  const refresh = useRefreshStock()
  const close = () => setDialog(null)

  const products = useQuery({
    queryKey: ['products', { search: debouncedSearch, categoryId, page }],
    queryFn: () =>
      api<Page<Product>>('/api/products', {
        query: { search: debouncedSearch, categoryId, page: page - 1, size: PAGE_SIZE, sort: 'name,asc' },
      }),
    // Keep the current rows on screen while the next page loads
    placeholderData: keepPreviousData,
  })

  const remove = useMutation({
    mutationFn: (product: Product) => api(`/api/products/${product.id}`, { method: 'DELETE' }),
    onSuccess: (_, product) => {
      notifySuccess(`${product.name} deleted`)
      refresh()
      close()
    },
    // e.g. 409 "cannot be deleted: it appears in 3 sale item(s)"
    onError: (error) => {
      notifyError(error)
      close()
    },
  })

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Products</Title>
        <Group>
          {products.data && <Text c="dimmed" size="sm">{products.data.page.totalElements} products</Text>}
          {isAdmin && (
            <Button leftSection={<IconPlus size={16} />} onClick={() => setDialog({ kind: 'create' })}>New product</Button>
          )}
        </Group>
      </Group>

      <Group>
        <TextInput
          placeholder="Search by name"
          leftSection={<IconSearch size={16} />}
          value={search}
          onChange={(e) => {
            setSearch(e.currentTarget.value)
            setPage(1)
          }}
          w={300}
        />
        <Select
          aria-label="Category"
          placeholder="All categories"
          clearable
          data={(categories.data ?? []).map((c) => ({ value: String(c.id), label: c.name }))}
          value={categoryId}
          onChange={(value) => {
            setParams(value ? { category: value } : {})
            setPage(1)
          }}
          w={220}
        />
      </Group>

      <QueryState isPending={products.isPending} error={products.error}>
        {products.data && (
          <>
            {/* On a phone the table scrolls sideways inside its box instead of widening the page */}
            <Table.ScrollContainer minWidth={760}>
              <Table striped highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Name</Table.Th>
                    <Table.Th>Category</Table.Th>
                    <Table.Th ta="right">Price</Table.Th>
                    <Table.Th ta="right">In stock</Table.Th>
                    <Table.Th ta="right">Minimum</Table.Th>
                    <Table.Th />
                    {isAdmin && <Table.Th />}
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {products.data.content.map((p) => (
                    <Table.Tr key={p.id}>
                      <Table.Td>{p.name}</Table.Td>
                      <Table.Td>{p.category && <Badge variant="light" color="gray" tt="none">{p.category.name}</Badge>}</Table.Td>
                      <Table.Td ta="right"><Price price={p.price} previousPrice={p.previousPrice} /></Table.Td>
                      <Table.Td ta="right">{p.quantity}</Table.Td>
                      <Table.Td ta="right">{p.minQuantity}</Table.Td>
                      <Table.Td>
                        {p.quantity === 0 ? <Badge color="red">Out of stock</Badge>
                          : p.lowStock ? <Badge color="orange">Low stock</Badge> : null}
                      </Table.Td>
                      {isAdmin && (
                        <Table.Td ta="right">
                          <Menu position="bottom-end">
                            <Menu.Target>
                              <ActionIcon variant="subtle" aria-label={`Actions for ${p.name}`}><IconDots size={18} /></ActionIcon>
                            </Menu.Target>
                            <Menu.Dropdown>
                              <Menu.Item leftSection={<IconTruckDelivery size={16} />} onClick={() => setDialog({ kind: 'restock', product: p })}>Restock</Menu.Item>
                              <Menu.Item leftSection={<IconAdjustments size={16} />} onClick={() => setDialog({ kind: 'adjust', product: p })}>Stock correction</Menu.Item>
                              <Menu.Item leftSection={<IconEdit size={16} />} onClick={() => setDialog({ kind: 'edit', product: p })}>Edit</Menu.Item>
                              <Menu.Item leftSection={<IconTag size={16} />} onClick={() => setDialog({ kind: 'prices', product: p })}>Price history</Menu.Item>
                              <Menu.Divider />
                              <Menu.Item color="red" leftSection={<IconTrash size={16} />} onClick={() => setDialog({ kind: 'delete', product: p })}>Delete</Menu.Item>
                            </Menu.Dropdown>
                          </Menu>
                        </Table.Td>
                      )}
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
            {products.data.content.length === 0 && <Text c="dimmed">No products found</Text>}
            {products.data.page.totalPages > 1 && (
              <Pagination total={products.data.page.totalPages} value={page} onChange={setPage} />
            )}
          </>
        )}
      </QueryState>

      {/* key: a fresh form for each product */}
      {(dialog?.kind === 'create' || dialog?.kind === 'edit') && (
        <ProductFormModal key={dialog.kind === 'edit' ? dialog.product.id : 'new'} opened onClose={close}
          product={dialog.kind === 'edit' ? dialog.product : null} />
      )}
      {(dialog?.kind === 'restock' || dialog?.kind === 'adjust') && (
        <StockChangeModal opened onClose={close} product={dialog.product} mode={dialog.kind} />
      )}
      {dialog?.kind === 'prices' && <PriceHistoryModal onClose={close} product={dialog.product} />}
      {dialog?.kind === 'delete' && (
        <ConfirmModal opened onClose={close} title="Delete product" confirmLabel="Delete"
          loading={remove.isPending} onConfirm={() => remove.mutate(dialog.product)}>
          Delete {dialog.product.name}? A product that was sold can't be deleted.
        </ConfirmModal>
      )}
    </Stack>
  )
}
