import { useState } from 'react'
import type { EditorDocument } from './tab/tabModel'
import { EXAMPLE_JSON, importAuto } from '../utils/importers'

interface ImportDialogProps {
  onImport: (doc: EditorDocument, mode: 'replace' | 'append') => void
  onClose: () => void
}

/**
 * Importación de tablaturas: pegar JSON (v1/v2) o ASCII, o subir un archivo.
 * Pensado también para pegar directamente la estructura que devuelva un agente.
 */
export default function ImportDialog({ onImport, onClose }: ImportDialogProps) {
  const [text, setText] = useState('')
  const [mode, setMode] = useState<'replace' | 'append'>('replace')
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)

  const handleFile = async (file: File | undefined) => {
    if (!file) return
    setText(await file.text())
    setError(null)
  }

  const handleImport = () => {
    try {
      const doc = importAuto(text)
      onImport(doc, mode)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo importar')
    }
  }

  const copyExample = async () => {
    await navigator.clipboard.writeText(EXAMPLE_JSON)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Importar tablatura</h3>
          <button type="button" onClick={onClose}>✕</button>
        </div>

        <p className="muted">
          Pega el JSON del documento (formato v2 con secciones, también acepta el
          v1 antiguo) o una tablatura ASCII. Si se la pides a un agente, cópiale
          antes el formato de ejemplo.
        </p>

        <div className="import-actions">
          <label className="button">
            📄 Subir archivo
            <input
              type="file"
              accept=".json,.txt,application/json,text/plain"
              hidden
              onChange={(e) => handleFile(e.target.files?.[0])}
            />
          </label>
          <button type="button" onClick={copyExample}>
            {copied ? '✓ Copiado' : '📋 Copiar formato de ejemplo'}
          </button>
        </div>

        <textarea
          rows={14}
          placeholder='{"version": 2, "title": "...", "sections": [...]}'
          value={text}
          onChange={(e) => {
            setText(e.target.value)
            setError(null)
          }}
        />

        {error && <p className="error">{error}</p>}

        <div className="modal-footer">
          <label className="radio">
            <input
              type="radio"
              checked={mode === 'replace'}
              onChange={() => setMode('replace')}
            />
            Reemplazar el documento actual
          </label>
          <label className="radio">
            <input
              type="radio"
              checked={mode === 'append'}
              onChange={() => setMode('append')}
            />
            Añadir como secciones nuevas
          </label>
          <button type="button" className="primary" disabled={!text.trim()} onClick={handleImport}>
            Importar
          </button>
        </div>
      </div>
    </div>
  )
}
