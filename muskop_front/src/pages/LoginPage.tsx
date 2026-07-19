import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { listDeviceSessions, removeDeviceSession } from '../storage/sessionManager'
import type { StoredSession } from '../storage/db'
import { useI18n } from '../i18n/I18nContext'
import LanguageSwitcher from '../i18n/LanguageSwitcher'

/**
 * Pantalla de entrada local-first: crear una sesión nueva, importar un
 * archivo .muskop.json o continuar una sesión usada en este dispositivo.
 */
export default function LoginPage() {
  const { user, ready, createSession, openSession, importSession } = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [label, setLabel] = useState('')
  const [stored, setStored] = useState<StoredSession[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    listDeviceSessions()
      .then(setStored)
      .catch(() => setStored([]))
  }, [])

  if (ready && user) {
    return <Navigate to="/" replace />
  }

  const run = (action: () => Promise<void>) => async () => {
    setLoading(true)
    setError(null)
    try {
      await action()
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : t('login.errorOpen'))
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = (e: FormEvent) => {
    e.preventDefault()
    if (!username.trim()) return
    run(() => createSession(username, label))()
  }

  const handleImport = (file: File | undefined) => {
    if (!file) return
    run(() => importSession(file))()
  }

  const handleDelete = async (id: string) => {
    if (!window.confirm(t('login.deleteConfirm'))) {
      return
    }
    await removeDeviceSession(id)
    setStored(await listDeviceSessions())
  }

  return (
    <div className="login-page">
      <div className="login-card session-card">
        <div className="login-top">
          <h1>Muskop</h1>
          <LanguageSwitcher />
        </div>
        <p>{t('login.tagline')}</p>

        <form className="session-new" onSubmit={handleCreate}>
          <input
            type="text"
            placeholder={t('login.usernamePlaceholder')}
            value={username}
            autoFocus
            onChange={(e) => setUsername(e.target.value)}
          />
          <input
            type="text"
            placeholder={t('login.labelPlaceholder')}
            value={label}
            onChange={(e) => setLabel(e.target.value)}
          />
          <button type="submit" className="primary" disabled={loading || !username.trim()}>
            {loading ? t('login.opening') : t('login.create')}
          </button>
        </form>

        <label className="button session-import">
          {t('login.import')}
          <input
            type="file"
            accept=".json,application/json"
            hidden
            onChange={(e) => handleImport(e.target.files?.[0])}
          />
        </label>

        {error && <p className="error">{error}</p>}

        {stored.length > 0 && (
          <div className="session-list">
            <h3>{t('login.deviceSessions')}</h3>
            <ul>
              {stored.map((s) => (
                <li key={s.id}>
                  <button
                    type="button"
                    className="session-open"
                    onClick={run(() => openSession(s.id))}
                  >
                    <strong>{s.label ? `${s.username} — ${s.label}` : s.username}</strong>
                    <span className="muted">
                      {new Date(s.updatedAt).toLocaleString()}
                    </span>
                  </button>
                  <button
                    type="button"
                    title={t('login.deleteTitle')}
                    onClick={() => handleDelete(s.id)}
                  >
                    🗑
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        <p className="muted session-hint">{t('login.hint')}</p>
      </div>
    </div>
  )
}
