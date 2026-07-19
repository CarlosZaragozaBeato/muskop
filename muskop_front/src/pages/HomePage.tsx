import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { useI18n } from '../i18n/I18nContext'

export default function HomePage() {
  const { user } = useAuth()
  const { t } = useI18n()

  return (
    <div className="home-page">
      <section>
        <h2>{t('home.greeting', { name: user?.username ?? '' })}</h2>
        <p className="muted">{t('home.intro')}</p>
      </section>

      <section className="card-grid">
        <div className="card">
          <h3>{t('home.routinesTitle')}</h3>
          <p className="muted">{t('home.routinesText')}</p>
          <Link className="button primary" to="/routines">{t('home.routinesCta')}</Link>
        </div>

        <div className="card">
          <h3>{t('home.editorTitle')}</h3>
          <p className="muted">{t('home.editorText')}</p>
          <Link className="button primary" to="/tabs/new">{t('home.editorCta')}</Link>
        </div>

        <div className="card">
          <h3>{t('home.libraryTitle')}</h3>
          <p className="muted">{t('home.libraryText')}</p>
          <Link className="button" to="/library">{t('home.libraryCta')}</Link>
        </div>
      </section>
    </div>
  )
}
