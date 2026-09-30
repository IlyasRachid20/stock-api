import type { ReactNode } from 'react'
import { Alert, Center, Loader } from '@mantine/core'
import { IconAlertTriangle } from '@tabler/icons-react'

// Shows a loader while data loads and the API's error message if it fails
export function QueryState({ isPending, error, children }: { isPending: boolean; error: Error | null; children: ReactNode }) {
  if (error) {
    return (
      <Alert color="red" icon={<IconAlertTriangle size={18} />} title="Could not load data">
        {error.message}
      </Alert>
    )
  }
  if (isPending) {
    return (
      <Center py="xl">
        <Loader />
      </Center>
    )
  }
  return children
}
