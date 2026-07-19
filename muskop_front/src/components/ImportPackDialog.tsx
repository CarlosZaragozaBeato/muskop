import { useState } from 'react'
import {
  EXAMPLE_PACK_JSON,
  importExercisePack,
  type PackExerciseInput,
} from '../utils/exerciseIO'

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
      setError(err instanceof Error ? err.message : 'No se pudo importar')
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
          <h3>Importar pack de ejercicios</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>

        <p className="muted">
          Pega el JSON del pack o sube un archivo. Cada ejercicio se añade a tu
          librería con su habilidad y nivel, y aparece en Explorar.
        </p>

        <div className="import-actions">
          <label className="button">
            📄 Subir archivo
            <input
              type="file"
              accept=".json,application/json"
              hidden
              onChange={(e) => handleFile(e.target.files?.[0])}
            />
          </label>
          <button type="button" onClick={copyExample}>
            {copied ? '✓ Copiado' : '📋 Copiar formato de ejemplo'}
          </button>
        </div>

        <textarea
          rows={12}
          placeholder='{"muskopExercisePack": 1, "exercises": [...]}'
          value={text}
          onChange={(e) => {
            setText(e.target.value)
            setError(null)
          }}
        />

        {error && <p className="error">{error}</p>}

        <div className="modal-footer">
          <button type="button" className="primary" disabled={!text.trim()} onClick={handleImport}>
            Importar
          </button>
        </div>
      </div>
    </div>
  )
}
