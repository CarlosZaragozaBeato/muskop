import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useI18n } from '../i18n/I18nContext'
import LanguageSwitcher from '../i18n/LanguageSwitcher'
import * as sessions from '../storage/sessionManager'
import ThemeToggle from '../theme/ThemeToggle'

/**
 * Página de ajustes: reúne las opciones que antes estaban sueltas en la
 * cabecera (tema, idioma, sesión, cuenta). Base para ampliar con más
 * secciones (p. ej. gestión de recursos).
 */
export default function SettingsPage() {
  const { t } = useI18n()
  const navigate = useNavigate()
  const { user, downloadSession, logout } = useAuth()
  const hasMedia = useMemo(() => sessions.activeSessionHasMedia(), [])
  const [includeMedia, setIncludeMedia] = useState(false)
  const [shareNote, setShareNote] = useState<string | null>(null)

  const shareSession = async () => {
    setShareNote(null)
    const result = await sessions.shareActiveSession(includeMedia)
    if (result === 'download') setShareNote(t('settings.shareDownloaded'))
  }

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
        <h3>{t('settings.resourcesSection')}</h3>
        <p className="muted">{t('settings.resourcesDesc')}</p>
        <div className="settings-row">
          <button type="button" onClick={() => navigate('/settings/resources')}>
            {t('settings.manageResources')}
          </button>
        </div>
      </section>

      <section className="settings-section">
        <h3>{t('settings.sessionSection')}</h3>
        <p className="muted">{t('settings.sessionDesc')}</p>
        {hasMedia && (
          <label className="check">
            <input
              type="checkbox"
              checked={includeMedia}
              onChange={(e) => setIncludeMedia(e.target.checked)}
            />
            {t('settings.includeMedia')}
          </label>
        )}
        <div className="header-actions">
          <button type="button" onClick={() => downloadSession(includeMedia)}>
            {t('settings.downloadSession')}
          </button>
          <button type="button" onClick={shareSession}>
            {t('settings.shareSession')}
          </button>
        </div>
        {shareNote && <p className="success">{shareNote}</p>}
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
