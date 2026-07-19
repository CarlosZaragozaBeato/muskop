import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import * as sessions from '../storage/sessionManager'
import type { ResourceSummary } from '../types/tab'
import {
  ROUTINE_CATEGORIES,
  SKILLS,
  newBlock,
  routineMinutes,
  type PracticeBlock,
  type Routine,
} from '../types/routine'
import { useI18n } from '../i18n/I18nContext'

const RESOURCE_TYPES_FOR_BLOCKS = ['TAB', 'CHORD', 'SNIPPET', 'THEORY']

export default function RoutineEditorPage() {
  const { t } = useI18n()
  const { id } = useParams()
  const navigate = useNavigate()
  const routineId = id ?? null

  const [routine, setRoutine] = useState<Omit<Routine, 'id'> | null>(
    routineId === null
      ? { name: '', description: null, category: null, blocks: [newBlock()] }
      : null,
  )
  const [resources, setResources] = useState<ResourceSummary[]>([])
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    sessions
      .listResources()
      .then((list) =>
        setResources(list.filter((r) => RESOURCE_TYPES_FOR_BLOCKS.includes(r.type.toUpperCase()))),
      )
      .catch(() => setResources([]))
  }, [])

  useEffect(() => {
    if (routineId === null) return
    sessions
      .getRoutine(routineId)
      .then((r) =>
        setRoutine({
          name: r.name,
          description: r.description,
          category: r.category,
          blocks: [...r.blocks],
        }),
      )
      .catch((err) => setError(err instanceof Error ? err.message : t('routineEditor.loadError')))
  }, [routineId, t])

  if (routine === null) {
    return <p className="muted">{error ?? t('common.loading')}</p>
  }

  const updateBlock = (blockId: string, patch: Partial<PracticeBlock>) => {
    setRoutine((prev) =>
      prev && {
        ...prev,
        blocks: prev.blocks.map((b) => (b.id === blockId ? { ...b, ...patch } : b)),
      },
    )
  }

  const moveBlock = (index: number, delta: number) => {
    setRoutine((prev) => {
      if (!prev) return prev
      const target = index + delta
      if (target < 0 || target >= prev.blocks.length) return prev
      const blocks = [...prev.blocks]
      ;[blocks[index], blocks[target]] = [blocks[target], blocks[index]]
      return { ...prev, blocks }
    })
  }

  const removeBlock = (blockId: string) => {
    setRoutine((prev) => prev && { ...prev, blocks: prev.blocks.filter((b) => b.id !== blockId) })
  }

  const handleSave = async () => {
    if (!routine.name.trim()) {
      setError(t('routineEditor.needName'))
      return
    }
    if (routine.blocks.length === 0) {
      setError(t('routineEditor.needBlock'))
      return
    }
    setSaving(true)
    setError(null)
    try {
      if (routineId === null) {
        const newId = await sessions.createRoutine(routine)
        navigate(`/routines/${newId}`, { replace: true })
      } else {
        await sessions.updateRoutine(routineId, routine)
      }
      setMessage(t('routineEditor.saved'))
      setTimeout(() => setMessage(null), 2500)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('routineEditor.saveError'))
    } finally {
      setSaving(false)
    }
  }

  const total = routineMinutes(routine)

  return (
    <div className="routine-editor-page">
      <div className="page-header">
        <h2>{routineId === null ? t('routineEditor.newTitle') : t('routineEditor.editTitle')}</h2>
        <div className="header-actions">
          <button type="button" onClick={() => navigate('/routines')}>{t('routineEditor.backToRoutines')}</button>
          <button type="button" className="primary" disabled={saving} onClick={handleSave}>
            {saving ? t('common.saving') : t('common.save')}
          </button>
        </div>
      </div>

      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}

      <div className="tab-editor-meta">
        <label>
          {t('routineEditor.nameLabel')}
          <input
            type="text"
            value={routine.name}
            placeholder={t('routineEditor.namePlaceholder')}
            onChange={(e) => setRoutine({ ...routine, name: e.target.value })}
          />
        </label>
        <label>
          {t('routineEditor.descriptionLabel')}
          <input
            type="text"
            value={routine.description ?? ''}
            placeholder={t('routineEditor.descriptionPlaceholder')}
            onChange={(e) => setRoutine({ ...routine, description: e.target.value || null })}
          />
        </label>
        <label>
          {t('routineEditor.categoryLabel')}
          <input
            type="text"
            list="routine-categories"
            value={routine.category ?? ''}
            placeholder="daily, song…"
            onChange={(e) => setRoutine({ ...routine, category: e.target.value || null })}
          />
          <datalist id="routine-categories">
            {ROUTINE_CATEGORIES.map((c) => (
              <option key={c} value={c} />
            ))}
          </datalist>
        </label>
        <span className="tab-editor-tuning">{t('routineEditor.totalLabel', { n: total })}</span>
      </div>

      <div className="blocks-list">
        {routine.blocks.map((block, i) => (
          <div className="block-card" key={block.id}>
            <div className="block-header">
              <span className="block-number">{i + 1}</span>
              <input
                type="text"
                className="block-name"
                value={block.name}
                placeholder={t('routineEditor.blockNamePlaceholder')}
                onChange={(e) => updateBlock(block.id, { name: e.target.value })}
              />
              <label>
                {t('routineEditor.minutesLabel')}
                <input
                  type="number"
                  min={1}
                  max={120}
                  value={block.minutes}
                  onChange={(e) =>
                    updateBlock(block.id, { minutes: Math.max(1, Number(e.target.value)) })
                  }
                />
              </label>
              <label>
                {t('routineEditor.bpmLabel')}
                <input
                  type="number"
                  min={20}
                  max={300}
                  placeholder={t('routineEditor.bpmFree')}
                  value={block.bpm ?? ''}
                  onChange={(e) =>
                    updateBlock(block.id, { bpm: e.target.value ? Number(e.target.value) : null })
                  }
                />
              </label>
              <label>
                {t('routineEditor.skillLabel')}
                <select
                  value={block.skill ?? ''}
                  onChange={(e) => updateBlock(block.id, { skill: e.target.value || null })}
                >
                  <option value="">—</option>
                  {SKILLS.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.icon} {t(`skills.${s.id}`)}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                {t('routineEditor.resourceLabel')}
                <select
                  value={block.resourceId ?? ''}
                  onChange={(e) =>
                    updateBlock(block.id, {
                      resourceId: e.target.value || null,
                    })
                  }
                >
                  <option value="">{t('routineEditor.resourceNone')}</option>
                  {resources.map((r) => (
                    <option key={r.id} value={r.id}>
                      [{r.type}] {r.title}
                    </option>
                  ))}
                </select>
              </label>
              <span className="section-spacer" />
              <button type="button" disabled={i === 0} onClick={() => moveBlock(i, -1)}>↑</button>
              <button
                type="button"
                disabled={i === routine.blocks.length - 1}
                onClick={() => moveBlock(i, 1)}
              >
                ↓
              </button>
              <button
                type="button"
                disabled={routine.blocks.length <= 1}
                onClick={() => removeBlock(block.id)}
              >
                ✕
              </button>
            </div>
            <input
              type="text"
              className="block-notes"
              value={block.notes ?? ''}
              placeholder={t('routineEditor.notesPlaceholder')}
              onChange={(e) => updateBlock(block.id, { notes: e.target.value || null })}
            />
          </div>
        ))}
      </div>

      <button
        type="button"
        className="add-block"
        onClick={() => setRoutine({ ...routine, blocks: [...routine.blocks, newBlock()] })}
      >
        {t('routineEditor.addBlock')}
      </button>
    </div>
  )
}
