import { useState } from 'react'
import type { ResourceSummary } from '../types/tab'
import type { Routine } from '../types/routine'
import { EXAMPLE_ROUTINE_JSON, importRoutineFromJson } from '../utils/routineIO'

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
      setError(err instanceof Error ? err.message : 'No se pudo importar')
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
          <h3>Importar rutina</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>

        <p className="muted">
          Pega el JSON de la rutina o sube un archivo. Los recursos se
          referencian por título (<code>resourceTitle</code>) y se buscan en tu
          librería al importar.
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
          placeholder='{"muskopRoutine": 1, "name": "...", "blocks": [...]}'
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
