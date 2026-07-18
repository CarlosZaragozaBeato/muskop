import { useEffect, useState } from 'react'
import * as api from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
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
  const [items, setItems] = useState<ResourceSummary[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    Promise.all([
      api.listResources(user.id, { type: 'CHORD' }),
      api.listResources(user.id, { type: 'SNIPPET' }),
    ])
      .then(([chords, snippets]) => setItems([...chords, ...snippets]))
      .catch((err) => setError(err instanceof Error ? err.message : 'Error cargando la librería'))
  }, [user])

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
      setError(err instanceof Error ? err.message : 'Error insertando el recurso')
    }
  }

  return (
    <div className="library-panel">
      <div className="library-panel-header">
        <strong>Incrustar desde la librería</strong>
        <button type="button" onClick={onClose}>✕</button>
      </div>
      {error && <p className="error">{error}</p>}
      {items === null && !error && <p className="muted">Cargando…</p>}
      {items?.length === 0 && (
        <p className="muted">
          No tienes acordes ni fragmentos guardados. Usa ♥ en un acorde o
          «Compás → snippet» para crear reutilizables.
        </p>
      )}
      <ul>
        {items?.map((item) => (
          <li key={item.id}>
            <span className={`badge badge-${item.type.toLowerCase()}`}>
              {item.type === 'CHORD' ? 'Acorde' : 'Fragmento'}
            </span>
            <span className="library-item-title">{item.title}</span>
            <button type="button" onClick={() => insert(item)}>Insertar</button>
          </li>
        ))}
      </ul>
    </div>
  )
}
