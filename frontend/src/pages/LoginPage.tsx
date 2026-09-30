import { useState } from 'react'
import { Alert, Button, Center, Paper, PasswordInput, Stack, Text, TextInput, Title } from '@mantine/core'
import { useForm } from '@mantine/form'
import { Navigate, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'

export function LoginPage() {
  const { session, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const form = useForm({
    initialValues: { username: '', password: '' },
    validate: {
      username: (value) => (value.trim() ? null : 'Enter your username'),
      password: (value) => (value ? null : 'Enter your password'),
    },
  })

  const from = (location.state as { from?: string } | null)?.from ?? '/'
  if (session) return <Navigate to={from} replace />

  const submit = form.onSubmit(async ({ username, password }) => {
    setError(null)
    setLoading(true)
    try {
      await login(username, password)
      navigate(from, { replace: true })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  })

  return (
    <Center mih="100vh" p="md" bg="gray.0">
      <Paper withBorder shadow="sm" p="xl" radius="md" w={380}>
        <form onSubmit={submit}>
          <Stack>
            <div>
              <Title order={2}>Stock API</Title>
              <Text c="dimmed" size="sm">Sign in to manage products, sales and stock</Text>
            </div>
            {error && <Alert color="red">{error}</Alert>}
            <TextInput label="Username" autoComplete="username" {...form.getInputProps('username')} />
            <PasswordInput label="Password" autoComplete="current-password" {...form.getInputProps('password')} />
            <Button type="submit" loading={loading}>Sign in</Button>
          </Stack>
        </form>
      </Paper>
    </Center>
  )
}
