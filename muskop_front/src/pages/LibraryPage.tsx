import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import * as api from '../api/client'
import { useAuth } from '../auth/AuthContext'
import {
  fromDocument,
  parseTabContent,
  type EditorDocument,
} from '../components/tab/tabModel'
import { exportPdf, exportPng, exportText } from '../utils/exporters'
import ExerciseDialog from '../components/ExerciseDialog'
import { skillLabel } from '../types/routine'
import type { CollectionContent, ExerciseMeta, ResourceSummary } from '../types/tab'

const TYPE_LABELS: Record<string, string> = {
  TAB: 'Tablatura',
  CHORD: 'Acorde',
  SNIPPET: 'Fragmento',
  THEORY: 'Teoría',
  COLLECTION: 'Colección',
}

export default function LibraryPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const [items, setItems] = useState<ResourceSummary[] | null>(null)
  const [typeFilter, setTypeFilter] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [creatingCollection, setCreatingCollection] = useState(false)
  const [exerciseTarget, setExerciseTarget] = useState<ResourceSummary | null>(null)

  const reload = useCallback(() => {
    if (!user) return
    api
      .listResources(user.id)
      .then(setItems)
      .catch((err) => setError(err instanceof Error ? err.message : 'Error cargando la librería'))
  }, [user])

  useEffect(reload, [reload])

  const categories = useMemo(
    () => [...new Set((items ?? []).map((i) => i.category).filter((c): c is string => !!c))],
    [items],
  )

  const filtered = useMemo(
    () =>
      (items ?? []).filter(
        (i) =>
          (!typeFilter || i.type.toUpperCase() === typeFilter) &&
          (!categoryFilter || i.category === categoryFilter),
      ),
    [items, typeFilter, categoryFilter],
  )

  const loadDocument = async (id: string): Promise<EditorDocument> => {
    const detail = await api.getResource(id)
    const doc = fromDocument(parseTabContent(detail.content))
    doc.title = doc.title || detail.title
    return doc
  }

  const loadCollectionDocs = async (item: ResourceSummary): Promise<EditorDocument[]> => {
    const detail = await api.getResource(item.id)
    const content = detail.content as CollectionContent
    return Promise.all((content.resourceIds ?? []).map(loadDocument))
  }

  const run = (action: () => Promise<void> | void) => async () => {
    try {
      setError(null)
      await action()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error en la exportación')
    }
  }

  const remove = async (item: ResourceSummary) => {
    if (!window.confirm(`¿Eliminar «${item.title}»?`)) return
    await api.deleteResource(item.id)
    reload()
  }

  const saveExercise = async (meta: ExerciseMeta | null) => {
    if (!exerciseTarget) return
    await api.setResourceExercise(exerciseTarget.id, meta)
    setExerciseTarget(null)
    reload()
  }

  return (
    <div className="library-page">
      <div className="page-header">
        <h2>Librería</h2>
        <div className="header-actions">
          <Link className="button" to="/theory/new">+ Teoría</Link>
          <button type="button" onClick={() => setCreatingCollection(true)}>
            + Colección
          </button>
          <Link className="button primary" to="/tabs/new">✚ Nueva tablatura</Link>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="library-filters">
        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
          <option value="">Todos los tipos</option>
          {Object.entries(TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
        <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
          <option value="">Todas las categorías</option>
          {categories.map((c) => (
            <option key={c} value={c}>{c}</option>
          ))}
        </select>
      </div>

      {items === null && !error && <p className="muted">Cargando…</p>}
      {items !== null && filtered.length === 0 && (
        <p className="muted">No hay recursos con esos filtros.</p>
      )}

      <ul className="library-list">
        {filtered.map((item) => {
          const type = item.type.toUpperCase()
          return (
            <li key={item.id} className="library-row">
              <span className={`badge badge-${type.toLowerCase()}`}>
                {TYPE_LABELS[type] ?? item.type}
              </span>
              <span className="library-item-title">{item.title}</span>
              {item.category && <span className="chip">{item.category}</span>}
              {item.exercise && (
                <span className="chip chip-exercise" title="Aparece en Explorar y Progreso">
                  🎯 {skillLabel(item.exercise.skill)} · nivel {item.exercise.level}
                </span>
              )}
              <span className="row-actions">
                {type === 'TAB' && (
                  <>
                    <button type="button" onClick={() => navigate(`/tabs/${item.id}`)}>
                      Editar
                    </button>
                    <button
                      type="button"
                      onClick={run(async () => exportText([await loadDocument(item.id)], item.title))}
                    >
                      .txt
                    </button>
                    <button
                      type="button"
                      onClick={run(async () => exportPng(await loadDocument(item.id)))}
                    >
                      .png
                    </button>
                    <button
                      type="button"
                      onClick={run(async () => exportPdf([await loadDocument(item.id)]))}
                    >
                      PDF
                    </button>
                  </>
                )}
                {type === 'THEORY' && (
                  <button type="button" onClick={() => navigate(`/theory/${item.id}`)}>
                    Editar
                  </button>
                )}
                {(type === 'TAB' || type === 'THEORY') && (
                  <button type="button" onClick={() => setExerciseTarget(item)}>
                    {item.exercise ? '🎯 Ejercicio' : '🎯 Marcar'}
                  </button>
                )}
                {type === 'COLLECTION' && (
                  <>
                    <button
                      type="button"
                      onClick={run(async () => exportText(await loadCollectionDocs(item), item.title))}
                    >
                      .txt
                    </button>
                    <button
                      type="button"
                      onClick={run(async () => exportPdf(await loadCollectionDocs(item), item.title))}
                    >
                      PDF
                    </button>
                  </>
                )}
                <button type="button" onClick={run(() => remove(item))}>Eliminar</button>
              </span>
            </li>
          )
        })}
      </ul>

      {exerciseTarget && (
        <ExerciseDialog
          title={exerciseTarget.title}
          initial={exerciseTarget.exercise ?? null}
          onSave={saveExercise}
          onRemove={() => saveExercise(null)}
          onClose={() => setExerciseTarget(null)}
        />
      )}

      {creatingCollection && items !== null && (
        <CollectionDialog
          tabs={items.filter((i) => i.type.toUpperCase() === 'TAB')}
          onClose={() => setCreatingCollection(false)}
          onCreate={async (name, ids) => {
            if (!user) return
            await api.createResource(user.id, {
              title: name,
              type: 'COLLECTION',
              category: null,
              content: { kind: 'collection', resourceIds: ids },
            })
            setCreatingCollection(false)
            reload()
          }}
        />
      )}
    </div>
  )
}

function CollectionDialog({
  tabs,
  onClose,
  onCreate,
}: {
  tabs: ResourceSummary[]
  onClose: () => void
  onCreate: (name: string, ids: string[]) => Promise<void>
}) {
  const [name, setName] = useState('')
  const [selected, setSelected] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)

  const toggle = (id: string) => {
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id],
    )
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Nueva colección</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>
        <input
          type="text"
          placeholder="Nombre de la colección"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <p className="muted">Elige las tablaturas que la componen:</p>
        <ul className="collection-picker">
          {tabs.map((tab) => (
            <li key={tab.id}>
              <label className="check">
                <input
                  type="checkbox"
                  checked={selected.includes(tab.id)}
                  onChange={() => toggle(tab.id)}
                />
                {tab.title}
                {tab.category && <span className="chip">{tab.category}</span>}
              </label>
            </li>
          ))}
          {tabs.length === 0 && <li className="muted">Aún no tienes tablaturas guardadas.</li>}
        </ul>
        {error && <p className="error">{error}</p>}
        <div className="modal-footer">
          <button
            type="button"
            className="primary"
            disabled={!name.trim() || selected.length === 0}
            onClick={() =>
              onCreate(name.trim(), selected).catch((err) =>
                setError(err instanceof Error ? err.message : 'Error creando la colección'),
              )
            }
          >
            Crear colección
          </button>
        </div>
      </div>
    </div>
  )
}
