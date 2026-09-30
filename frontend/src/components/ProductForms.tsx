import { Button, Group, Modal, NumberInput, Stack, Text, TextInput } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useMutation } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Product } from '../api/types'
import { notifySuccess, showApiErrors } from '../utils/notify'
import { useRefreshStock } from '../utils/refresh'

interface ModalProps {
  opened: boolean
  onClose: () => void
}

// Create (product = null) or edit a product. Stock is changed with restock / correction instead.
export function ProductFormModal({ opened, onClose, product }: ModalProps & { product: Product | null }) {
  const refresh = useRefreshStock()
  const form = useForm({
    initialValues: {
      name: product?.name ?? '',
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
      const body = { name: values.name, price: Number(values.price), minQuantity: Number(values.minQuantity) || 0 }
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
