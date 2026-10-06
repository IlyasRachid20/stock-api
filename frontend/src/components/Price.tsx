import { Badge, Group, Text, VisuallyHidden } from '@mantine/core'
import { formatMoney, reductionPercent } from '../utils/format'

// After a price drop the API sends the old price for 30 days: it's shown struck through,
// with the reduction, e.g. "MAD 9,999.00 MAD 9,500.00 -5%"
export function Price({ price, previousPrice }: { price: number; previousPrice: number | null | undefined }) {
  if (!previousPrice || previousPrice <= price) return <>{formatMoney(price)}</>
  return (
    <Group gap={6} justify="flex-end" wrap="nowrap">
      <Text span size="xs" c="dimmed" td="line-through">
        <VisuallyHidden>Was </VisuallyHidden>{formatMoney(previousPrice)}
      </Text>
      <Text span fw={600}>{formatMoney(price)}</Text>
      <Badge color="red" variant="light" size="sm">-{reductionPercent(price, previousPrice)}%</Badge>
    </Group>
  )
}
