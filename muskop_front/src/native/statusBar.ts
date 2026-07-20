import { Capacitor } from '@capacitor/core'

// ==========================================================================
// Coordina la barra de estado con el tema de la app.
//   - Web/PWA: actualiza <meta name="theme-color"> con el color de fondo.
//   - Android (Capacitor): ajusta el estilo de los iconos de la barra de
//     estado (claros sobre fondo oscuro y viceversa) para que se lean bien.
// ==========================================================================

const FALLBACK_BG: Record<'light' | 'dark', string> = {
  light: '#f5f1e8',
  dark: '#1f2430',
}

/** Color de fondo actual del tema (lee la variable CSS, con respaldo). */
function themeBg(theme: 'light' | 'dark'): string {
  const v = getComputedStyle(document.documentElement).getPropertyValue('--bg').trim()
  return v || FALLBACK_BG[theme]
}

export async function applyThemeToStatusBar(theme: 'light' | 'dark'): Promise<void> {
  const bg = themeBg(theme)
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', bg)

  if (!Capacitor.isNativePlatform()) return
  try {
    const { StatusBar, Style } = await import('@capacitor/status-bar')
    // Style.Dark = contenido claro (para fondo oscuro); Style.Light = contenido
    // oscuro (para fondo claro).
    await StatusBar.setStyle({ style: theme === 'dark' ? Style.Dark : Style.Light })
    await StatusBar.setBackgroundColor({ color: bg })
  } catch {
    // el plugin puede no estar disponible en algún entorno: se ignora
  }
}
