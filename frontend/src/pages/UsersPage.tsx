import { useState } from 'react'
import { ActionIcon, Badge, Button, Group, Modal, PasswordInput, SegmentedControl, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { IconPlus, IconTrash } from '@tabler/icons-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Page, Role, User } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { ConfirmModal } from '../components/ConfirmModal'
import { QueryState } from '../components/QueryState'
import { notifyError, notifySuccess, showApiErrors } from '../utils/notify'

// Admins only (route guard + API rule)
export function UsersPage() {
  const { session } = useAuth()
  const queryClient = useQueryClient()
  const [creating, setCreating] = useState(false)
  const [deleting, setDeleting] = useState<User | null>(null)

  const users = useQuery({
    queryKey: ['users'],
    queryFn: () => api<Page<User>>('/api/users', { query: { size: 100 } }),
  })

  const remove = useMutation({
    mutationFn: (user: User) => api(`/api/users/${user.id}`, { method: 'DELETE' }),
    onSuccess: (_, user) => {
      notifySuccess(`${user.username} deleted`)
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setDeleting(null)
    },
    onError: (error) => {
      notifyError(error)
      setDeleting(null)
    },
  })

  return (
    <Stack maw={700}>
      <Group justify="space-between">
        <Title order={2}>Users</Title>
        <Button leftSection={<IconPlus size={16} />} onClick={() => setCreating(true)}>New user</Button>
      </Group>

      <QueryState isPending={users.isPending} error={users.error}>
        <Table striped>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Username</Table.Th>
              <Table.Th>Role</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {users.data?.content.map((u) => (
              <Table.Tr key={u.id}>
                <Table.Td>{u.username}{u.username === session?.username && <Text span c="dimmed"> (you)</Text>}</Table.Td>
                <Table.Td><Badge color={u.role === 'ADMIN' ? 'grape' : 'blue'} variant="light">{u.role}</Badge></Table.Td>
                <Table.Td ta="right">
                  {u.username !== session?.username && (
                    <ActionIcon variant="subtle" color="red" aria-label={`Delete ${u.username}`} onClick={() => setDeleting(u)}>
                      <IconTrash size={18} />
                    </ActionIcon>
                  )}
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </QueryState>

      {creating && <NewUserModal onClose={() => setCreating(false)} />}
      {deleting && (
        <ConfirmModal opened onClose={() => setDeleting(null)} title="Delete user" confirmLabel="Delete"
          loading={remove.isPending} onConfirm={() => remove.mutate(deleting)}>
          Delete {deleting.username}? They won't be able to log in anymore.
        </ConfirmModal>
      )}
    </Stack>
  )
}

function NewUserModal({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const form = useForm({
    initialValues: { username: '', password: '', role: 'CASHIER' as Role },
    validate: {
      username: (v) => (/^[a-z0-9._-]{3,50}$/.test(v.trim().toLowerCase()) ? null : '3 to 50 letters, digits, ".", "_" or "-"'),
      password: (v) => (v.length >= 8 ? null : 'At least 8 characters'),
    },
  })

  const save = useMutation({
    mutationFn: (values: typeof form.values) => api<User>('/api/users', { method: 'POST', body: values }),
    onSuccess: (user) => {
      notifySuccess(`${user.username} can now log in`)
      queryClient.invalidateQueries({ queryKey: ['users'] })
      onClose()
    },
    // 409 "Username ... is already taken" under the username field
    onError: (error) => {
      if (error.message.startsWith('Username ')) form.setFieldError('username', error.message)
      else showApiErrors(form, error)
    },
  })

  return (
    <Modal opened onClose={onClose} title="New user">
      <form onSubmit={form.onSubmit((values) => save.mutate(values))}>
        <Stack>
          <TextInput label="Username" data-autofocus autoComplete="off" {...form.getInputProps('username')} />
          <PasswordInput label="Password" autoComplete="new-password" {...form.getInputProps('password')} />
          <SegmentedControl data={[{ label: 'Cashier', value: 'CASHIER' }, { label: 'Admin', value: 'ADMIN' }]}
            {...form.getInputProps('role')} />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>Cancel</Button>
            <Button type="submit" loading={save.isPending}>Create user</Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
