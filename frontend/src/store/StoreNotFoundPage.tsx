import { Button, Container, Stack, Text, Title } from '@mantine/core'
import { Link } from 'react-router'

export function StoreNotFoundPage() {
  return (
    <Container size="sm" py={80}>
      <Stack align="center">
        <Title order={1} fz={30}>Page not found</Title>
        <Text c="dimmed">This page doesn't exist, or the product is no longer sold.</Text>
        <Button component={Link} to="/shop">Back to the shop</Button>
      </Stack>
    </Container>
  )
}
