import { useState } from 'react'
import type { ResourceSummary } from '../types/tab'
import type { Routine } from '../types/routine'
import { EXAMPLE_ROUTINE_JSON, importRoutineFromJson } from '../utils/routineIO'
import { useI18n } from '../i18n/I18nContext'

interface ImportRoutineDialogProps {
  resources: ResourceSummary[]
  onImport: (routine: Omit<Routine, 'id'>, warnings: string[]) => void
  onClose: () => void
}

/** Importación de rutinas: pegar JSON o subir un archivo. */
export default function ImportRoutineDialog({
  resources,
  onImport,
  onClose,
}: ImportRoutineDialogProps) {
  const { t } = useI18n()
  const [text, setText] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const handleFile = async (file: File | undefined) => {
    if (!file) return
    setText(await file.text())
    setError(null)
  }

  const handleImport = () => {
    try {
      const result = importRoutineFromJson(text, resources)
      onImport(result.routine, result.warnings)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('dialogs.importRoutine.importError'))
    }
  }

  const copyExample = async () => {
    await navigator.clipboard.writeText(EXAMPLE_ROUTINE_JSON)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{t('dialogs.importRoutine.title')}</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>

        <p className="muted">{t('dialogs.importRoutine.intro')}</p>

        <div className="import-actions">
          <label className="button">
            📄 {t('common.uploadFile')}
            <input
              type="file"
              accept=".json,application/json"
              hidden
              onChange={(e) => handleFile(e.target.files?.[0])}
            />
          </label>
          <button type="button" onClick={copyExample}>
            {copied ? `✓ ${t('common.copied')}` : `📋 ${t('common.copyExample')}`}
          </button>
        </div>

        <textarea
          rows={12}
          placeholder={t('dialogs.importRoutine.placeholder')}
          value={text}
          onChange={(e) => {
            setText(e.target.value)
            setError(null)
          }}
        />

        {error && <p className="error">{error}</p>}

        <div className="modal-footer">
          <button type="button" className="primary" disabled={!text.trim()} onClick={handleImport}>
            {t('common.import')}
          </button>
        </div>
      </div>
    </div>
  )
}
