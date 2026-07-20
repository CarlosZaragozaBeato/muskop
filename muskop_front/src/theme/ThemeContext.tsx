import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'

// ==========================================================================
// Tema claro/oscuro. Se guarda por dispositivo en localStorage
// ('muskop.theme'); en el primer arranque sigue la preferencia del sistema.
// El atributo data-theme en <html> activa las variables de color (index.css).
// ==========================================================================

export type Theme = 'light' | 'dark'
const STORAGE_KEY = 'muskop.theme'

export function getInitialTheme(): Theme {
  const v = typeof localStorage !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null
  if (v === 'light' || v === 'dark') return v
  if (typeof matchMedia !== 'undefined' && matchMedia('(prefers-color-scheme: light)').matches) {
    return 'light'
  }
  return 'dark'
}

interface ThemeContextValue {
  theme: Theme
  setTheme: (theme: Theme) => void
  toggle: () => void
}

const ThemeContext = createContext<ThemeContextValue | null>(null)

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(getInitialTheme)

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem(STORAGE_KEY, theme)
  }, [theme])

  const setTheme = useCallback((next: Theme) => setThemeState(next), [])
  const toggle = useCallback(() => setThemeState((t) => (t === 'dark' ? 'light' : 'dark')), [])

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggle }}>{children}</ThemeContext.Provider>
  )
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme debe usarse dentro de <ThemeProvider>')
  return ctx
}
