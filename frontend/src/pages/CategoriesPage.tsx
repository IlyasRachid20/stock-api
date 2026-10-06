import { useState } from 'react'
import { ActionIcon, Anchor, Button, Group, Modal, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { IconEdit, IconPlus, IconTrash } from '@tabler/icons-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router'
import { useCategories } from '../api/categories'
import { api } from '../api/client'
import type { Category } from '../api/types'
import { ConfirmModal } from '../components/ConfirmModal'
import { QueryState } from '../components/QueryState'
import { notifyError, notifySuccess, showApiErrors } from '../utils/notify'

type Dialog = { kind: 'form'; category: Category | null } | { kind: 'delete'; category: Category } | null

// Admin only (see router.tsx): add, rename and delete the product categories
export function CategoriesPage() {
  const queryClient = useQueryClient()
  const categories = useCategories()
  const [dialog, setDialog] = useState<Dialog>(null)
  const close = () => setDialog(null)

  const remove = useMutation({
    mutationFn: (category: Category) => api(`/api/categories/${category.id}`, { method: 'DELETE' }),
    onSuccess: (_, category) => {
      notifySuccess(`${category.name} deleted`)
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      close()
    },
    // e.g. 409 "Category 'Phones' cannot be deleted: it has 3 product(s)"
    onError: (error) => {
      notifyError(error)
      close()
    },
  })

  return (
    <Stack maw={700}>
      <Group justify="space-between">
        <Title order={2}>Categories</Title>
        <Button leftSection={<IconPlus size={16} />} onClick={() => setDialog({ kind: 'form', category: null })}>New category</Button>
      </Group>

      <QueryState isPending={categories.isPending} error={categories.error}>
        {categories.data?.length ? (
          <Table striped highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Name</Table.Th>
                <Table.Th ta="right">Products</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {categories.data.map((c) => (
                <Table.Tr key={c.id}>
                  <Table.Td>{c.name}</Table.Td>
                  <Table.Td ta="right">
                    {/* Opens the product list filtered on this category */}
                    <Anchor component={Link} to={`/products?category=${c.id}`} size="sm">
                      {c.productCount} {c.productCount === 1 ? 'product' : 'products'}
                    </Anchor>
                  </Table.Td>
                  <Table.Td>
                    <Group gap={4} justify="flex-end">
                      <ActionIcon variant="subtle" aria-label={`Rename ${c.name}`} onClick={() => setDialog({ kind: 'form', category: c })}>
                        <IconEdit size={18} />
                      </ActionIcon>
                      <ActionIcon variant="subtle" color="red" aria-label={`Delete ${c.name}`} onClick={() => setDialog({ kind: 'delete', category: c })}>
                        <IconTrash size={18} />
                      </ActionIcon>
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        ) : (
          <Text c="dimmed">No categories yet: add the first one, e.g. "Phones"</Text>
        )}
      </QueryState>

      {dialog?.kind === 'form' && <CategoryFormModal key={dialog.category?.id ?? 'new'} category={dialog.category} onClose={close} />}
      {dialog?.kind === 'delete' && (
        <ConfirmModal opened onClose={close} title="Delete category" confirmLabel="Delete"
          loading={remove.isPending} onConfirm={() => remove.mutate(dialog.category)}>
          Delete {dialog.category.name}? Only a category without products can be deleted.
        </ConfirmModal>
      )}
    </Stack>
  )
}

function CategoryFormModal({ category, onClose }: { category: Category | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const form = useForm({
    initialValues: { name: category?.name ?? '' },
    validate: { name: (v) => (v.trim() ? null : 'Enter a name') },
  })

  const save = useMutation({
    mutationFn: (values: typeof form.values) =>
      category
        ? api<Category>(`/api/categories/${category.id}`, { method: 'PUT', body: values })
        : api<Category>('/api/categories', { method: 'POST', body: values }),
    onSuccess: (saved) => {
      notifySuccess(category ? `Renamed to ${saved.name}` : `${saved.name} added`)
      // Products show their category's name, so they are refreshed too
      queryClient.invalidateQueries({ queryKey: ['categories'] })
      queryClient.invalidateQueries({ queryKey: ['products'] })
      onClose()
    },
    // 409 "A category named ... already exists" under the field, like the 400 validation errors
    onError: (error) => {
      if (error.message.startsWith('A category named')) form.setFieldError('name', error.message)
      else showApiErrors(form, error)
    },
  })

  return (
    <Modal opened onClose={onClose} title={category ? `Rename ${category.name}` : 'New category'}>
      <form onSubmit={form.onSubmit((values) => save.mutate(values))}>
        <Stack>
          <TextInput label="Name" placeholder="e.g. Chargers & cables" data-autofocus {...form.getInputProps('name')} />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>Cancel</Button>
            <Button type="submit" loading={save.isPending}>{category ? 'Save' : 'Add category'}</Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
