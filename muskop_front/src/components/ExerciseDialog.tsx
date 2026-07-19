import { useState } from 'react'
import { SKILLS } from '../types/routine'
import { useI18n } from '../i18n/I18nContext'
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
  const { t } = useI18n()
  const [skill, setSkill] = useState(initial?.skill ?? SKILLS[0].id)
  const [level, setLevel] = useState(initial?.level ?? 1)
  const [description, setDescription] = useState(initial?.description ?? '')

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{t('dialogs.exercise.title', { name: title })}</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>

        <p className="muted">{t('dialogs.exercise.intro')}</p>

        <div className="tab-editor-meta">
          <label>
            {t('dialogs.exercise.skillLabel')}
            <select value={skill} onChange={(e) => setSkill(e.target.value)}>
              {SKILLS.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.icon} {t(`skills.${s.id}`)}
                </option>
              ))}
            </select>
          </label>
          <label>
            {t('dialogs.exercise.levelLabel')}
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
          {t('dialogs.exercise.descriptionLabel')}
          <textarea
            rows={3}
            placeholder={t('dialogs.exercise.descriptionPlaceholder')}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </label>

        <div className="modal-footer">
          {initial && (
            <button type="button" onClick={onRemove}>
              {t('dialogs.exercise.remove')}
            </button>
          )}
          <button
            type="button"
            className="primary"
            onClick={() => onSave({ skill, level, description: description.trim() })}
          >
            {initial ? t('dialogs.exercise.save') : t('dialogs.exercise.mark')}
          </button>
        </div>
      </div>
    </div>
  )
}
