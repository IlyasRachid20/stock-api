import { Center, Image } from '@mantine/core'
import { IconPhoto } from '@tabler/icons-react'
import type { Product } from '../api/types'

// The product's cover picture, or a placeholder. Decorative (alt=""): the name is always next to it.
export function ProductThumb({ product, size = 40 }: { product: Pick<Product, 'images'>; size?: number }) {
  const cover = product.images?.[0]
  if (cover) {
    return <Image src={cover.url} alt="" w={size} h={size} radius="sm" fit="cover" style={{ flex: 'none' }} />
  }
  return (
    <Center w={size} h={size} bg="gray.1" style={{ borderRadius: 'var(--mantine-radius-sm)', flex: 'none' }}>
      <IconPhoto size={size / 2} color="var(--mantine-color-gray-5)" />
    </Center>
  )
}
