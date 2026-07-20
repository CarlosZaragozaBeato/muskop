import { useEffect, useState } from 'react'
import * as api from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { useI18n } from '../../i18n/I18nContext'
import type {
  ChordContent,
  ChordShape,
  MeasureDTO,
  ResourceSummary,
  SnippetContent,
} from '../../types/tab'

interface InsertFromLibraryProps {
  onInsertChord: (chord: ChordShape) => void
  onInsertFragment: (measures: MeasureDTO[]) => void
  onClose: () => void
}

/**
 * Panel para incrustar recursos guardados (acordes y fragmentos de tablatura)
 * en el documento actual.
 */
export default function InsertFromLibrary({
  onInsertChord,
  onInsertFragment,
  onClose,
}: InsertFromLibraryProps) {
  const { user } = useAuth()
  const { t } = useI18n()
  const [items, setItems] = useState<ResourceSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    Promise.all([
      api.listResources(user.id, { type: 'CHORD' }),
      api.listResources(user.id, { type: 'SNIPPET' }),
    ])
      .then(([chords, snippets]) => setItems([...chords, ...snippets]))
      .catch((err) => setError(err instanceof Error ? err.message : t('insertLibrary.loadError')))
  }, [user, t])

  const insert = async (item: ResourceSummary) => {
    try {
      const detail = await api.getResource(item.id)
      if (item.type === 'CHORD') {
        const content = detail.content as ChordContent
        onInsertChord(content.chord)
      } else {
        const content = detail.content as SnippetContent
        onInsertFragment(content.measures)
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : t('insertLibrary.insertError'))
    }
  }

  return (
    <div className="library-panel">
      <div className="library-panel-header">
        <strong>{t('insertLibrary.title')}</strong>
        <button type="button" onClick={onClose}>✕</button>
      </div>
      {error && <p className="error">{error}</p>}
      {items === null && !error && <p className="muted">{t('common.loading')}</p>}
      {items?.length === 0 && <p className="muted">{t('insertLibrary.empty')}</p>}
      <ul>
        {items?.map((item) => (
          <li key={item.id}>
            <span className={`badge badge-${item.type.toLowerCase()}`}>
              {t(`library.types.${item.type.toUpperCase()}`)}
            </span>
            <span className="library-item-title">{item.title}</span>
            <button type="button" onClick={() => insert(item)}>{t('insertLibrary.insert')}</button>
          </li>
        ))}
      </ul>
    </div>
  )
}
