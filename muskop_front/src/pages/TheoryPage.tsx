import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import TheoryView from '../components/TheoryView'
import * as sessions from '../storage/sessionManager'
import { useI18n } from '../i18n/I18nContext'
import type { TheoryContent } from '../types/tab'

/**
 * Editor de artículos de teoría: título, categoría y cuerpo en markdown
 * sencillo (#, ##, -, **negrita**) con vista previa en vivo.
 */
export default function TheoryPage() {
  const { t } = useI18n()
  const { id } = useParams()
  const navigate = useNavigate()
  const resourceId = id ?? null

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
      .catch((err) => setError(err instanceof Error ? err.message : t('theoryPage.loadError')))
  }, [resourceId, t])

  const handleSave = async () => {
    if (!title.trim()) {
      setError(t('theoryPage.needTitle'))
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
      setMessage(t('theoryPage.saved'))
      setTimeout(() => setMessage(null), 2500)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('theoryPage.saveError'))
    } finally {
      setSaving(false)
    }
  }

  if (!loaded && !error) {
    return <p className="muted">{t('common.loading')}</p>
  }

  return (
    <div className="theory-page">
      <div className="page-header">
        <h2>{resourceId === null ? t('theoryPage.newTitle') : t('theoryPage.editTitle')}</h2>
        <div className="header-actions">
          <button type="button" onClick={() => navigate('/library')}>{t('theoryPage.backToLibrary')}</button>
          <button type="button" className="primary" disabled={saving} onClick={handleSave}>
            {saving ? t('common.saving') : t('common.save')}
          </button>
        </div>
      </div>

      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}

      <div className="tab-editor-meta">
        <label>
          {t('theoryPage.titleLabel')}
          <input
            type="text"
            value={title}
            placeholder={t('theoryPage.titlePlaceholder')}
            onChange={(e) => setTitle(e.target.value)}
          />
        </label>
        <label>
          {t('theoryPage.categoryLabel')}
          <input
            type="text"
            value={category}
            placeholder={t('theoryPage.categoryPlaceholder')}
            onChange={(e) => setCategory(e.target.value)}
          />
        </label>
      </div>

      <div className="theory-editor">
        <div className="theory-editor-input">
          <span className="muted">{t('theoryPage.markdownHint')}</span>
          <textarea
            rows={22}
            value={body}
            placeholder={t('theoryPage.bodyPlaceholder')}
            onChange={(e) => setBody(e.target.value)}
          />
        </div>
        <div className="theory-editor-preview">
          <span className="muted">{t('theoryPage.preview')}</span>
          <div className="theory-preview-box">
            <TheoryView body={body || '*'} />
          </div>
        </div>
      </div>
    </div>
  )
}
