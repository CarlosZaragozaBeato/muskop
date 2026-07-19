import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import TheoryView from '../components/TheoryView'
import TabSvg from '../components/tab/TabSvg'
import ImportPackDialog from '../components/ImportPackDialog'
import { fromDocument, parseTabContent } from '../components/tab/tabModel'
import { EXPLORE_ITEMS } from '../data/exploreContent'
import { exportExercisePack, type PackExerciseInput } from '../utils/exerciseIO'
import * as sessions from '../storage/sessionManager'
import { SKILLS, skillLabel } from '../types/routine'
import type { TabDocument, TheoryContent } from '../types/tab'

/**
 * Vista unificada de una tarjeta de Explorar: el catálogo incluido y los
 * ejercicios propios del usuario comparten esta forma.
 */
interface ExploreEntry {
  key: string
  kind: 'exercise' | 'theory'
  title: string
  skill: string | null
  level: number | null
  description: string
  /** true si es un ejercicio propio (ya está en la librería) */
  mine: boolean
  resourceId?: string
  doc?: TabDocument
  body?: string
}

function download(filename: string, text: string) {
  const blob = new Blob([text], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

const BUILT_IN: ExploreEntry[] = EXPLORE_ITEMS.map((item) =>
  item.kind === 'exercise'
    ? {
        key: `builtin-${item.id}`,
        kind: 'exercise',
        title: item.title,
        skill: item.skill,
        level: item.level,
        description: item.description,
        mine: false,
        doc: item.doc,
      }
    : {
        key: `builtin-${item.id}`,
        kind: 'theory',
        title: item.title,
        skill: item.skill,
        level: item.level,
        description: item.description,
        mine: false,
        body: item.body,
      },
)

/**
 * Explorar: catálogo de ejercicios fingerstyle y teoría incluidos con la app,
 * junto a los ejercicios propios del usuario. Permite añadir contenido a la
 * librería e importar/exportar packs de ejercicios (JSON).
 */
export default function ExplorePage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const skillFilter = searchParams.get('skill') ?? ''
  const [kindFilter, setKindFilter] = useState('')
  const [sourceFilter, setSourceFilter] = useState<'' | 'mine' | 'catalog'>('')
  const [ownedTitles, setOwnedTitles] = useState<Set<string>>(new Set())
  const [mine, setMine] = useState<ExploreEntry[]>([])
  const [preview, setPreview] = useState<ExploreEntry | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [importing, setImporting] = useState(false)

  const reload = useCallback(() => {
    sessions
      .listResources()
      .then((list) => setOwnedTitles(new Set(list.map((r) => r.title.toLowerCase()))))
      .catch(() => setOwnedTitles(new Set()))
    sessions
      .listExercises()
      .then((list) =>
        setMine(
          list.map((ex) => ({
            key: `mine-${ex.id}`,
            kind: ex.type.toUpperCase() === 'THEORY' ? 'theory' : 'exercise',
            title: ex.title,
            skill: ex.skill,
            level: ex.level,
            description: ex.description,
            mine: true,
            resourceId: ex.id,
            ...(ex.type.toUpperCase() === 'THEORY'
              ? { body: (ex.content as TheoryContent)?.body ?? '' }
              : { doc: safeDoc(ex.content) }),
          })),
        ),
      )
      .catch(() => setMine([]))
  }, [])
  useEffect(reload, [reload])

  const entries = useMemo(() => [...mine, ...BUILT_IN], [mine])

  const filtered = useMemo(
    () =>
      entries.filter(
        (item) =>
          (!skillFilter || item.skill === skillFilter) &&
          (!kindFilter || item.kind === kindFilter) &&
          (!sourceFilter ||
            (sourceFilter === 'mine' && item.mine) ||
            (sourceFilter === 'catalog' && !item.mine)),
      ),
    [entries, skillFilter, kindFilter, sourceFilter],
  )

  const flashNotice = (text: string) => {
    setNotice(text)
    setTimeout(() => setNotice(null), 3500)
  }

  const addToLibrary = async (item: ExploreEntry) => {
    if (item.kind === 'exercise') {
      await sessions.createResource({
        title: item.title,
        type: 'TAB',
        category: item.skill,
        content: item.doc,
      })
    } else {
      await sessions.createResource({
        title: item.title,
        type: 'THEORY',
        category: item.skill ?? 'teoria',
        content: { kind: 'theory', body: item.body ?? '' },
      })
    }
    reload()
    flashNotice(`«${item.title}» añadido a tu librería`)
  }

  const exportMine = async () => {
    const list = await sessions.listExercises()
    if (list.length === 0) {
      flashNotice('Aún no tienes ejercicios propios que exportar')
      return
    }
    download('mis-ejercicios.muskoppack.json', exportExercisePack(list))
  }

  const handleImport = async (exercises: PackExerciseInput[], warnings: string[]) => {
    for (const ex of exercises) {
      await sessions.createResource({
        title: ex.title,
        type: ex.type,
        category: ex.category,
        content: ex.content,
        exercise: ex.exercise,
      })
    }
    setImporting(false)
    reload()
    const base = `${exercises.length} ejercicio${exercises.length === 1 ? '' : 's'} importado${exercises.length === 1 ? '' : 's'}`
    flashNotice(warnings.length ? `${base}. Avisos: ${warnings.join('; ')}` : base)
  }

  return (
    <div className="explore-page">
      <div className="page-header">
        <h2>Explorar</h2>
        <div className="header-actions">
          <button type="button" onClick={() => setImporting(true)}>⇧ Importar pack</button>
          <button type="button" onClick={exportMine}>⬇ Exportar mis ejercicios</button>
        </div>
      </div>
      <p className="muted">
        Ejercicios guiados y teoría incluidos con Muskop, junto a tus propios
        ejercicios. Marca cualquier tablatura o teoría de tu librería como
        ejercicio (habilidad y nivel) y aparecerá aquí y en Progreso.
      </p>

      {notice && <p className="success">{notice}</p>}

      <div className="library-filters">
        <select
          value={skillFilter}
          onChange={(e) => {
            const value = e.target.value
            setSearchParams(value ? { skill: value } : {})
          }}
        >
          <option value="">Todas las habilidades</option>
          {SKILLS.filter((s) => s.id !== 'general').map((s) => (
            <option key={s.id} value={s.id}>
              {s.icon} {s.label}
            </option>
          ))}
        </select>
        <select value={kindFilter} onChange={(e) => setKindFilter(e.target.value)}>
          <option value="">Todo el contenido</option>
          <option value="exercise">Ejercicios</option>
          <option value="theory">Teoría</option>
        </select>
        <select
          value={sourceFilter}
          onChange={(e) => setSourceFilter(e.target.value as '' | 'mine' | 'catalog')}
        >
          <option value="">Todo el origen</option>
          <option value="mine">Solo míos</option>
          <option value="catalog">Solo del catálogo</option>
        </select>
      </div>

      {filtered.length === 0 && <p className="muted">No hay contenido con esos filtros.</p>}

      <div className="explore-grid">
        {filtered.map((item) => {
          const owned = item.mine || ownedTitles.has(item.title.toLowerCase())
          return (
            <div className="explore-card" key={item.key}>
              <div className="explore-card-top">
                <span className={`badge badge-${item.kind === 'theory' ? 'theory' : 'fingerstyle'}`}>
                  {item.kind === 'theory' ? 'Teoría' : 'Ejercicio'}
                </span>
                {item.mine && <span className="chip chip-exercise">🎯 Tuyo</span>}
                {item.skill && (
                  <span className="chip">
                    {skillLabel(item.skill)}
                    {item.level ? ` · nivel ${item.level}` : ''}
                  </span>
                )}
              </div>
              <strong>{item.title}</strong>
              <p className="muted">{item.description}</p>
              <div className="explore-card-actions">
                <button type="button" onClick={() => setPreview(item)}>Ver</button>
                {item.mine ? (
                  <button
                    type="button"
                    onClick={() =>
                      navigate(
                        item.kind === 'theory'
                          ? `/theory/${item.resourceId}`
                          : `/tabs/${item.resourceId}`,
                      )
                    }
                  >
                    Editar
                  </button>
                ) : owned ? (
                  <span className="muted">✓ En tu librería</span>
                ) : (
                  <button type="button" className="primary" onClick={() => addToLibrary(item)}>
                    + Añadir
                  </button>
                )}
              </div>
            </div>
          )
        })}
      </div>

      {preview && (
        <div className="modal-backdrop" onClick={() => setPreview(null)}>
          <div className="modal modal-wide" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{preview.title}</h3>
              <button type="button" onClick={() => setPreview(null)}>✕</button>
            </div>
            {preview.kind === 'exercise' && preview.doc ? (
              <div className="preview-panel">
                <TabSvg doc={fromDocument(preview.doc)} ink="#e5e7eb" background="#16171d" />
              </div>
            ) : (
              <TheoryView body={preview.body ?? ''} />
            )}
            {!preview.mine && !ownedTitles.has(preview.title.toLowerCase()) && (
              <div className="modal-footer">
                <button
                  type="button"
                  className="primary"
                  onClick={() => {
                    addToLibrary(preview)
                    setPreview(null)
                  }}
                >
                  + Añadir a mi librería
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {importing && (
        <ImportPackDialog
          existingTitles={[...ownedTitles]}
          onImport={handleImport}
          onClose={() => setImporting(false)}
        />
      )}
    </div>
  )
}

/** Parsea el contenido de una tablatura sin lanzar (para la previsualización). */
function safeDoc(content: unknown): TabDocument | undefined {
  try {
    return parseTabContent(content)
  } catch {
    return undefined
  }
}
