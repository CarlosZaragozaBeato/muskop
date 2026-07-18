import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import * as api from '../api/client'
import { useAuth } from '../auth/AuthContext'
import TabEditor from '../components/tab/TabEditor'
import {
  emptyDocument,
  fromDocument,
  parseTabContent,
  toDocument,
  type EditorDocument,
} from '../components/tab/tabModel'

export default function TabEditorPage() {
  const { user } = useAuth()
  const { id } = useParams()
  const navigate = useNavigate()
  const resourceId = id ? Number(id) : null

  const [initialDoc, setInitialDoc] = useState<EditorDocument | null>(
    resourceId === null ? emptyDocument() : null,
  )
  const [saving, setSaving] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (resourceId === null) return
    api
      .getResource(resourceId)
      .then((detail) => {
        const doc = fromDocument(parseTabContent(detail.content))
        doc.title = doc.title || detail.title
        doc.category = doc.category ?? detail.category
        setInitialDoc(doc)
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Error cargando la tablatura'))
  }, [resourceId])

  const handleSave = async (doc: EditorDocument) => {
    if (!user) return
    if (!doc.title.trim()) {
      setError('Ponle un título a la tablatura antes de guardar')
      return
    }
    setSaving(true)
    setError(null)
    setMessage(null)
    const payload = {
      title: doc.title,
      type: 'TAB',
      category: doc.category,
      content: toDocument(doc),
    }
    try {
      if (resourceId === null) {
        const newId = await api.createResource(user.id, payload)
        setMessage(`Tablatura guardada (#${newId})`)
        navigate(`/tabs/${newId}`, { replace: true })
      } else {
        await api.updateResource(resourceId, payload)
        setMessage('Cambios guardados')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al guardar')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="tab-editor-page">
      <div className="page-header">
        <h2>{resourceId === null ? 'Nueva tablatura' : 'Editar tablatura'}</h2>
        <button type="button" onClick={() => navigate('/library')}>← Librería</button>
      </div>
      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}
      {initialDoc === null && !error && <p className="muted">Cargando…</p>}
      {initialDoc !== null && (
        <TabEditor
          key={resourceId ?? 'new'}
          initialDoc={initialDoc}
          onSave={handleSave}
          saving={saving}
        />
      )}
    </div>
  )
}
