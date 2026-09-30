import { useState } from 'react'
import { ActionIcon, Alert, Button, Card, Group, NumberInput, Select, Stack, Table, Text, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconShoppingCartCheck, IconTrash } from '@tabler/icons-react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { api } from '../api/client'
import type { Customer, Page, Product, Sale } from '../api/types'
import { formatMoney } from '../utils/format'
import { messageOf, notifySuccess } from '../utils/notify'
import { useRefreshStock } from '../utils/refresh'

interface CartLine {
  product: Product
  quantity: number
}

export function NewSalePage() {
  const refresh = useRefreshStock()
  const [customerId, setCustomerId] = useState<string | null>(null)
  const [customerSearch, setCustomerSearch] = useState('')
  const [productSearch, setProductSearch] = useState('')
  const [debouncedCustomerSearch] = useDebouncedValue(customerSearch, 250)
  const [debouncedProductSearch] = useDebouncedValue(productSearch, 250)
  const [cart, setCart] = useState<CartLine[]>([])
  const [lastSale, setLastSale] = useState<Sale | null>(null)

  const customers = useQuery({
    queryKey: ['customers', { search: debouncedCustomerSearch, picker: true }],
    queryFn: () => api<Page<Customer>>('/api/customers', { query: { search: debouncedCustomerSearch, size: 20, sort: 'name,asc' } }),
  })
  const products = useQuery({
    queryKey: ['products', { search: debouncedProductSearch, picker: true }],
    queryFn: () => api<Page<Product>>('/api/products', { query: { search: debouncedProductSearch, size: 20, sort: 'name,asc' } }),
  })

  const total = cart.reduce((sum, line) => sum + line.product.price * line.quantity, 0)

  function addProduct(id: string | null) {
    const product = products.data?.content.find((p) => String(p.id) === id)
    if (!product) return
    setLastSale(null)
    setCart((lines) => {
      const existing = lines.find((l) => l.product.id === product.id)
      if (existing) {
        return lines.map((l) => (l === existing ? { ...l, quantity: Math.min(l.quantity + 1, product.quantity) } : l))
      }
      return [...lines, { product, quantity: 1 }]
    })
    setProductSearch('')
  }

  // One request for the whole sale: the API saves everything or nothing (e.g. if stock ran out meanwhile)
  const checkout = useMutation({
    mutationFn: () =>
      api<Sale>('/api/sales', {
        method: 'POST',
        body: { customerId: Number(customerId), items: cart.map((l) => ({ productId: l.product.id, quantity: l.quantity })) },
      }),
    onSuccess: (sale) => {
      notifySuccess(`Sale #${sale.id} recorded: ${formatMoney(sale.total)}`)
      setLastSale(sale)
      setCart([])
      setCustomerId(null)
      setCustomerSearch('')
      refresh()
    },
  })

  const customerOptions = (customers.data?.content ?? []).map((c) => ({
    value: String(c.id),
    label: c.email ? `${c.name} (${c.email})` : c.name,
  }))
  const productOptions = (products.data?.content ?? []).map((p) => ({
    value: String(p.id),
    label: `${p.name} · ${formatMoney(p.price)} · ${p.quantity === 0 ? 'out of stock' : `${p.quantity} in stock`}`,
    disabled: p.quantity === 0,
  }))

  return (
    <Stack maw={900}>
      <Title order={2}>New sale</Title>

      {lastSale && (
        <Alert color="green" title={`Sale #${lastSale.id} recorded`}>
          {lastSale.items.length} line(s), total {formatMoney(lastSale.total)}. <Link to="/sales">See all sales</Link>
        </Alert>
      )}

      <Card withBorder>
        <Stack>
          <Select
            label="Customer"
            placeholder="Search a customer"
            searchable
            data={customerOptions}
            value={customerId}
            onChange={setCustomerId}
            searchValue={customerSearch}
            onSearchChange={setCustomerSearch}
            nothingFoundMessage="No customer found (add them on the Customers page)"
            filter={({ options }) => options}
          />
          <Select
            label="Add a product"
            placeholder="Search a product"
            searchable
            data={productOptions}
            value={null}
            onChange={addProduct}
            searchValue={productSearch}
            onSearchChange={setProductSearch}
            nothingFoundMessage="No product found"
            filter={({ options }) => options}
          />
        </Stack>
      </Card>

      <Card withBorder>
        {cart.length === 0 ? (
          <Text c="dimmed">The cart is empty: add products above.</Text>
        ) : (
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Product</Table.Th>
                <Table.Th ta="right">Unit price</Table.Th>
                <Table.Th w={130}>Quantity</Table.Th>
                <Table.Th ta="right">Line total</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {cart.map((line) => (
                <Table.Tr key={line.product.id}>
                  <Table.Td>{line.product.name}</Table.Td>
                  <Table.Td ta="right">{formatMoney(line.product.price)}</Table.Td>
                  <Table.Td>
                    <NumberInput
                      aria-label={`Quantity of ${line.product.name}`}
                      value={line.quantity}
                      min={1}
                      max={line.product.quantity}
                      allowDecimal={false}
                      clampBehavior="strict"
                      onChange={(value) =>
                        setCart((lines) => lines.map((l) => (l === line ? { ...l, quantity: Math.max(1, Number(value) || 1) } : l)))
                      }
                    />
                  </Table.Td>
                  <Table.Td ta="right">{formatMoney(line.product.price * line.quantity)}</Table.Td>
                  <Table.Td>
                    <ActionIcon variant="subtle" color="red" aria-label={`Remove ${line.product.name}`}
                      onClick={() => setCart((lines) => lines.filter((l) => l !== line))}>
                      <IconTrash size={18} />
                    </ActionIcon>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        )}
      </Card>

      {checkout.error && <Alert color="red" title="The sale was not recorded">{messageOf(checkout.error)}</Alert>}

      <Group justify="space-between">
        <Text fz="xl" fw={700}>Total: {formatMoney(total)}</Text>
        <Button size="md" leftSection={<IconShoppingCartCheck size={18} />} disabled={!customerId || cart.length === 0}
          loading={checkout.isPending} onClick={() => checkout.mutate()}>
          Complete sale
        </Button>
      </Group>
    </Stack>
  )
}
