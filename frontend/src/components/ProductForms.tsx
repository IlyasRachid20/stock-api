import { Button, Group, Modal, NumberInput, Select, Stack, Table, Text, Textarea, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useMutation, useQuery } from '@tanstack/react-query'
import { useCategories } from '../api/categories'
import { api } from '../api/client'
import type { PriceChange, Product } from '../api/types'
import { formatDateTime, formatMoney } from '../utils/format'
import { QueryState } from './QueryState'
import { notifySuccess, showApiErrors } from '../utils/notify'
import { useRefreshStock } from '../utils/refresh'

interface ModalProps {
  opened: boolean
  onClose: () => void
}

// Create (product = null) or edit a product. Stock is changed with restock / correction instead.
export function ProductFormModal({ opened, onClose, product }: ModalProps & { product: Product | null }) {
  const refresh = useRefreshStock()
  const categories = useCategories()
  const form = useForm({
    initialValues: {
      name: product?.name ?? '',
      categoryId: product?.category ? String(product.category.id) : (null as string | null),
      description: product?.description ?? '',
      price: product?.price ?? ('' as number | string),
      quantity: 0 as number | string,
      minQuantity: product?.minQuantity ?? (0 as number | string),
    },
    validate: {
      name: (v) => (v.trim() ? null : 'Enter a name'),
      price: (v) => (v === '' || Number(v) < 0 ? 'Enter a price of 0 or more' : null),
    },
  })

  const save = useMutation({
    mutationFn: (values: typeof form.values) => {
      const body = {
        name: values.name,
        price: Number(values.price),
        minQuantity: Number(values.minQuantity) || 0,
        categoryId: values.categoryId ? Number(values.categoryId) : null,
        description: values.description.trim() || null,
      }
      return product
        ? api<Product>(`/api/products/${product.id}`, { method: 'PUT', body })
        : api<Product>('/api/products', { method: 'POST', body: { ...body, quantity: Number(values.quantity) || 0 } })
    },
    onSuccess: (saved) => {
      notifySuccess(product ? `${saved.name} updated` : `${saved.name} created`)
      refresh()
      onClose()
    },
    onError: (error) => showApiErrors(form, error),
  })

  return (
    <Modal opened={opened} onClose={onClose} title={product ? `Edit ${product.name}` : 'New product'}>
      <form onSubmit={form.onSubmit((values) => save.mutate(values))}>
        <Stack>
          <TextInput label="Name" data-autofocus {...form.getInputProps('name')} />
          <Select
            label="Category"
            placeholder="No category"
            clearable
            data={(categories.data ?? []).map((c) => ({ value: String(c.id), label: c.name }))}
            {...form.getInputProps('categoryId')}
          />
          <Textarea label="Description (optional)" placeholder="What the customer should know, for the online shop"
            rows={3} maxLength={2000} {...form.getInputProps('description')} />
          <NumberInput label="Price (MAD)" min={0} decimalScale={2} fixedDecimalScale {...form.getInputProps('price')} />
          {!product && <NumberInput label="Initial stock" min={0} allowDecimal={false} {...form.getInputProps('quantity')} />}
          <NumberInput label="Minimum stock (low-stock alert)" min={0} allowDecimal={false} {...form.getInputProps('minQuantity')} />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>Cancel</Button>
            <Button type="submit" loading={save.isPending}>{product ? 'Save' : 'Create'}</Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}

// Every price change of a product: when, from what to what, and who changed it
export function PriceHistoryModal({ onClose, product }: { onClose: () => void; product: Product }) {
  const history = useQuery({
    queryKey: ['products', product.id, 'price-history'],
    queryFn: () => api<PriceChange[]>(`/api/products/${product.id}/price-history`),
  })

  return (
    <Modal opened onClose={onClose} title={`Price history: ${product.name}`} size="lg">
      <QueryState isPending={history.isPending} error={history.error}>
        {history.data?.length ? (
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Date</Table.Th>
                <Table.Th ta="right">Old price</Table.Th>
                <Table.Th ta="right">New price</Table.Th>
                <Table.Th>By</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {history.data.map((c) => (
                <Table.Tr key={c.id}>
                  <Table.Td>{formatDateTime(c.changedAt)}</Table.Td>
                  <Table.Td ta="right">{formatMoney(c.oldPrice)}</Table.Td>
                  <Table.Td ta="right" c={c.newPrice < c.oldPrice ? 'green.8' : 'red.8'}>{formatMoney(c.newPrice)}</Table.Td>
                  <Table.Td>{c.changedBy}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        ) : (
          <Text c="dimmed" size="sm">The price hasn't changed since the product was created.</Text>
        )}
        <Text c="dimmed" size="xs" mt="md">
          After a price drop, the old price is shown struck through for 30 days: the lowest price of the 30 days before the drop.
        </Text>
      </QueryState>
    </Modal>
  )
}

// Goods received (restock, always adds) or a correction (count, damage, loss: + or -, reason required)
export function StockChangeModal({ opened, onClose, product, mode }: ModalProps & { product: Product; mode: 'restock' | 'adjust' }) {
  const refresh = useRefreshStock()
  const isRestock = mode === 'restock'
  const form = useForm({
    initialValues: { quantity: '' as number | string, reason: '' },
    validate: {
      quantity: (v) =>
        v === '' ? 'Enter a quantity'
          : isRestock && Number(v) < 1 ? 'Enter at least 1'
            : !isRestock && Number(v) === 0 ? 'Must not be 0' : null,
      reason: (v) => (!isRestock && !v.trim() ? 'Explain the correction' : null),
    },
  })

  const save = useMutation({
    mutationFn: ({ quantity, reason }: typeof form.values) =>
      isRestock
        ? api<Product>(`/api/products/${product.id}/restock`, { method: 'POST', body: { quantity: Number(quantity), reason: reason || null } })
        : api<Product>(`/api/products/${product.id}/adjustments`, { method: 'POST', body: { quantityChange: Number(quantity), reason } }),
    onSuccess: (saved) => {
      notifySuccess(`${saved.name}: ${saved.quantity} in stock`)
      refresh()
      onClose()
    },
    onError: (error) => showApiErrors(form, error),
  })

  return (
    <Modal opened={opened} onClose={onClose} title={`${isRestock ? 'Restock' : 'Stock correction'}: ${product.name}`}>
      <form onSubmit={form.onSubmit((values) => save.mutate(values))}>
        <Stack>
          <Text size="sm" c="dimmed">Currently {product.quantity} in stock</Text>
          <NumberInput
            label={isRestock ? 'Quantity received' : 'Change (negative to remove, e.g. -2)'}
            allowDecimal={false}
            min={isRestock ? 1 : undefined}
            data-autofocus
            {...form.getInputProps('quantity')}
          />
          <TextInput
            label={isRestock ? 'Reason (optional)' : 'Reason'}
            placeholder={isRestock ? 'e.g. Delivery #42' : 'e.g. Broken screen'}
            {...form.getInputProps('reason')}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>Cancel</Button>
            <Button type="submit" loading={save.isPending}>{isRestock ? 'Add to stock' : 'Apply correction'}</Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
