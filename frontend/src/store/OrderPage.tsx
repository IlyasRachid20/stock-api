import { useState } from 'react'
import { Alert, Button, Card, Container, Divider, Group, Stack, Stepper, Text, TextInput, ThemeIcon, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useDocumentTitle } from '@mantine/hooks'
import { IconCircleCheck } from '@tabler/icons-react'
import { useMutation } from '@tanstack/react-query'
import { Link, useLocation, useParams } from 'react-router'
import { api } from '../api/client'
import { SHOP } from '../shop'
import { formatDateTime, formatMoney } from '../utils/format'
import { messageOf } from '../utils/notify'
import { ORDER_STATUS_LABELS, type OrderStatus, type OrderView } from './types'

// Right after checkout: the confirmation (the order comes with the navigation, nothing is
// fetched). Opened later from a link, it asks for the phone number to show the order.
export function OrderPage() {
  const { number = '' } = useParams()
  const placed = (useLocation().state as { order?: OrderView } | null)?.order
  useDocumentTitle(`Order ${number} · ${SHOP.name}`)

  if (placed?.orderNumber !== number) {
    return <TrackPage initialNumber={number} />
  }
  return (
    <Container size="md" pt="xl">
      <Stack>
        <Group gap="sm" wrap="nowrap">
          <ThemeIcon size={48} radius="xl" color="teal"><IconCircleCheck size={28} /></ThemeIcon>
          <div>
            <Title order={1} fz={28}>Thank you, {placed.delivery.name.split(' ')[0]}!</Title>
            <Text c="dimmed">Your order <b>{placed.orderNumber}</b> is placed.</Text>
          </div>
        </Group>
        <Alert color="blue" variant="light">
          We'll call you on {placed.delivery.phone} to confirm it, then deliver it to {placed.delivery.city}.
          You pay {formatMoney(placed.total)} in cash when it arrives. Keep the order number to follow it.
        </Alert>
        <OrderDetails order={placed} />
        <Group>
          <Button component={Link} to="/shop">Continue shopping</Button>
          <Button variant="default" component={Link} to="/track">Track an order</Button>
        </Group>
      </Stack>
    </Container>
  )
}

// Order number + the phone used: strangers can't read someone else's order
export function TrackPage({ initialNumber = '' }: { initialNumber?: string }) {
  useDocumentTitle(`Track an order · ${SHOP.name}`)
  const [order, setOrder] = useState<OrderView | null>(null)
  const form = useForm({
    initialValues: { orderNumber: initialNumber, phone: '' },
    validate: {
      orderNumber: (v) => (v.trim() ? null : 'Enter the order number, e.g. TS-7K3F9Q'),
      phone: (v) => (v.trim() ? null : 'Enter the phone number used for the order'),
    },
  })
  const track = useMutation({
    mutationFn: (values: typeof form.values) => api<OrderView>('/api/shop/orders/track', { method: 'POST', body: values }),
    onSuccess: setOrder,
  })

  return (
    <Container size="md" pt="xl">
      <Stack>
        <Title order={1} fz={28}>Track an order</Title>
        <Card withBorder radius="lg">
          <form onSubmit={form.onSubmit((values) => track.mutate(values))}>
            <Group align="flex-end">
              <TextInput label="Order number" placeholder="TS-7K3F9Q" style={{ flex: 1 }} {...form.getInputProps('orderNumber')} />
              <TextInput label="Phone" type="tel" placeholder="06 12 34 56 78" style={{ flex: 1 }} {...form.getInputProps('phone')} />
              <Button type="submit" loading={track.isPending}>Show my order</Button>
            </Group>
          </form>
          {track.error && <Alert color="red" mt="md">{messageOf(track.error)}</Alert>}
        </Card>
        {order && <OrderDetails order={order} />}
      </Stack>
    </Container>
  )
}

const STEPS: OrderStatus[] = ['NEW', 'CONFIRMED', 'SHIPPED', 'DELIVERED']

function OrderDetails({ order }: { order: OrderView }) {
  const ended = order.status === 'CANCELLED' || order.status === 'RETURNED'
  const when = (status: OrderStatus) => order.history.find((step) => step.status === status)?.at

  return (
    <Card withBorder radius="lg" p="lg">
      <Stack>
        <Group justify="space-between">
          <Text fw={700}>Order {order.orderNumber}</Text>
          <Text size="sm" c="dimmed">Placed {formatDateTime(order.placedAt)}</Text>
        </Group>
        {ended ? (
          <Alert color="gray">This order was {ORDER_STATUS_LABELS[order.status].toLowerCase()}.</Alert>
        ) : (
          <Stepper active={STEPS.indexOf(order.status) + 1} size="sm" allowNextStepsSelect={false}>
            {STEPS.map((status) => (
              <Stepper.Step key={status} label={ORDER_STATUS_LABELS[status]}
                description={when(status) ? formatDateTime(when(status)!) : undefined} />
            ))}
          </Stepper>
        )}
        <Divider />
        {order.items.map((item) => (
          <Group key={item.productId} justify="space-between" wrap="nowrap">
            <Text size="sm">{item.quantity} × {item.name}</Text>
            <Text size="sm">{formatMoney(item.lineTotal)}</Text>
          </Group>
        ))}
        <Group justify="space-between">
          <Text size="sm" c="dimmed">Delivery</Text>
          <Text size="sm">{order.deliveryFee === 0 ? 'Free' : formatMoney(order.deliveryFee)}</Text>
        </Group>
        <Group justify="space-between">
          <Text fw={700}>To pay on delivery</Text>
          <Text fw={800}>{formatMoney(order.total)}</Text>
        </Group>
        <Divider />
        <Text size="sm" c="dimmed">
          Delivery to {order.delivery.name}, {order.delivery.address}, {order.delivery.city}
        </Text>
      </Stack>
    </Card>
  )
}
