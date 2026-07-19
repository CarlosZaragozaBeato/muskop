import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import TheoryView from '../components/TheoryView'
import TabSvg from '../components/tab/TabSvg'
import ImportPackDialog from '../components/ImportPackDialog'
import { buildTabLabels, fromDocument, parseTabContent } from '../components/tab/tabModel'
import { getExploreItems } from '../data/exploreContent'
import { exportExercisePack, type PackExerciseInput } from '../utils/exerciseIO'
import { saveText } from '../native/share'
import * as sessions from '../storage/sessionManager'
import { SKILLS } from '../types/routine'
import { useI18n } from '../i18n/I18nContext'
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

import type { Lang } from '../i18n/types'

function builtInEntries(lang: Lang): ExploreEntry[] {
  return getExploreItems(lang).map((item) =>
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
}

/**
 * Explorar: catálogo de ejercicios fingerstyle y teoría incluidos con la app,
 * junto a los ejercicios propios del usuario. Permite añadir contenido a la
 * librería e importar/exportar packs de ejercicios (JSON).
 */
export default function ExplorePage() {
  const navigate = useNavigate()
  const { t, lang } = useI18n()
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

  const builtIn = useMemo(() => builtInEntries(lang), [lang])
  const entries = useMemo(() => [...mine, ...builtIn], [mine, builtIn])

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
    flashNotice(t('explore.added', { name: item.title }))
  }

  const exportMine = async () => {
    const list = await sessions.listExercises()
    if (list.length === 0) {
      flashNotice(t('explore.nothingToExport'))
      return
    }
    await saveText('my-exercises.muskoppack.json', exportExercisePack(list))
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
    const base = t(exercises.length === 1 ? 'explore.importedOne' : 'explore.importedMany', {
      n: exercises.length,
    })
    flashNotice(warnings.length ? `${base}. ${t('explore.warnings', { list: warnings.join('; ') })}` : base)
  }

  return (
    <div className="explore-page">
      <div className="page-header">
        <h2>{t('explore.title')}</h2>
        <div className="header-actions">
          <button type="button" onClick={() => setImporting(true)}>{t('explore.importPack')}</button>
          <button type="button" onClick={exportMine}>{t('explore.exportMine')}</button>
        </div>
      </div>
      <p className="muted">{t('explore.intro')}</p>

      {notice && <p className="success">{notice}</p>}

      <div className="library-filters">
        <select
          value={skillFilter}
          onChange={(e) => {
            const value = e.target.value
            setSearchParams(value ? { skill: value } : {})
          }}
        >
          <option value="">{t('explore.allSkills')}</option>
          {SKILLS.filter((s) => s.id !== 'general').map((s) => (
            <option key={s.id} value={s.id}>
              {s.icon} {t(`skills.${s.id}`)}
            </option>
          ))}
        </select>
        <select value={kindFilter} onChange={(e) => setKindFilter(e.target.value)}>
          <option value="">{t('explore.allContent')}</option>
          <option value="exercise">{t('explore.exercises')}</option>
          <option value="theory">{t('explore.theory')}</option>
        </select>
        <select
          value={sourceFilter}
          onChange={(e) => setSourceFilter(e.target.value as '' | 'mine' | 'catalog')}
        >
          <option value="">{t('explore.allSources')}</option>
          <option value="mine">{t('explore.onlyMine')}</option>
          <option value="catalog">{t('explore.onlyCatalog')}</option>
        </select>
      </div>

      {filtered.length === 0 && <p className="muted">{t('explore.empty')}</p>}

      <div className="explore-grid">
        {filtered.map((item) => {
          const owned = item.mine || ownedTitles.has(item.title.toLowerCase())
          return (
            <div className="explore-card" key={item.key}>
              <div className="explore-card-top">
                <span className={`badge badge-${item.kind === 'theory' ? 'theory' : 'fingerstyle'}`}>
                  {item.kind === 'theory' ? t('explore.badgeTheory') : t('explore.badgeExercise')}
                </span>
                {item.mine && <span className="chip chip-exercise">{t('explore.mine')}</span>}
                {item.skill && (
                  <span className="chip">
                    {item.level
                      ? t('explore.levelChip', { skill: t(`skills.${item.skill}`), level: item.level })
                      : t(`skills.${item.skill}`)}
                  </span>
                )}
              </div>
              <strong>{item.title}</strong>
              <p className="muted">{item.description}</p>
              <div className="explore-card-actions">
                <button type="button" onClick={() => setPreview(item)}>{t('explore.view')}</button>
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
                    {t('common.edit')}
                  </button>
                ) : owned ? (
                  <span className="muted">{t('explore.inLibrary')}</span>
                ) : (
                  <button type="button" className="primary" onClick={() => addToLibrary(item)}>
                    {t('explore.add')}
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
                <TabSvg doc={fromDocument(preview.doc)} ink="#e5e7eb" background="#16171d" labels={buildTabLabels(t)} />
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
                  {t('explore.addToLibrary')}
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
