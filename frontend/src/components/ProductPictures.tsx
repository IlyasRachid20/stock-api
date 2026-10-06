import { useState } from 'react'
import { ActionIcon, Badge, Button, Card, FileButton, Group, Image, Modal, SimpleGrid, Stack, Text, Tooltip } from '@mantine/core'
import { IconStar, IconTrash, IconUpload } from '@tabler/icons-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Product, ProductImage } from '../api/types'
import { prepareImage } from '../utils/images'
import { messageOf, notifyError, notifySuccess } from '../utils/notify'
import { QueryState } from './QueryState'

const MAX_PICTURES = 6

// Admin only: add pictures (shrunk in the browser first), choose the cover, delete
export function ProductPicturesModal({ onClose, product }: { onClose: () => void; product: Product }) {
  const queryClient = useQueryClient()
  const [uploadError, setUploadError] = useState<string | null>(null)
  const current = useQuery({
    queryKey: ['products', product.id],
    queryFn: () => api<Product>(`/api/products/${product.id}`),
    initialData: product,
  })
  const pictures = current.data.images
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['products'] })

  const upload = useMutation({
    mutationFn: async (files: File[]) => {
      for (const file of files.slice(0, MAX_PICTURES - pictures.length)) {
        const form = new FormData()
        form.append('file', await prepareImage(file), 'picture.jpg')
        await api<ProductImage>(`/api/products/${product.id}/images`, { method: 'POST', body: form })
      }
    },
    onMutate: () => setUploadError(null),
    onSuccess: (_, files) => notifySuccess(files.length > 1 ? `${files.length} pictures added` : 'Picture added'),
    onError: (error) => setUploadError(messageOf(error)),
    onSettled: refresh,
  })
  const makeCover = useMutation({
    mutationFn: (picture: ProductImage) => api(`/api/products/${product.id}/images/${picture.id}/cover`, { method: 'PUT' }),
    onSuccess: refresh,
    onError: notifyError,
  })
  const remove = useMutation({
    mutationFn: (picture: ProductImage) => api(`/api/products/${product.id}/images/${picture.id}`, { method: 'DELETE' }),
    onSuccess: refresh,
    onError: notifyError,
  })

  return (
    <Modal opened onClose={onClose} title={`Pictures: ${product.name}`} size="lg">
      <Stack>
        <QueryState isPending={false} error={current.error}>
          {pictures.length ? (
            <SimpleGrid cols={{ base: 2, sm: 3 }}>
              {pictures.map((picture, index) => (
                <Card key={picture.id} withBorder padding="xs">
                  <Card.Section>
                    <Image src={picture.url} alt={`Picture ${index + 1} of ${product.name}`} h={140} fit="contain" bg="gray.0" />
                  </Card.Section>
                  <Group justify="space-between" mt="xs">
                    {index === 0 ? <Badge size="sm" variant="light">Cover</Badge> : (
                      <Tooltip label="Use as cover">
                        <ActionIcon variant="subtle" aria-label={`Use picture ${index + 1} as cover`}
                          loading={makeCover.isPending && makeCover.variables?.id === picture.id}
                          onClick={() => makeCover.mutate(picture)}>
                          <IconStar size={16} />
                        </ActionIcon>
                      </Tooltip>
                    )}
                    <ActionIcon variant="subtle" color="red" aria-label={`Delete picture ${index + 1}`}
                      loading={remove.isPending && remove.variables?.id === picture.id}
                      onClick={() => remove.mutate(picture)}>
                      <IconTrash size={16} />
                    </ActionIcon>
                  </Group>
                </Card>
              ))}
            </SimpleGrid>
          ) : (
            <Text c="dimmed" size="sm">No pictures yet. The first one becomes the product's cover.</Text>
          )}
        </QueryState>

        {uploadError && <Text c="red" size="sm">{uploadError}</Text>}
        <Group justify="space-between">
          <Text c="dimmed" size="xs">JPEG or PNG, up to {MAX_PICTURES} per product. Large photos are shrunk before sending.</Text>
          <FileButton onChange={(files) => files.length && upload.mutate(files)} accept="image/*" multiple>
            {(props) => (
              <Button {...props} leftSection={<IconUpload size={16} />} loading={upload.isPending}
                disabled={pictures.length >= MAX_PICTURES}>
                Add pictures
              </Button>
            )}
          </FileButton>
        </Group>
      </Stack>
    </Modal>
  )
}
