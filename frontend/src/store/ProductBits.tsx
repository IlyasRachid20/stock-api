import { AspectRatio, Badge, Center, Group, Image, Text, VisuallyHidden, type MantineSize } from '@mantine/core'
import { IconPhoto } from '@tabler/icons-react'
import { formatMoney, reductionPercent } from '../utils/format'
import type { Availability } from './types'

// Small pieces shared by the shop's product cards, product page and cart

export function StorePrice({ price, previousPrice, size = 'lg' }: { price: number; previousPrice: number | null; size?: MantineSize | number }) {
  const reduced = previousPrice !== null && previousPrice > price
  return (
    <Group gap={8} align="baseline" style={{ rowGap: 0 }}>
      <Text fw={800} fz={size} c={reduced ? 'red.7' : 'dark.8'} style={{ whiteSpace: 'nowrap' }}>{formatMoney(price)}</Text>
      {reduced && (
        <Text size="sm" c="dimmed" td="line-through" style={{ whiteSpace: 'nowrap' }}>
          <VisuallyHidden>Was </VisuallyHidden>{formatMoney(previousPrice)}
        </Text>
      )}
    </Group>
  )
}

export function ReductionBadge({ price, previousPrice, ...rest }: { price: number; previousPrice: number | null; pos?: 'absolute'; top?: number; left?: number }) {
  if (previousPrice === null || previousPrice <= price) return null
  return <Badge color="red" variant="filled" {...rest}>-{reductionPercent(price, previousPrice)}%</Badge>
}

const AVAILABILITY = {
  IN_STOCK: { color: 'teal.7', label: () => 'In stock' },
  FEW_LEFT: { color: 'orange.7', label: (left: number | null) => `Only ${left} left` },
  OUT_OF_STOCK: { color: 'red.7', label: () => 'Out of stock' },
}

export function AvailabilityText({ availability, onlyLeft }: { availability: Availability; onlyLeft: number | null }) {
  const { color, label } = AVAILABILITY[availability]
  return (
    <Group gap={6} wrap="nowrap">
      <span style={{ width: 8, height: 8, borderRadius: 8, background: `var(--mantine-color-${color.replace('.', '-')})` }} />
      <Text size="sm" c={color} fw={500}>{label(onlyLeft)}</Text>
    </Group>
  )
}

// A product picture filling a box of the given shape (cropped to it, or whole with fit="contain"),
// or a placeholder when the product has none
export function ProductPicture({ url, alt, ratio = 4 / 3, fit = 'cover' }: {
  url: string | null
  alt: string
  ratio?: number
  fit?: 'cover' | 'contain'
}) {
  return (
    <AspectRatio ratio={ratio}>
      {url ? <Image src={url} alt={alt} fit={fit} bg="white" /> : (
        <Center bg="gray.0">
          <IconPhoto size={32} color="var(--mantine-color-gray-4)" />
        </Center>
      )}
    </AspectRatio>
  )
}
