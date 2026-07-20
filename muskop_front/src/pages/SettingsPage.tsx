import { useAuth } from '../auth/AuthContext'
import { useI18n } from '../i18n/I18nContext'
import LanguageSwitcher from '../i18n/LanguageSwitcher'
import ThemeToggle from '../theme/ThemeToggle'

/**
 * Página de ajustes: reúne las opciones que antes estaban sueltas en la
 * cabecera (tema, idioma, sesión, cuenta). Base para ampliar con más
 * secciones (p. ej. gestión de recursos).
 */
export default function SettingsPage() {
  const { t } = useI18n()
  const { user, downloadSession, logout } = useAuth()

  return (
    <div className="settings-page">
      <div className="page-header">
        <h2>{t('settings.title')}</h2>
      </div>

      <section className="settings-section">
        <h3>{t('settings.appearance')}</h3>
        <div className="settings-row">
          <span>{t('settings.theme')}</span>
          <ThemeToggle />
        </div>
        <div className="settings-row">
          <span>{t('settings.language')}</span>
          <LanguageSwitcher />
        </div>
      </section>

      <section className="settings-section">
        <h3>{t('settings.sessionSection')}</h3>
        <p className="muted">{t('settings.sessionDesc')}</p>
        <div className="settings-row">
          <button type="button" onClick={downloadSession}>
            {t('settings.downloadSession')}
          </button>
        </div>
      </section>

      <section className="settings-section">
        <h3>{t('settings.account')}</h3>
        {user && <p className="muted">{t('settings.loggedInAs', { name: user.username })}</p>}
        <div className="settings-row">
          <button type="button" onClick={logout}>
            {t('settings.logout')}
          </button>
        </div>
      </section>
    </div>
  )
}
