import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { listDeviceSessions, removeDeviceSession } from '../storage/sessionManager'
import type { StoredSession } from '../storage/db'

/**
 * Pantalla de entrada local-first: crear una sesión nueva, importar un
 * archivo .muskop.json o continuar una sesión usada en este dispositivo.
 */
export default function LoginPage() {
  const { user, ready, createSession, openSession, importSession } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
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
      setError(err instanceof Error ? err.message : 'Error abriendo la sesión')
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = (e: FormEvent) => {
    e.preventDefault()
    if (!username.trim()) return
    run(() => createSession(username))()
  }

  const handleImport = (file: File | undefined) => {
    if (!file) return
    run(() => importSession(file))()
  }

  const handleDelete = async (id: string) => {
    if (!window.confirm('¿Eliminar esta sesión del dispositivo? Si no la has descargado, se perderá.')) {
      return
    }
    await removeDeviceSession(id)
    setStored(await listDeviceSessions())
  }

  return (
    <div className="login-page">
      <div className="login-card session-card">
        <h1>Muskop</h1>
        <p>Tu rutina de guitarra · tus datos en tus archivos</p>

        <form className="session-new" onSubmit={handleCreate}>
          <input
            type="text"
            placeholder="Nombre de usuario"
            value={username}
            autoFocus
            onChange={(e) => setUsername(e.target.value)}
          />
          <button type="submit" className="primary" disabled={loading || !username.trim()}>
            {loading ? 'Abriendo…' : 'Crear sesión nueva'}
          </button>
        </form>

        <label className="button session-import">
          📂 Importar sesión (.muskop.json)
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
            <h3>Sesiones en este dispositivo</h3>
            <ul>
              {stored.map((s) => (
                <li key={s.id}>
                  <button
                    type="button"
                    className="session-open"
                    onClick={run(() => openSession(s.id))}
                  >
                    <strong>{s.username}</strong>
                    <span className="muted">
                      {new Date(s.updatedAt).toLocaleString()}
                    </span>
                  </button>
                  <button
                    type="button"
                    title="Eliminar del dispositivo"
                    onClick={() => handleDelete(s.id)}
                  >
                    🗑
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        <p className="muted session-hint">
          Una sesión es un archivo con toda tu librería y rutinas. Descárgala
          desde la cabecera para llevártela a otro dispositivo o aplicación.
        </p>
      </div>
    </div>
  )
}
