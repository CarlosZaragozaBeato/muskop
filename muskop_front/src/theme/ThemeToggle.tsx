import { useTheme } from './ThemeContext'
import { useI18n } from '../i18n/I18nContext'

/** Botón para alternar entre tema claro y oscuro. */
export default function ThemeToggle() {
  const { theme, toggle } = useTheme()
  const { t } = useI18n()
  return (
    <button
      type="button"
      className="theme-toggle"
      title={t('theme.toggle')}
      aria-label={t('theme.toggle')}
      onClick={toggle}
    >
      {theme === 'dark' ? '☀️' : '🌙'}
    </button>
  )
}
