import { Group, Stack, Text, ThemeIcon } from '@mantine/core'
import { IconShoppingBag } from '@tabler/icons-react'
import { SHOP } from '../shop'

// The shop's logo: a bag in a blue square, and the name in two colours
export function Logo({ subtitle, size = 'md' }: { subtitle?: string; size?: 'sm' | 'md' }) {
  const small = size === 'sm'
  return (
    <Group gap={small ? 8 : 10} wrap="nowrap" aria-label={SHOP.name}>
      <ThemeIcon size={small ? 32 : 40} radius="md" variant="filled">
        <IconShoppingBag size={small ? 20 : 24} stroke={2} />
      </ThemeIcon>
      <Stack gap={0}>
        <Text fw={800} fz={small ? 20 : 24} lh={1.05} c="dark.8" style={{ letterSpacing: '-0.02em' }}>
          {SHOP.nameParts[0]}<Text span inherit c="brand.6">{SHOP.nameParts[1]}</Text>
        </Text>
        {subtitle && <Text size="xs" c="dimmed" lh={1.2}>{subtitle}</Text>}
      </Stack>
    </Group>
  )
}
