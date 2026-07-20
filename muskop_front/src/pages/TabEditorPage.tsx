import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import * as api from '../api/client'
import { useAuth } from '../auth/AuthContext'
import TabEditor from '../components/tab/TabEditor'
import { useI18n } from '../i18n/I18nContext'
import {
  emptyDocument,
  fromDocument,
  parseTabContent,
  toDocument,
  type EditorDocument,
} from '../components/tab/tabModel'

export default function TabEditorPage() {
  const { user } = useAuth()
  const { t } = useI18n()
  const { id } = useParams()
  const navigate = useNavigate()
  const resourceId = id ?? null

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
      .catch((err) => setError(err instanceof Error ? err.message : t('tabEditorPage.loadError')))
  }, [resourceId, t])

  const handleSave = async (doc: EditorDocument) => {
    if (!user) return
    if (!doc.title.trim()) {
      setError(t('tabEditorPage.needTitle'))
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
        setMessage(t('tabEditorPage.saved', { id: newId }))
        navigate(`/tabs/${newId}`, { replace: true })
      } else {
        await api.updateResource(resourceId, payload)
        setMessage(t('tabEditorPage.savedChanges'))
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : t('tabEditorPage.saveError'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="tab-editor-page">
      <div className="page-header">
        <h2>{resourceId === null ? t('tabEditorPage.newTitle') : t('tabEditorPage.editTitle')}</h2>
        <button type="button" onClick={() => navigate('/library')}>{t('tabEditorPage.backToLibrary')}</button>
      </div>
      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}
      {initialDoc === null && !error && <p className="muted">{t('common.loading')}</p>}
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
