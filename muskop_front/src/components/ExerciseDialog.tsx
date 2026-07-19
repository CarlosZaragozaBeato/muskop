import { useState } from 'react'
import { SKILLS } from '../types/routine'
import type { ExerciseMeta } from '../types/tab'

interface ExerciseDialogProps {
  title: string
  initial: ExerciseMeta | null
  onSave: (meta: ExerciseMeta) => void
  onRemove: () => void
  onClose: () => void
}

/**
 * Marca un recurso de la librería como ejercicio guiado: habilidad, nivel y
 * descripción. Estos metadatos hacen que aparezca en Explorar y en las
 * recomendaciones por nivel de Progreso.
 */
export default function ExerciseDialog({
  title,
  initial,
  onSave,
  onRemove,
  onClose,
}: ExerciseDialogProps) {
  const [skill, setSkill] = useState(initial?.skill ?? SKILLS[0].id)
  const [level, setLevel] = useState(initial?.level ?? 1)
  const [description, setDescription] = useState(initial?.description ?? '')

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Ejercicio: {title}</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>

        <p className="muted">
          Al marcarlo como ejercicio aparecerá en Explorar y en las
          recomendaciones de tu nivel en Progreso.
        </p>

        <div className="tab-editor-meta">
          <label>
            Habilidad
            <select value={skill} onChange={(e) => setSkill(e.target.value)}>
              {SKILLS.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.icon} {s.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Nivel
            <input
              type="number"
              min={1}
              max={20}
              value={level}
              onChange={(e) => setLevel(Math.max(1, Math.round(Number(e.target.value) || 1)))}
            />
          </label>
        </div>

        <label>
          Descripción
          <textarea
            rows={3}
            placeholder="Qué se practica y en qué fijarse"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </label>

        <div className="modal-footer">
          {initial && (
            <button type="button" onClick={onRemove}>
              Quitar ejercicio
            </button>
          )}
          <button
            type="button"
            className="primary"
            onClick={() => onSave({ skill, level, description: description.trim() })}
          >
            {initial ? 'Guardar' : 'Marcar como ejercicio'}
          </button>
        </div>
      </div>
    </div>
  )
}
