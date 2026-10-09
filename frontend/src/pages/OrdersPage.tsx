import { useState } from 'react'
import { Anchor, Badge, Button, Divider, Drawer, Group, Pagination, Stack, Table, Tabs, Text, Textarea, TextInput, Timeline, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconPhone, IconSearch } from '@tabler/icons-react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { OrderDetail, OrderStatus, OrderSummary, Page } from '../api/types'
import { QueryState } from '../components/QueryState'
import { formatDateTime, formatMoney } from '../utils/format'
import { notifyError, notifySuccess } from '../utils/notify'
import { ORDER_STATUS } from '../utils/orderStatus'
import { useRefreshStock } from '../utils/refresh'

// The buttons for the next steps (cancelling or a return also puts the products back in stock)
const ACTIONS: Partial<Record<OrderStatus, { label: string; color?: string; variant?: 'filled' | 'light' }>> = {
  CONFIRMED: { label: 'Confirm (customer called)' },
  SHIPPED: { label: 'Mark as shipped' },
  DELIVERED: { label: 'Delivered, cash collected', color: 'teal' },
  CANCELLED: { label: 'Cancel the order', color: 'red', variant: 'light' },
  RETURNED: { label: 'Refused at the door', color: 'red', variant: 'light' },
}

const TABS: { value: string; label: string }[] = [
  { value: 'NEW', label: 'To confirm' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'SHIPPED', label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
  { value: 'RETURNED', label: 'Returned' },
  { value: 'ALL', label: 'All' },
]

// Online orders (cash on delivery): the cashier calls to confirm, then ships; cancelling before
// shipping, or a parcel refused at the door, puts the products back in stock
export function OrdersPage() {
  const [tab, setTab] = useState('NEW')
  const [search, setSearch] = useState('')
  const [debouncedSearch] = useDebouncedValue(search, 300)
  const [page, setPage] = useState(1)
  const [selectedId, setSelectedId] = useState<number | null>(null)

  const orders = useQuery({
    queryKey: ['orders', { tab, search: debouncedSearch, page }],
    queryFn: () => api<Page<OrderSummary>>('/api/orders', {
      query: { status: tab === 'ALL' ? null : tab, search: debouncedSearch, page: page - 1, size: 20 },
    }),
    placeholderData: keepPreviousData,
  })
  const counts = useQuery({ queryKey: ['orders', 'counts'], queryFn: () => api<Partial<Record<OrderStatus, number>>>('/api/orders/counts') })

  return (
    <Stack>
      <Title order={2}>Online orders</Title>
      <Group justify="space-between" align="flex-end">
        <Tabs value={tab} onChange={(value) => { setTab(value ?? 'NEW'); setPage(1) }}>
          <Tabs.List>
            {TABS.map((t) => (
              <Tabs.Tab key={t.value} value={t.value}
                rightSection={counts.data?.[t.value as OrderStatus] ? <Badge size="xs" circle>{counts.data[t.value as OrderStatus]}</Badge> : null}>
                {t.label}
              </Tabs.Tab>
            ))}
          </Tabs.List>
        </Tabs>
        <TextInput placeholder="Number, name or phone" leftSection={<IconSearch size={16} />} value={search} w={260}
          onChange={(e) => { setSearch(e.currentTarget.value); setPage(1) }} />
      </Group>

      <QueryState isPending={orders.isPending} error={orders.error}>
        {orders.data && (
          <>
            <Table.ScrollContainer minWidth={760}>
              <Table striped highlightOnHover>
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th>Order</Table.Th>
                    <Table.Th>Placed</Table.Th>
                    <Table.Th>Customer</Table.Th>
                    <Table.Th>City</Table.Th>
                    <Table.Th ta="right">Items</Table.Th>
                    <Table.Th ta="right">To collect</Table.Th>
                    <Table.Th>Status</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {orders.data.content.map((o) => (
                    <Table.Tr key={o.id} onClick={() => setSelectedId(o.id)} style={{ cursor: 'pointer' }}>
                      <Table.Td fw={600}>{o.orderNumber}</Table.Td>
                      <Table.Td>{formatDateTime(o.placedAt)}</Table.Td>
                      <Table.Td>{o.customerName}<Text size="xs" c="dimmed">{o.phone}</Text></Table.Td>
                      <Table.Td>{o.city}</Table.Td>
                      <Table.Td ta="right">{o.itemCount}</Table.Td>
                      <Table.Td ta="right" fw={600}>{formatMoney(o.total)}</Table.Td>
                      <Table.Td><Badge color={ORDER_STATUS[o.status].color} variant="light">{ORDER_STATUS[o.status].label}</Badge></Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </Table.ScrollContainer>
            {orders.data.content.length === 0 && <Text c="dimmed">No orders here</Text>}
            {orders.data.page.totalPages > 1 && <Pagination total={orders.data.page.totalPages} value={page} onChange={setPage} />}
          </>
        )}
      </QueryState>

      <Drawer opened={selectedId !== null} onClose={() => setSelectedId(null)} position="right" size="md" title="Order">
        {selectedId !== null && <OrderPanel id={selectedId} />}
      </Drawer>
    </Stack>
  )
}

function OrderPanel({ id }: { id: number }) {
  const queryClient = useQueryClient()
  const refreshStock = useRefreshStock()
  const [note, setNote] = useState('')
  const order = useQuery({ queryKey: ['orders', id], queryFn: () => api<OrderDetail>(`/api/orders/${id}`) })

  const change = useMutation({
    mutationFn: (status: OrderStatus) => api<OrderDetail>(`/api/orders/${id}/status`, {
      method: 'POST', body: { status, note: note.trim() || null },
    }),
    onSuccess: (updated) => {
      notifySuccess(`${updated.orderNumber}: ${ORDER_STATUS[updated.status].label.toLowerCase()}`)
      setNote('')
      queryClient.setQueryData(['orders', id], updated)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      // Cancelled or returned: the stock changed; delivered: the revenue changed
      refreshStock()
    },
    onError: notifyError,
  })

  return (
    <QueryState isPending={order.isPending} error={order.error}>
      {order.data && (
        <Stack>
          <Group justify="space-between">
            <Title order={3}>{order.data.orderNumber}</Title>
            <Badge size="lg" color={ORDER_STATUS[order.data.status].color} variant="light">{ORDER_STATUS[order.data.status].label}</Badge>
          </Group>
          <Stack gap={2}>
            <Text fw={600}>{order.data.delivery.name}</Text>
            <Anchor href={`tel:${order.data.delivery.phone}`} size="sm"><IconPhone size={14} /> {order.data.delivery.phone}</Anchor>
            <Text size="sm">{order.data.delivery.address}, {order.data.delivery.city}</Text>
            {order.data.delivery.note && <Text size="sm" c="dimmed">Note: {order.data.delivery.note}</Text>}
          </Stack>
          <Table>
            <Table.Tbody>
              {order.data.items.map((item) => (
                <Table.Tr key={item.id}>
                  <Table.Td>{item.quantity} × {item.product.name}</Table.Td>
                  <Table.Td ta="right">{formatMoney(item.lineTotal)}</Table.Td>
                </Table.Tr>
              ))}
              <Table.Tr>
                <Table.Td c="dimmed">Delivery</Table.Td>
                <Table.Td ta="right">{order.data.deliveryFee === 0 ? 'Free' : formatMoney(order.data.deliveryFee)}</Table.Td>
              </Table.Tr>
            </Table.Tbody>
          </Table>
          <Text fw={700} ta="right">To collect {formatMoney(order.data.total)}</Text>

          {order.data.nextStatuses.length > 0 && (
            <>
              <Divider label="Next step" labelPosition="left" />
              <Textarea placeholder="Note (optional), e.g. why it's cancelled" rows={2} maxLength={255} value={note}
                onChange={(e) => setNote(e.currentTarget.value)} aria-label="Note" />
              <Stack gap="xs">
                {order.data.nextStatuses.map((status) => (
                  <Button key={status} color={ACTIONS[status]?.color} variant={ACTIONS[status]?.variant ?? 'filled'}
                    loading={change.isPending && change.variables === status} onClick={() => change.mutate(status)}>
                    {ACTIONS[status]?.label ?? status}
                  </Button>
                ))}
              </Stack>
            </>
          )}

          <Divider label="History" labelPosition="left" />
          <Timeline active={order.data.history.length - 1} bulletSize={14} lineWidth={2}>
            {order.data.history.map((step, index) => (
              <Timeline.Item key={index} title={ORDER_STATUS[step.status].label}>
                <Text size="xs" c="dimmed">{formatDateTime(step.changedAt)} · {step.changedBy}</Text>
                {step.note && <Text size="sm">{step.note}</Text>}
              </Timeline.Item>
            ))}
          </Timeline>
        </Stack>
      )}
    </QueryState>
  )
}
