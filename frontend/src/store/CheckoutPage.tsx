import { useState } from 'react'
import { Alert, Button, Card, Container, Divider, Grid, Group, Stack, Text, Textarea, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useDocumentTitle } from '@mantine/hooks'
import { IconCash } from '@tabler/icons-react'
import { useMutation } from '@tanstack/react-query'
import { Navigate, useNavigate } from 'react-router'
import { api, ApiError } from '../api/client'
import { SHOP } from '../shop'
import { formatMoney } from '../utils/format'
import { messageOf } from '../utils/notify'
import { DeliveryLine } from './CartPage'
import { useCart } from './cart/useCart'
import { useShopInfo } from './queries'
import { deliveryFeeFor, type OrderView } from './types'

// The order form: who and where. No account and no card: the customer pays cash on delivery.
export function CheckoutPage() {
  useDocumentTitle(`Checkout · ${SHOP.name}`)
  const cart = useCart()
  const info = useShopInfo()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  const form = useForm({
    initialValues: { name: '', phone: '', city: '', address: '', note: '' },
    validate: {
      name: (v) => (v.trim() ? null : 'Enter your name'),
      phone: (v) => (v.replace(/\D/g, '').length >= 9 ? null : 'Enter a phone number, e.g. 06 12 34 56 78'),
      city: (v) => (v.trim() ? null : 'Enter your city'),
      address: (v) => (v.trim() ? null : 'Enter the delivery address'),
    },
  })

  const placeOrder = useMutation({
    mutationFn: (values: typeof form.values) => api<OrderView>('/api/shop/orders', {
      method: 'POST',
      body: { ...values, items: cart.lines.map((line) => ({ productId: line.productId, quantity: line.quantity })) },
    }),
    onMutate: () => setError(null),
    // To the confirmation first, then the cart is emptied (an empty cart on this page sends back to the cart)
    onSuccess: (order) => {
      navigate(`/order/${order.orderNumber}`, { replace: true, state: { order } })
      cart.clear()
    },
    // Field errors go under the fields; a stock or limit problem is shown above the button
    onError: (e) => {
      if (e instanceof ApiError && Object.keys(e.fieldErrors).length > 0) form.setErrors(e.fieldErrors)
      else setError(messageOf(e))
    },
  })

  if (cart.lines.length === 0 && !placeOrder.isSuccess) {
    return <Navigate to="/cart" replace />
  }
  const deliveryFee = info.data ? deliveryFeeFor(cart.subtotal, info.data) : 0

  return (
    <Container size="lg" pt="xl">
      <Title order={1} fz={30} mb="lg">Checkout</Title>
      <form onSubmit={form.onSubmit((values) => placeOrder.mutate(values))}>
        <Grid gap="xl">
          <Grid.Col span={{ base: 12, md: 7 }}>
            <Card withBorder radius="lg" p="lg">
              <Stack>
                <Text fw={700}>Delivery details</Text>
                <TextInput label="Full name" autoComplete="name" {...form.getInputProps('name')} />
                <TextInput label="Phone" description="We call you to confirm the order before shipping it" type="tel"
                  autoComplete="tel" placeholder="06 12 34 56 78" {...form.getInputProps('phone')} />
                <TextInput label="City" autoComplete="address-level2" placeholder="e.g. Casablanca" {...form.getInputProps('city')} />
                <TextInput label="Address" autoComplete="street-address" {...form.getInputProps('address')} />
                <Textarea label="Note for the delivery (optional)" placeholder="e.g. Call me when you arrive" rows={2} maxLength={500}
                  {...form.getInputProps('note')} />
              </Stack>
            </Card>
          </Grid.Col>

          <Grid.Col span={{ base: 12, md: 5 }}>
            <Card withBorder radius="lg" p="lg">
              <Stack>
                <Text fw={700}>Your order</Text>
                {cart.lines.map((line) => (
                  <Group key={line.productId} justify="space-between" wrap="nowrap">
                    <Text size="sm">{line.quantity} × {line.name}</Text>
                    <Text size="sm" fw={500}>{formatMoney(line.price * line.quantity)}</Text>
                  </Group>
                ))}
                <Divider />
                <Group justify="space-between">
                  <Text c="dimmed">Items</Text>
                  <Text>{formatMoney(cart.subtotal)}</Text>
                </Group>
                {info.data && <DeliveryLine subtotal={cart.subtotal} deliveryFee={deliveryFee} freeFrom={info.data.freeDeliveryFrom} />}
                <Divider />
                <Group justify="space-between">
                  <Text fw={700}>To pay on delivery</Text>
                  <Text fw={800} fz="xl">{formatMoney(cart.subtotal + deliveryFee)}</Text>
                </Group>
                {error && <Alert color="red" title="The order was not placed">{error}</Alert>}
                <Button type="submit" size="md" loading={placeOrder.isPending} leftSection={<IconCash size={18} />}>
                  Place order
                </Button>
                <Text size="xs" c="dimmed" ta="center">Cash on delivery: nothing to pay now.</Text>
              </Stack>
            </Card>
          </Grid.Col>
        </Grid>
      </form>
    </Container>
  )
}
