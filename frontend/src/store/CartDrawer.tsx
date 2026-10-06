import { ActionIcon, Button, Divider, Drawer, Group, NumberInput, ScrollArea, Stack, Text } from '@mantine/core'
import { IconShoppingBag, IconTrash } from '@tabler/icons-react'
import { Link } from 'react-router'
import { formatMoney } from '../utils/format'
import { useCart } from './cart/useCart'
import { ProductPicture } from './ProductBits'

// Opens from the header's cart button, and after "Add to cart"
export function CartDrawer() {
  const cart = useCart()

  return (
    <Drawer opened={cart.drawerOpened} onClose={cart.closeDrawer} position="right" size="md" title={`Your cart (${cart.count})`}
      scrollAreaComponent={ScrollArea.Autosize}>
      {cart.lines.length === 0 ? (
        <Stack align="center" py="xl" gap="sm">
          <IconShoppingBag size={48} color="var(--mantine-color-gray-4)" />
          <Text c="dimmed">Your cart is empty</Text>
          <Button variant="light" component={Link} to="/shop" onClick={cart.closeDrawer}>Browse the shop</Button>
        </Stack>
      ) : (
        <Stack>
          {cart.lines.map((line) => (
            <Group key={line.productId} wrap="nowrap" align="flex-start">
              <div style={{ width: 64, flex: 'none', borderRadius: 8, overflow: 'hidden' }}>
                <ProductPicture url={line.imageUrl} alt="" ratio={1} />
              </div>
              <Stack gap={4} style={{ flex: 1 }}>
                <Text fw={600} size="sm" lineClamp={2}>{line.name}</Text>
                <Text size="sm" c="dimmed">{formatMoney(line.price)}</Text>
                <Group gap="xs">
                  <NumberInput size="xs" w={80} min={1} max={line.maxQuantity} value={line.quantity} allowDecimal={false}
                    clampBehavior="strict" aria-label={`Quantity of ${line.name}`}
                    onChange={(value) => cart.setQuantity(line.productId, Number(value) || 1)} />
                  <ActionIcon variant="subtle" color="red" aria-label={`Remove ${line.name}`} onClick={() => cart.remove(line.productId)}>
                    <IconTrash size={16} />
                  </ActionIcon>
                </Group>
              </Stack>
              <Text fw={600} size="sm">{formatMoney(line.price * line.quantity)}</Text>
            </Group>
          ))}
          <Divider />
          <Group justify="space-between">
            <Text fw={600}>Subtotal</Text>
            <Text fw={800} fz="lg">{formatMoney(cart.subtotal)}</Text>
          </Group>
          <Button size="md" component={Link} to="/cart" onClick={cart.closeDrawer}>View cart</Button>
          <Button variant="subtle" onClick={cart.closeDrawer}>Continue shopping</Button>
        </Stack>
      )}
    </Drawer>
  )
}
