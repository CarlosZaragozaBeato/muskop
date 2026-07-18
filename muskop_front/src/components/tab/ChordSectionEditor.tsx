import { useState } from 'react'
import type { ChordShape } from '../../types/tab'
import ChordDiagram from './ChordDiagram'
import { COMMON_CHORDS, emptyChord } from './chords'
import { chordFretsLabel, type EditorChordSection } from './tabModel'

interface ChordSectionEditorProps {
  section: EditorChordSection
  playChordIndex: number | null
  onChange: (chords: ChordShape[]) => void
  /** Guardar un acorde como recurso reutilizable en la librería */
  onSaveChord?: (chord: ChordShape) => void
}

export default function ChordSectionEditor({
  section,
  playChordIndex,
  onChange,
  onSaveChord,
}: ChordSectionEditorProps) {
  const [libraryChord, setLibraryChord] = useState('')

  const update = (index: number, chord: ChordShape) => {
    onChange(section.chords.map((c, i) => (i === index ? chord : c)))
  }

  const remove = (index: number) => {
    onChange(section.chords.filter((_, i) => i !== index))
  }

  const move = (index: number, delta: number) => {
    const target = index + delta
    if (target < 0 || target >= section.chords.length) return
    const chords = [...section.chords]
    ;[chords[index], chords[target]] = [chords[target], chords[index]]
    onChange(chords)
  }

  const addFromLibrary = (name: string) => {
    const found = COMMON_CHORDS.find((c) => c.name === name)
    if (found) {
      onChange([...section.chords, { ...found, frets: [...found.frets] }])
    }
    setLibraryChord('')
  }

  return (
    <div className="chord-section">
      <div className="chord-list">
        {section.chords.map((chord, i) => (
          <div
            key={i}
            className={'chord-card' + (playChordIndex === i ? ' playing' : '')}
          >
            <input
              type="text"
              className="chord-name"
              value={chord.name}
              placeholder="Nombre"
              onChange={(e) => update(i, { ...chord, name: e.target.value })}
            />
            <ChordDiagram chord={chord} width={104} onChange={(c) => update(i, c)} />
            <span className="chord-frets muted">{chordFretsLabel(chord)}</span>
            <div className="chord-controls">
              <label title="Traste base del diagrama">
                Traste
                <input
                  type="number"
                  min={1}
                  max={21}
                  value={chord.baseFret ?? 1}
                  onChange={(e) =>
                    update(i, { ...chord, baseFret: Math.max(1, Number(e.target.value)) })
                  }
                />
              </label>
              <label title="Pulsos que suena al reproducir">
                Pulsos
                <input
                  type="number"
                  min={1}
                  max={16}
                  value={chord.beats ?? 4}
                  onChange={(e) =>
                    update(i, { ...chord, beats: Math.max(1, Number(e.target.value)) })
                  }
                />
              </label>
            </div>
            <div className="chord-actions">
              <button type="button" title="Mover a la izquierda" onClick={() => move(i, -1)}>←</button>
              <button type="button" title="Mover a la derecha" onClick={() => move(i, 1)}>→</button>
              {onSaveChord && (
                <button
                  type="button"
                  title="Guardar en la librería para reutilizar"
                  onClick={() => onSaveChord(chord)}
                >
                  ♥
                </button>
              )}
              <button type="button" title="Eliminar acorde" onClick={() => remove(i)}>✕</button>
            </div>
          </div>
        ))}
      </div>

      <div className="chord-add">
        <select
          value={libraryChord}
          onChange={(e) => addFromLibrary(e.target.value)}
        >
          <option value="">+ Acorde común…</option>
          {COMMON_CHORDS.map((c) => (
            <option key={c.name} value={c.name}>
              {c.name} ({chordFretsLabel(c)})
            </option>
          ))}
        </select>
        <button type="button" onClick={() => onChange([...section.chords, emptyChord()])}>
          + Acorde vacío
        </button>
      </div>
    </div>
  )
}
