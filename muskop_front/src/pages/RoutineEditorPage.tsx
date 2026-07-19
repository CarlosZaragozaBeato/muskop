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

const RESOURCE_TYPES_FOR_BLOCKS = ['TAB', 'CHORD', 'SNIPPET', 'THEORY']

export default function RoutineEditorPage() {
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
      .catch((err) => setError(err instanceof Error ? err.message : 'Error cargando la rutina'))
  }, [routineId])

  if (routine === null) {
    return <p className="muted">{error ?? 'Cargando…'}</p>
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
      setError('Ponle un nombre a la rutina')
      return
    }
    if (routine.blocks.length === 0) {
      setError('Añade al menos un bloque')
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
      setMessage('Rutina guardada')
      setTimeout(() => setMessage(null), 2500)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al guardar')
    } finally {
      setSaving(false)
    }
  }

  const total = routineMinutes(routine)

  return (
    <div className="routine-editor-page">
      <div className="page-header">
        <h2>{routineId === null ? 'Nueva rutina' : 'Editar rutina'}</h2>
        <div className="header-actions">
          <button type="button" onClick={() => navigate('/routines')}>← Rutinas</button>
          <button type="button" className="primary" disabled={saving} onClick={handleSave}>
            {saving ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </div>

      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}

      <div className="tab-editor-meta">
        <label>
          Nombre
          <input
            type="text"
            value={routine.name}
            placeholder="Rutina de fingerstyle"
            onChange={(e) => setRoutine({ ...routine, name: e.target.value })}
          />
        </label>
        <label>
          Descripción
          <input
            type="text"
            value={routine.description ?? ''}
            placeholder="Objetivo de la rutina…"
            onChange={(e) => setRoutine({ ...routine, description: e.target.value || null })}
          />
        </label>
        <label>
          Categoría
          <input
            type="text"
            list="routine-categories"
            value={routine.category ?? ''}
            placeholder="diaria, canción…"
            onChange={(e) => setRoutine({ ...routine, category: e.target.value || null })}
          />
          <datalist id="routine-categories">
            {ROUTINE_CATEGORIES.map((c) => (
              <option key={c} value={c} />
            ))}
          </datalist>
        </label>
        <span className="tab-editor-tuning">Total: {total} min</span>
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
                placeholder="Nombre del bloque (calentamiento, patrón…)"
                onChange={(e) => updateBlock(block.id, { name: e.target.value })}
              />
              <label>
                Min
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
              <label title="BPM objetivo; vacío = libre">
                BPM
                <input
                  type="number"
                  min={20}
                  max={300}
                  placeholder="libre"
                  value={block.bpm ?? ''}
                  onChange={(e) =>
                    updateBlock(block.id, { bpm: e.target.value ? Number(e.target.value) : null })
                  }
                />
              </label>
              <label title="Habilidad que entrena el bloque: gana experiencia al practicarlo">
                Habilidad
                <select
                  value={block.skill ?? ''}
                  onChange={(e) => updateBlock(block.id, { skill: e.target.value || null })}
                >
                  <option value="">—</option>
                  {SKILLS.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.icon} {s.label}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Recurso
                <select
                  value={block.resourceId ?? ''}
                  onChange={(e) =>
                    updateBlock(block.id, {
                      resourceId: e.target.value || null,
                    })
                  }
                >
                  <option value="">— sin recurso —</option>
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
              placeholder="Indicaciones: en qué fijarse durante este bloque…"
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
        + Añadir bloque
      </button>
    </div>
  )
}
