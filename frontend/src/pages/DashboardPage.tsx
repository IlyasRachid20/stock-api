import { useState } from 'react'
import { AreaChart } from '@mantine/charts'
import { Badge, Button, Card, Group, SegmentedControl, SimpleGrid, Stack, Table, Text, Title } from '@mantine/core'
import { IconDownload } from '@tabler/icons-react'
import { useQuery } from '@tanstack/react-query'
import { api, download } from '../api/client'
import type { DailySales, Page, Product, SalesSummary, TopProduct } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { QueryState } from '../components/QueryState'
import { daysAgo, formatDay, formatInteger, formatMoney } from '../utils/format'

const PERIODS = [
  { label: '7 days', value: '7' },
  { label: '30 days', value: '30' },
  { label: '90 days', value: '90' },
]

export function DashboardPage() {
  const { isAdmin } = useAuth()
  return (
    <Stack>
      <Title order={2}>Dashboard</Title>
      {isAdmin && <SalesReports />}
      <LowStock />
    </Stack>
  )
}

// Reports are admin-only in the API, so cashiers don't see this part
function SalesReports() {
  const [days, setDays] = useState('30')
  const range = { from: daysAgo(Number(days) - 1), to: daysAgo(0) }

  const summary = useQuery({
    queryKey: ['reports', 'summary', range],
    queryFn: () => api<SalesSummary>('/api/reports/summary', { query: range }),
  })
  const byDay = useQuery({
    queryKey: ['reports', 'sales-by-day', range],
    queryFn: () => api<DailySales[]>('/api/reports/sales-by-day', { query: range }),
  })
  const top = useQuery({
    queryKey: ['reports', 'top-products', range],
    queryFn: () => api<TopProduct[]>('/api/reports/top-products', { query: { ...range, limit: 5 } }),
  })

  return (
    <Stack>
      <Group justify="space-between">
        <SegmentedControl data={PERIODS} value={days} onChange={setDays} aria-label="Period" />
        <Button variant="light" leftSection={<IconDownload size={16} />}
          onClick={() => download('/api/reports/sales.csv', range)}>
          Export CSV
        </Button>
      </Group>

      <QueryState isPending={summary.isPending} error={summary.error}>
        {summary.data && (
          <SimpleGrid cols={{ base: 2, md: 4 }}>
            <Kpi label="Revenue" value={formatMoney(summary.data.revenue)} />
            <Kpi label="Sales" value={formatInteger(summary.data.salesCount)} />
            <Kpi label="Items sold" value={formatInteger(summary.data.itemsSold)} />
            <Kpi label="Average sale" value={formatMoney(summary.data.averageSale)} />
          </SimpleGrid>
        )}
      </QueryState>

      <SimpleGrid cols={{ base: 1, lg: 3 }}>
        <Card withBorder style={{ gridColumn: 'span 2' }}>
          <Text fw={600} mb="sm">Revenue per day</Text>
          <QueryState isPending={byDay.isPending} error={byDay.error}>
            <AreaChart
              h={260}
              data={(byDay.data ?? []).map((d) => ({ day: formatDay(d.date), Revenue: d.revenue }))}
              dataKey="day"
              series={[{ name: 'Revenue', color: 'indigo.6' }]}
              curveType="monotone"
              withDots={false}
              valueFormatter={formatMoney}
            />
          </QueryState>
        </Card>

        <Card withBorder>
          <Text fw={600} mb="sm">Best sellers</Text>
          <QueryState isPending={top.isPending} error={top.error}>
            {top.data?.length ? (
              <Table>
                <Table.Tbody>
                  {top.data.map((p) => (
                    <Table.Tr key={p.productId}>
                      <Table.Td>{p.name}</Table.Td>
                      <Table.Td ta="right">{formatInteger(p.quantitySold)} sold</Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            ) : (
              <Text c="dimmed" size="sm">No sales in this period</Text>
            )}
          </QueryState>
        </Card>
      </SimpleGrid>
    </Stack>
  )
}

function Kpi({ label, value }: { label: string; value: string }) {
  return (
    <Card withBorder>
      <Text size="sm" c="dimmed">{label}</Text>
      <Text fw={700} fz="xl">{value}</Text>
    </Card>
  )
}

function LowStock() {
  const lowStock = useQuery({
    queryKey: ['products', 'low-stock'],
    queryFn: () => api<Page<Product>>('/api/products/low-stock', { query: { size: 10 } }),
  })

  return (
    <Card withBorder>
      <Group justify="space-between" mb="sm">
        <Text fw={600}>Low stock</Text>
        {lowStock.data && <Badge color={lowStock.data.page.totalElements ? 'red' : 'green'}>
          {lowStock.data.page.totalElements} to reorder
        </Badge>}
      </Group>
      <QueryState isPending={lowStock.isPending} error={lowStock.error}>
        {lowStock.data?.content.length ? (
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Product</Table.Th>
                <Table.Th ta="right">In stock</Table.Th>
                <Table.Th ta="right">Minimum</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {lowStock.data.content.map((p) => (
                <Table.Tr key={p.id}>
                  <Table.Td>{p.name}</Table.Td>
                  <Table.Td ta="right" c={p.quantity === 0 ? 'red' : undefined} fw={p.quantity === 0 ? 700 : undefined}>
                    {p.quantity}
                  </Table.Td>
                  <Table.Td ta="right">{p.minQuantity}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        ) : (
          <Text c="dimmed" size="sm">Every product is above its minimum level</Text>
        )}
      </QueryState>
    </Card>
  )
}
