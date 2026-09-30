import { useState } from 'react'
import { ActionIcon, Button, Group, Modal, Pagination, Stack, Table, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { useDebouncedValue } from '@mantine/hooks'
import { IconEdit, IconPlus, IconSearch, IconTrash } from '@tabler/icons-react'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Customer, Page } from '../api/types'
import { useAuth } from '../auth/useAuth'
import { ConfirmModal } from '../components/ConfirmModal'
import { QueryState } from '../components/QueryState'
import { notifyError, notifySuccess, showApiErrors } from '../utils/notify'

type Dialog = { kind: 'form'; customer: Customer | null } | { kind: 'delete'; customer: Customer } | null

export function CustomersPage() {
  const { isAdmin } = useAuth()
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [debouncedSearch] = useDebouncedValue(search, 300)
  const [page, setPage] = useState(1)
  const [dialog, setDialog] = useState<Dialog>(null)
  const close = () => setDialog(null)

  const customers = useQuery({
    queryKey: ['customers', { search: debouncedSearch, page }],
    queryFn: () => api<Page<Customer>>('/api/customers', { query: { search: debouncedSearch, page: page - 1, size: 20, sort: 'name,asc' } }),
    placeholderData: keepPreviousData,
  })

  const remove = useMutation({
    mutationFn: (customer: Customer) => api(`/api/customers/${customer.id}`, { method: 'DELETE' }),
    onSuccess: (_, customer) => {
      notifySuccess(`${customer.name} deleted`)
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      close()
    },
    // e.g. 409 "cannot be deleted: they have 2 sale(s)"
    onError: (error) => {
      notifyError(error)
      close()
    },
  })

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Customers</Title>
        <Button leftSection={<IconPlus size={16} />} onClick={() => setDialog({ kind: 'form', customer: null })}>New customer</Button>
      </Group>

      <TextInput placeholder="Search by name or email" leftSection={<IconSearch size={16} />} value={search} maw={360}
        onChange={(e) => {
          setSearch(e.currentTarget.value)
          setPage(1)
        }} />

      <QueryState isPending={customers.isPending} error={customers.error}>
        {customers.data && (
          <>
            <Table striped highlightOnHover>
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Name</Table.Th>
                  <Table.Th>Email</Table.Th>
                  <Table.Th>Phone</Table.Th>
                  <Table.Th />
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {customers.data.content.map((c) => (
                  <Table.Tr key={c.id}>
                    <Table.Td>{c.name}</Table.Td>
                    <Table.Td>{c.email ?? ''}</Table.Td>
                    <Table.Td>{c.phone ?? ''}</Table.Td>
                    <Table.Td>
                      <Group gap={4} justify="flex-end">
                        <ActionIcon variant="subtle" aria-label={`Edit ${c.name}`} onClick={() => setDialog({ kind: 'form', customer: c })}>
                          <IconEdit size={18} />
                        </ActionIcon>
                        {isAdmin && (
                          <ActionIcon variant="subtle" color="red" aria-label={`Delete ${c.name}`} onClick={() => setDialog({ kind: 'delete', customer: c })}>
                            <IconTrash size={18} />
                          </ActionIcon>
                        )}
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
            {customers.data.content.length === 0 && <Text c="dimmed">No customers found</Text>}
            {customers.data.page.totalPages > 1 && (
              <Pagination total={customers.data.page.totalPages} value={page} onChange={setPage} />
            )}
          </>
        )}
      </QueryState>

      {dialog?.kind === 'form' && <CustomerFormModal key={dialog.customer?.id ?? 'new'} customer={dialog.customer} onClose={close} />}
      {dialog?.kind === 'delete' && (
        <ConfirmModal opened onClose={close} title="Delete customer" confirmLabel="Delete"
          loading={remove.isPending} onConfirm={() => remove.mutate(dialog.customer)}>
          Delete {dialog.customer.name}? A customer with sales can't be deleted.
        </ConfirmModal>
      )}
    </Stack>
  )
}

function CustomerFormModal({ customer, onClose }: { customer: Customer | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const form = useForm({
    initialValues: { name: customer?.name ?? '', email: customer?.email ?? '', phone: customer?.phone ?? '' },
    validate: { name: (v) => (v.trim() ? null : 'Enter a name') },
  })

  const save = useMutation({
    mutationFn: (values: typeof form.values) =>
      customer
        ? api<Customer>(`/api/customers/${customer.id}`, { method: 'PUT', body: values })
        : api<Customer>('/api/customers', { method: 'POST', body: values }),
    onSuccess: (saved) => {
      notifySuccess(customer ? `${saved.name} updated` : `${saved.name} added`)
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      onClose()
    },
    // 400 per field (e.g. invalid email); 409 "Email ... is already used" as a message under the email
    onError: (error) => {
      if (error.message.startsWith('Email ')) form.setFieldError('email', error.message)
      else showApiErrors(form, error)
    },
  })

  return (
    <Modal opened onClose={onClose} title={customer ? `Edit ${customer.name}` : 'New customer'}>
      <form onSubmit={form.onSubmit((values) => save.mutate(values))}>
        <Stack>
          <TextInput label="Name" data-autofocus {...form.getInputProps('name')} />
          <TextInput label="Email (optional)" {...form.getInputProps('email')} />
          <TextInput label="Phone (optional)" {...form.getInputProps('phone')} />
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>Cancel</Button>
            <Button type="submit" loading={save.isPending}>{customer ? 'Save' : 'Add customer'}</Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
