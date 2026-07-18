import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import TheoryView from '../components/TheoryView'
import TabSvg from '../components/tab/TabSvg'
import { fromDocument } from '../components/tab/tabModel'
import { EXPLORE_ITEMS, type ExploreItem } from '../data/exploreContent'
import * as sessions from '../storage/sessionManager'
import { SKILLS, skillLabel } from '../types/routine'

/**
 * Explorar: catálogo de ejercicios fingerstyle y artículos de teoría
 * incluidos con la app, listos para añadir a la librería del usuario.
 */
export default function ExplorePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const skillFilter = searchParams.get('skill') ?? ''
  const [kindFilter, setKindFilter] = useState('')
  const [ownedTitles, setOwnedTitles] = useState<Set<string>>(new Set())
  const [preview, setPreview] = useState<ExploreItem | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const reloadOwned = () => {
    sessions
      .listResources()
      .then((list) => setOwnedTitles(new Set(list.map((r) => r.title.toLowerCase()))))
      .catch(() => setOwnedTitles(new Set()))
  }
  useEffect(reloadOwned, [])

  const filtered = useMemo(
    () =>
      EXPLORE_ITEMS.filter(
        (item) =>
          (!skillFilter || item.skill === skillFilter) &&
          (!kindFilter || item.kind === kindFilter),
      ),
    [skillFilter, kindFilter],
  )

  const addToLibrary = async (item: ExploreItem) => {
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
        content: { kind: 'theory', body: item.body },
      })
    }
    reloadOwned()
    setNotice(`«${item.title}» añadido a tu librería`)
    setTimeout(() => setNotice(null), 3000)
  }

  return (
    <div className="explore-page">
      <div className="page-header">
        <h2>Explorar</h2>
      </div>
      <p className="muted">
        Ejercicios guiados y teoría incluidos con Muskop. Añádelos a tu
        librería para usarlos en tus rutinas — cada uno indica la habilidad y
        el nivel para el que está pensado.
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
      </div>

      <div className="explore-grid">
        {filtered.map((item) => {
          const owned = ownedTitles.has(item.title.toLowerCase())
          return (
            <div className="explore-card" key={item.id}>
              <div className="explore-card-top">
                <span className={`badge badge-${item.kind === 'theory' ? 'theory' : 'fingerstyle'}`}>
                  {item.kind === 'theory' ? 'Teoría' : 'Ejercicio'}
                </span>
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
                {owned ? (
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
            {preview.kind === 'exercise' ? (
              <div className="preview-panel">
                <TabSvg doc={fromDocument(preview.doc)} ink="#e5e7eb" background="#16171d" />
              </div>
            ) : (
              <TheoryView body={preview.body} />
            )}
            {!ownedTitles.has(preview.title.toLowerCase()) && (
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
    </div>
  )
}
