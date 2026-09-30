import { useState } from 'react'
import { Badge, Group, Pagination, Select, Stack, Table, Text, Title } from '@mantine/core'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { MovementType, Page, StockMovement } from '../api/types'
import { QueryState } from '../components/QueryState'
import { formatDateTime, signed } from '../utils/format'

const TYPES: { value: MovementType; label: string; color: string }[] = [
  { value: 'INITIAL', label: 'Initial stock', color: 'gray' },
  { value: 'RESTOCK', label: 'Restock', color: 'green' },
  { value: 'ADJUSTMENT', label: 'Correction', color: 'yellow' },
  { value: 'SALE', label: 'Sale', color: 'blue' },
  { value: 'SALE_CANCELLED', label: 'Sale cancelled', color: 'grape' },
]
const typeInfo = (type: MovementType) => TYPES.find((t) => t.value === type) ?? TYPES[0]

export function StockHistoryPage() {
  const [type, setType] = useState<string | null>(null)
  const [page, setPage] = useState(1)

  const movements = useQuery({
    queryKey: ['stock-movements', { type, page }],
    queryFn: () => api<Page<StockMovement>>('/api/stock-movements', { query: { type, page: page - 1, size: 25 } }),
    placeholderData: keepPreviousData,
  })

  return (
    <Stack>
      <Title order={2}>Stock history</Title>
      <Select
        placeholder="All movements"
        data={TYPES.map(({ value, label }) => ({ value, label }))}
        value={type}
        onChange={(value) => {
          setType(value)
          setPage(1)
        }}
        clearable
        maw={240}
      />

      <QueryState isPending={movements.isPending} error={movements.error}>
        {movements.data && (
          <>
            <Table striped>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>When</Table.Th>
                  <Table.Th>Product</Table.Th>
                  <Table.Th>Type</Table.Th>
                  <Table.Th ta="right">Change</Table.Th>
                  <Table.Th ta="right">Stock after</Table.Th>
                  <Table.Th>Reason</Table.Th>
                  <Table.Th>By</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {movements.data.content.map((m) => (
                  <Table.Tr key={m.id}>
                    <Table.Td>{formatDateTime(m.createdAt)}</Table.Td>
                    <Table.Td>{m.product.name}</Table.Td>
                    <Table.Td><Badge color={typeInfo(m.type).color} variant="light">{typeInfo(m.type).label}</Badge></Table.Td>
                    <Table.Td ta="right" c={m.quantityChange < 0 ? 'red' : 'green'} fw={600}>{signed(m.quantityChange)}</Table.Td>
                    <Table.Td ta="right">{m.quantityAfter}</Table.Td>
                    <Table.Td>{m.reason ?? ''}</Table.Td>
                    <Table.Td>{m.createdBy}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
            {movements.data.content.length === 0 && <Text c="dimmed">No stock movements yet</Text>}
            {movements.data.page.totalPages > 1 && (
              <Group><Pagination total={movements.data.page.totalPages} value={page} onChange={setPage} /></Group>
            )}
          </>
        )}
      </QueryState>
    </Stack>
  )
}
