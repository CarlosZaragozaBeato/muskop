import { useState } from 'react'
import {
  EXAMPLE_PACK_JSON,
  importExercisePack,
  type PackExerciseInput,
} from '../utils/exerciseIO'
import { useI18n } from '../i18n/I18nContext'

interface ImportPackDialogProps {
  existingTitles: string[]
  onImport: (exercises: PackExerciseInput[], warnings: string[]) => void
  onClose: () => void
}

/** Importación de packs de ejercicios: pegar JSON o subir un archivo. */
export default function ImportPackDialog({
  existingTitles,
  onImport,
  onClose,
}: ImportPackDialogProps) {
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
      const result = importExercisePack(text, existingTitles)
      onImport(result.exercises, result.warnings)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('dialogs.importPack.importError'))
    }
  }

  const copyExample = async () => {
    await navigator.clipboard.writeText(EXAMPLE_PACK_JSON)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{t('dialogs.importPack.title')}</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>

        <p className="muted">{t('dialogs.importPack.intro')}</p>

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
          placeholder={t('dialogs.importPack.placeholder')}
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
