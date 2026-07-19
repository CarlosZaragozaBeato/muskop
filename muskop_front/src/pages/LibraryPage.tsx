import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import * as api from '../api/client'
import { useAuth } from '../auth/AuthContext'
import {
  buildTabLabels,
  fromDocument,
  parseTabContent,
  type EditorDocument,
} from '../components/tab/tabModel'
import { exportPdf, exportPng, exportText } from '../utils/exporters'
import ExerciseDialog from '../components/ExerciseDialog'
import { useI18n } from '../i18n/I18nContext'
import type { CollectionContent, ExerciseMeta, ResourceSummary } from '../types/tab'

const TYPE_ORDER = ['TAB', 'CHORD', 'SNIPPET', 'THEORY', 'COLLECTION']

export default function LibraryPage() {
  const { user } = useAuth()
  const { t } = useI18n()
  const tabLabels = buildTabLabels(t)
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
      .catch((err) => setError(err instanceof Error ? err.message : t('library.errorLoad')))
  }, [user, t])

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
      setError(err instanceof Error ? err.message : t('library.errorExport'))
    }
  }

  const remove = async (item: ResourceSummary) => {
    if (!window.confirm(t('library.deleteConfirm', { name: item.title }))) return
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
        <h2>{t('library.title')}</h2>
        <div className="header-actions">
          <Link className="button" to="/theory/new">{t('library.addTheory')}</Link>
          <button type="button" onClick={() => setCreatingCollection(true)}>
            {t('library.addCollection')}
          </button>
          <Link className="button primary" to="/tabs/new">{t('library.newTab')}</Link>
        </div>
      </div>

      {error && <p className="error">{error}</p>}

      <div className="library-filters">
        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
          <option value="">{t('library.allTypes')}</option>
          {TYPE_ORDER.map((value) => (
            <option key={value} value={value}>{t(`library.types.${value}`)}</option>
          ))}
        </select>
        <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
          <option value="">{t('library.allCategories')}</option>
          {categories.map((c) => (
            <option key={c} value={c}>{c}</option>
          ))}
        </select>
      </div>

      {items === null && !error && <p className="muted">{t('common.loading')}</p>}
      {items !== null && filtered.length === 0 && (
        <p className="muted">{t('library.empty')}</p>
      )}

      <ul className="library-list">
        {filtered.map((item) => {
          const type = item.type.toUpperCase()
          return (
            <li key={item.id} className="library-row">
              <span className={`badge badge-${type.toLowerCase()}`}>
                {t(`library.types.${type}`)}
              </span>
              <span className="library-item-title">{item.title}</span>
              {item.category && <span className="chip">{item.category}</span>}
              {item.exercise && (
                <span className="chip chip-exercise" title={t('library.exerciseChipTitle')}>
                  {t('library.exerciseChip', { skill: t(`skills.${item.exercise.skill}`), level: item.exercise.level })}
                </span>
              )}
              <span className="row-actions">
                {type === 'TAB' && (
                  <>
                    <button type="button" onClick={() => navigate(`/tabs/${item.id}`)}>
                      {t('common.edit')}
                    </button>
                    <button
                      type="button"
                      onClick={run(async () => exportText([await loadDocument(item.id)], item.title, tabLabels))}
                    >
                      .txt
                    </button>
                    <button
                      type="button"
                      onClick={run(async () => exportPng(await loadDocument(item.id), tabLabels))}
                    >
                      .png
                    </button>
                    <button
                      type="button"
                      onClick={run(async () => exportPdf([await loadDocument(item.id)], undefined, tabLabels))}
                    >
                      PDF
                    </button>
                  </>
                )}
                {type === 'THEORY' && (
                  <button type="button" onClick={() => navigate(`/theory/${item.id}`)}>
                    {t('common.edit')}
                  </button>
                )}
                {(type === 'TAB' || type === 'THEORY') && (
                  <button type="button" onClick={() => setExerciseTarget(item)}>
                    {item.exercise ? t('library.isExercise') : t('library.markExercise')}
                  </button>
                )}
                {type === 'COLLECTION' && (
                  <>
                    <button
                      type="button"
                      onClick={run(async () => exportText(await loadCollectionDocs(item), item.title, tabLabels))}
                    >
                      .txt
                    </button>
                    <button
                      type="button"
                      onClick={run(async () => exportPdf(await loadCollectionDocs(item), item.title, tabLabels))}
                    >
                      PDF
                    </button>
                  </>
                )}
                <button type="button" onClick={run(() => remove(item))}>{t('common.delete')}</button>
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
  const { t } = useI18n()
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
          <h3>{t('library.collection.title')}</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>
        <input
          type="text"
          placeholder={t('library.collection.namePlaceholder')}
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <p className="muted">{t('library.collection.pick')}</p>
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
          {tabs.length === 0 && <li className="muted">{t('library.collection.noTabs')}</li>}
        </ul>
        {error && <p className="error">{error}</p>}
        <div className="modal-footer">
          <button
            type="button"
            className="primary"
            disabled={!name.trim() || selected.length === 0}
            onClick={() =>
              onCreate(name.trim(), selected).catch((err) =>
                setError(err instanceof Error ? err.message : t('library.collection.error')),
              )
            }
          >
            {t('library.collection.create')}
          </button>
        </div>
      </div>
    </div>
  )
}
