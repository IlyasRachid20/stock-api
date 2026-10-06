import { useState } from 'react'
import { Badge, Button, Drawer, Group, Pagination, Stack, Table, Text, Title } from '@mantine/core'
import { IconPlus } from '@tabler/icons-react'
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { api } from '../api/client'
import type { Page, Sale } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { ConfirmModal } from '../components/ConfirmModal'
import { QueryState } from '../components/QueryState'
import { formatDateTime, formatMoney } from '../utils/format'
import { notifyError, notifySuccess } from '../utils/notify'
import { ORDER_STATUS } from '../utils/orderStatus'
import { useRefreshStock } from '../utils/refresh'

export function SalesPage() {
  const { isAdmin } = useAuth()
  const refresh = useRefreshStock()
  const [page, setPage] = useState(1)
  const [selected, setSelected] = useState<Sale | null>(null)
  const [confirmCancel, setConfirmCancel] = useState(false)

  const sales = useQuery({
    queryKey: ['sales', { page }],
    queryFn: () => api<Page<Sale>>('/api/sales', { query: { page: page - 1, size: 20 } }),
    placeholderData: keepPreviousData,
  })

  // Admins only: deleting a sale puts all its items back in stock
  const cancel = useMutation({
    mutationFn: (sale: Sale) => api(`/api/sales/${sale.id}`, { method: 'DELETE' }),
    onSuccess: (_, sale) => {
      notifySuccess(`Sale #${sale.id} cancelled, its items are back in stock`)
      setConfirmCancel(false)
      setSelected(null)
      refresh()
    },
    onError: notifyError,
  })

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Sales</Title>
        <Button component={Link} to="/admin/sales/new" leftSection={<IconPlus size={16} />}>New sale</Button>
      </Group>

      <QueryState isPending={sales.isPending} error={sales.error}>
        {sales.data && (
          <>
            <Table striped highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>#</Table.Th>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>Customer</Table.Th>
                  <Table.Th ta="right">Items</Table.Th>
                  <Table.Th ta="right">Total</Table.Th>
                  <Table.Th>Where</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {sales.data.content.map((s) => (
                  <Table.Tr key={s.id} onClick={() => setSelected(s)} style={{ cursor: 'pointer' }}>
                    <Table.Td>{s.id}</Table.Td>
                    <Table.Td>{formatDateTime(s.saleDate)}</Table.Td>
                    <Table.Td>{s.customer.name}</Table.Td>
                    <Table.Td ta="right">{s.items.reduce((n, i) => n + i.quantity, 0)}</Table.Td>
                    <Table.Td ta="right" fw={600}>{formatMoney(s.total)}</Table.Td>
                    <Table.Td><SaleBadge sale={s} /></Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
            {sales.data.content.length === 0 && <Text c="dimmed">No sales yet</Text>}
            {sales.data.page.totalPages > 1 && <Pagination total={sales.data.page.totalPages} value={page} onChange={setPage} />}
          </>
        )}
      </QueryState>

      <Drawer opened={selected !== null} onClose={() => setSelected(null)} position="right" title={selected ? `Sale #${selected.id}` : ''}>
        {selected && (
          <Stack>
            <Text size="sm" c="dimmed">{formatDateTime(selected.saleDate)} · {selected.customer.name}</Text>
            <Table>
              <Table.Tbody>
                {selected.items.map((item) => (
                  <Table.Tr key={item.id}>
                    <Table.Td>{item.quantity} × {item.product.name}</Table.Td>
                    <Table.Td ta="right">{formatMoney(item.lineTotal)}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
            <Text fw={700} ta="right">Total {formatMoney(selected.total)}</Text>
            {selected.channel === 'ONLINE' && (
              <Text size="sm" c="dimmed">Online order {selected.orderNumber}: it's handled on the Online orders page.</Text>
            )}
            {isAdmin && selected.channel === 'STORE' && (
              <Button color="red" variant="light" onClick={() => setConfirmCancel(true)}>Cancel this sale</Button>
            )}
          </Stack>
        )}
      </Drawer>

      {selected && confirmCancel && (
        <ConfirmModal opened onClose={() => setConfirmCancel(false)} title={`Cancel sale #${selected.id}`}
          confirmLabel="Cancel sale" loading={cancel.isPending} onConfirm={() => cancel.mutate(selected)}>
          The sale is deleted and its {selected.items.length} line(s) go back into stock. This is recorded in the stock history.
        </ConfirmModal>
      )}
    </Stack>
  )
}

// Counter sale, or online order with its status
function SaleBadge({ sale }: { sale: Sale }) {
  if (sale.channel === 'STORE') return <Badge variant="light" color="gray">Counter</Badge>
  const status = ORDER_STATUS[sale.status as keyof typeof ORDER_STATUS]
  return <Badge variant="light" color={status.color}>Online · {status.label}</Badge>
}
