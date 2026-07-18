import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import TheoryView from '../components/TheoryView'
import * as sessions from '../storage/sessionManager'
import type { TheoryContent } from '../types/tab'

/**
 * Editor de artículos de teoría: título, categoría y cuerpo en markdown
 * sencillo (#, ##, -, **negrita**) con vista previa en vivo.
 */
export default function TheoryPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const resourceId = id ? Number(id) : null

  const [title, setTitle] = useState('')
  const [category, setCategory] = useState('')
  const [body, setBody] = useState('')
  const [loaded, setLoaded] = useState(resourceId === null)
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (resourceId === null) return
    sessions
      .getResource(resourceId)
      .then((detail) => {
        setTitle(detail.title)
        setCategory(detail.category ?? '')
        setBody((detail.content as TheoryContent).body ?? '')
        setLoaded(true)
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Error cargando la teoría'))
  }, [resourceId])

  const handleSave = async () => {
    if (!title.trim()) {
      setError('Ponle un título')
      return
    }
    setSaving(true)
    setError(null)
    const payload = {
      title: title.trim(),
      type: 'THEORY',
      category: category.trim() || null,
      content: { kind: 'theory', body } satisfies TheoryContent,
    }
    try {
      if (resourceId === null) {
        const newId = await sessions.createResource(payload)
        navigate(`/theory/${newId}`, { replace: true })
      } else {
        await sessions.updateResource(resourceId, payload)
      }
      setMessage('Teoría guardada')
      setTimeout(() => setMessage(null), 2500)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al guardar')
    } finally {
      setSaving(false)
    }
  }

  if (!loaded && !error) {
    return <p className="muted">Cargando…</p>
  }

  return (
    <div className="theory-page">
      <div className="page-header">
        <h2>{resourceId === null ? 'Nueva teoría' : 'Editar teoría'}</h2>
        <div className="header-actions">
          <button type="button" onClick={() => navigate('/library')}>← Librería</button>
          <button type="button" className="primary" disabled={saving} onClick={handleSave}>
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </div>

      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}

      <div className="tab-editor-meta">
        <label>
          Título
          <input
            type="text"
            value={title}
            placeholder="La mano derecha: PIMA"
            onChange={(e) => setTitle(e.target.value)}
          />
        </label>
        <label>
          Categoría
          <input
            type="text"
            value={category}
            placeholder="teoria, arpegios…"
            onChange={(e) => setCategory(e.target.value)}
          />
        </label>
      </div>

      <div className="theory-editor">
        <div className="theory-editor-input">
          <span className="muted">
            Markdown sencillo: <code># título</code>, <code>## subtítulo</code>,{' '}
            <code>- lista</code>, <code>**negrita**</code>
          </span>
          <textarea
            rows={22}
            value={body}
            placeholder={'# Título\n\nExplica el concepto…\n\n## Cómo practicarlo\n\n- Primer paso\n- Segundo paso'}
            onChange={(e) => setBody(e.target.value)}
          />
        </div>
        <div className="theory-editor-preview">
          <span className="muted">Vista previa</span>
          <div className="theory-preview-box">
            <TheoryView body={body || '*'} />
          </div>
        </div>
      </div>
    </div>
  )
}
