import { notifications } from '@mantine/notifications'
import type { UseFormReturnType } from '@mantine/form'
import { ApiError } from '../api/client'

export function notifySuccess(message: string) {
  notifications.show({ color: 'green', message })
}

export function notifyError(error: unknown) {
  notifications.show({ color: 'red', title: 'Something went wrong', message: messageOf(error) })
}

export function messageOf(error: unknown): string {
  return error instanceof Error ? error.message : 'Unexpected error'
}

// Shows the API's validation errors ({"errors": {"price": "..."}}) under the matching form fields,
// and anything else as a notification
export function showApiErrors<Values>(form: UseFormReturnType<Values>, error: unknown) {
  if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
    form.setErrors(error.fieldErrors)
  } else {
    notifyError(error)
  }
}
