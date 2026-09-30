import type { ReactNode } from 'react'
import { Button, Group, Modal, Stack, Text } from '@mantine/core'

// Asks before a destructive action; the API's refusal (e.g. 409 "has sales") is shown by the caller
export function ConfirmModal({ opened, onClose, onConfirm, title, children, confirmLabel, loading }: {
  opened: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  children: ReactNode
  confirmLabel: string
  loading?: boolean
}) {
  return (
    <Modal opened={opened} onClose={onClose} title={title}>
      <Stack>
        <Text size="sm">{children}</Text>
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>Keep</Button>
          <Button color="red" onClick={onConfirm} loading={loading}>{confirmLabel}</Button>
        </Group>
      </Stack>
    </Modal>
  )
}
