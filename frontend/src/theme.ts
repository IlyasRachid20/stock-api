import { createTheme, type MantineColorsTuple } from '@mantine/core'

// TechSouk blue (shade 6 is the main one)
const brand: MantineColorsTuple = [
  '#e8f1ff', '#d0e1ff', '#a0c2fd', '#6da1fb', '#4285f9', '#2a73f8', '#1a69f8', '#0b58dd', '#004ec6', '#0043af',
]

export const theme = createTheme({
  primaryColor: 'brand',
  colors: { brand },
  defaultRadius: 'md',
  fontFamily: 'Inter, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif',
})
