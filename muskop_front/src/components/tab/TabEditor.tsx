import { useCallback, useEffect, useRef, useState } from 'react'
import * as api from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { TabPlayer, type PlaybackPosition } from '../../audio/player'
import type { ChordShape, MeasureDTO } from '../../types/tab'
import ImportDialog from '../ImportDialog'
import ChordSectionEditor from './ChordSectionEditor'
import FingerstyleLegend from './FingerstyleLegend'
import GridSection, { type GridSelection } from './GridSection'
import InsertFromLibrary from './InsertFromLibrary'
import TabSvg from './TabSvg'
import {
  DURATIONS,
  FINGERS,
  MAX_FRET,
  SLOTS_PER_MEASURE,
  STRING_COUNT,
  TECHNIQUES,
  cloneMeasure,
  emptyMeasure,
  measureFromDto,
  measureToDto,
  newChordSection,
  newTabSection,
  sectionKindLabel,
  type Duration,
  type EditorCell,
  type EditorDocument,
  type EditorSection,
  type EditorTabSection,
} from './tabModel'
import { exportPdf, exportPng, exportText } from '../../utils/exporters'
import './TabEditor.css'

interface TabEditorProps {
  initialDoc: EditorDocument
  saving?: boolean
  onSave: (doc: EditorDocument) => void
}

export default function TabEditor({ initialDoc, saving, onSave }: TabEditorProps) {
  const { user } = useAuth()
  const [doc, setDoc] = useState<EditorDocument>(initialDoc)
  const [activeSectionId, setActiveSectionId] = useState<string | null>(null)
  const [gridSel, setGridSel] = useState<GridSelection | null>(null)
  const [duration, setDuration] = useState<Duration>('quarter')
  const [metronome, setMetronome] = useState(true)
  const [playing, setPlaying] = useState(false)
  const [playPos, setPlayPos] = useState<PlaybackPosition | null>(null)
  const [showPreview, setShowPreview] = useState(false)
  const [showImport, setShowImport] = useState(false)
  const [showLibrary, setShowLibrary] = useState(false)
  const [showExport, setShowExport] = useState(false)
  const [toast, setToast] = useState<string | null>(null)
  const [openNotes, setOpenNotes] = useState<Set<string>>(
    () => new Set(initialDoc.sections.filter((s) => s.notes).map((s) => s.id)),
  )

  const player = useRef<TabPlayer | null>(null)
  useEffect(() => {
    player.current = new TabPlayer()
    return () => player.current?.stop()
  }, [])

  const notify = (message: string) => {
    setToast(message)
    setTimeout(() => setToast(null), 2500)
  }

  // ------------------------------------------------------------------
  // Actualizaciones de documento
  // ------------------------------------------------------------------

  const updateSection = useCallback(
    (sectionId: string, updater: (section: EditorSection) => EditorSection) => {
      setDoc((prev) => ({
        ...prev,
        sections: prev.sections.map((s) => (s.id === sectionId ? updater(s) : s)),
      }))
    },
    [],
  )

  const updateTabSection = useCallback(
    (sectionId: string, updater: (section: EditorTabSection) => EditorTabSection) => {
      updateSection(sectionId, (s) => (s.kind === 'chords' ? s : updater(s)))
    },
    [updateSection],
  )

  const setCell = useCallback(
    (sectionId: string, sel: GridSelection, fret: number | null, dur: Duration) => {
      updateTabSection(sectionId, (section) => ({
        ...section,
        measures: section.measures.map((measure, mi) => {
          if (mi !== sel.measure) return measure
          const cells = measure.cells.map((row, si) =>
            si === sel.string
              ? row.map((c, ci) => {
                  if (ci !== sel.slot) return c
                  if (fret === null) return null
                  // al cambiar el traste se conserva la digitación existente
                  return { fret, finger: c?.finger ?? null, technique: c?.technique ?? null }
                })
              : row,
          )
          const durations = [...measure.durations]
          const columnHasNotes = cells.some((row) => row[sel.slot] !== null)
          durations[sel.slot] = columnHasNotes
            ? fret !== null
              ? dur
              : durations[sel.slot]
            : null
          return { cells, durations }
        }),
      }))
    },
    [updateTabSection],
  )

  /** Modifica la nota seleccionada (dedo/técnica) si existe. */
  const updateNote = useCallback(
    (sectionId: string, sel: GridSelection, updater: (cell: EditorCell) => EditorCell) => {
      updateTabSection(sectionId, (section) => ({
        ...section,
        measures: section.measures.map((measure, mi) => {
          if (mi !== sel.measure) return measure
          const cells = measure.cells.map((row, si) =>
            si === sel.string
              ? row.map((c, ci) => (ci === sel.slot && c !== null ? updater(c) : c))
              : row,
          )
          return { ...measure, cells }
        }),
      }))
    },
    [updateTabSection],
  )

  // ------------------------------------------------------------------
  // Teclado sobre la rejilla
  // ------------------------------------------------------------------

  const handleGridKey = (section: EditorTabSection) => (e: React.KeyboardEvent) => {
    if (activeSectionId !== section.id || gridSel === null) return

    if (e.key >= '0' && e.key <= '9') {
      e.preventDefault()
      const digit = Number(e.key)
      const current =
        section.measures[gridSel.measure]?.cells[gridSel.string][gridSel.slot]?.fret ?? null
      const combined = current !== null ? current * 10 + digit : digit
      setCell(section.id, gridSel, combined <= MAX_FRET ? combined : digit, duration)
      return
    }

    // digitación de mano derecha (p, i, m, a) y técnicas (h, o, s, b, v, x, n)
    const key = e.key.toLowerCase()
    const finger = FINGERS.find((f) => f.key === key)
    if (finger) {
      e.preventDefault()
      updateNote(section.id, gridSel, (cell) => ({
        ...cell,
        finger: cell.finger === finger.value ? null : finger.value,
      }))
      return
    }
    const technique = TECHNIQUES.find((t) => t.key === key)
    if (technique) {
      e.preventDefault()
      updateNote(section.id, gridSel, (cell) => ({
        ...cell,
        technique: cell.technique === technique.value ? null : technique.value,
      }))
      return
    }

    const move = (dString: number, dSlot: number) => {
      setGridSel((sel) => {
        if (!sel) return sel
        const string = Math.min(STRING_COUNT - 1, Math.max(0, sel.string + dString))
        let slot = sel.slot + dSlot
        let measure = sel.measure
        if (slot < 0) {
          if (measure > 0) {
            measure -= 1
            slot = SLOTS_PER_MEASURE - 1
          } else slot = 0
        } else if (slot >= SLOTS_PER_MEASURE) {
          if (measure < section.measures.length - 1) {
            measure += 1
            slot = 0
          } else slot = SLOTS_PER_MEASURE - 1
        }
        return { measure, string, slot }
      })
    }

    switch (e.key) {
      case 'Backspace':
      case 'Delete':
        e.preventDefault()
        setCell(section.id, gridSel, null, duration)
        break
      case 'ArrowUp':
        e.preventDefault()
        move(-1, 0)
        break
      case 'ArrowDown':
        e.preventDefault()
        move(1, 0)
        break
      case 'ArrowLeft':
        e.preventDefault()
        move(0, -1)
        break
      case 'ArrowRight':
        e.preventDefault()
        move(0, 1)
        break
    }
  }

  // ------------------------------------------------------------------
  // Secciones
  // ------------------------------------------------------------------

  const addSection = (kind: EditorSection['kind']) => {
    const section =
      kind === 'chords'
        ? newChordSection()
        : newTabSection(kind, kind === 'tab' ? `Sección ${doc.sections.length + 1}` : undefined)
    setDoc((prev) => ({ ...prev, sections: [...prev.sections, section] }))
  }

  const removeSection = (sectionId: string) => {
    setDoc((prev) => ({
      ...prev,
      sections: prev.sections.filter((s) => s.id !== sectionId),
    }))
    if (activeSectionId === sectionId) {
      setActiveSectionId(null)
      setGridSel(null)
    }
  }

  const moveSection = (sectionId: string, delta: number) => {
    setDoc((prev) => {
      const idx = prev.sections.findIndex((s) => s.id === sectionId)
      const target = idx + delta
      if (idx < 0 || target < 0 || target >= prev.sections.length) return prev
      const sections = [...prev.sections]
      ;[sections[idx], sections[target]] = [sections[target], sections[idx]]
      return { ...prev, sections }
    })
  }

  // ------------------------------------------------------------------
  // Compases y snippets
  // ------------------------------------------------------------------

  const addMeasure = (sectionId: string) =>
    updateTabSection(sectionId, (s) => ({ ...s, measures: [...s.measures, emptyMeasure()] }))

  const removeLastMeasure = (sectionId: string) =>
    updateTabSection(sectionId, (s) =>
      s.measures.length > 1 ? { ...s, measures: s.measures.slice(0, -1) } : s,
    )

  const duplicateSelectedMeasure = (sectionId: string) => {
    if (activeSectionId !== sectionId || gridSel === null) return
    updateTabSection(sectionId, (s) => {
      const measures = [...s.measures]
      measures.splice(gridSel.measure + 1, 0, cloneMeasure(s.measures[gridSel.measure]))
      return { ...s, measures }
    })
  }

  const saveMeasureAsSnippet = async (section: EditorTabSection) => {
    if (!user || activeSectionId !== section.id || gridSel === null) return
    const name = window.prompt('Nombre del fragmento reutilizable:')
    if (!name) return
    try {
      await api.createResource(user.id, {
        title: name,
        type: 'SNIPPET',
        category: doc.category,
        content: {
          kind: 'fragment',
          tuning: doc.tuning,
          measures: [measureToDto(section.measures[gridSel.measure], 0)],
        },
      })
      notify(`Fragmento «${name}» guardado en la librería`)
    } catch (err) {
      notify(err instanceof Error ? err.message : 'Error guardando el fragmento')
    }
  }

  const saveChordToLibrary = async (chord: ChordShape) => {
    if (!user) return
    const name = window.prompt('Nombre con el que guardarlo:', chord.name || '')
    if (!name) return
    try {
      await api.createResource(user.id, {
        title: name,
        type: 'CHORD',
        category: doc.category,
        content: { kind: 'chord', chord: { ...chord, name: chord.name || name } },
      })
      notify(`Acorde «${name}» guardado en la librería`)
    } catch (err) {
      notify(err instanceof Error ? err.message : 'Error guardando el acorde')
    }
  }

  const insertChord = (chord: ChordShape) => {
    const active = doc.sections.find((s) => s.id === activeSectionId)
    // En una sección de acordes se añade al final; en una rejilla se
    // "estampa" el acorde en la columna seleccionada.
    if (active?.kind === 'chords') {
      updateSection(active.id, (s) =>
        s.kind === 'chords' ? { ...s, chords: [...s.chords, chord] } : s,
      )
      notify(`Acorde ${chord.name} añadido a «${active.name}»`)
      return
    }
    if (active && gridSel !== null) {
      chord.frets.forEach((fret, i) => {
        if (fret === null) return
        const editorString = STRING_COUNT - 1 - i
        setCell(active.id, { ...gridSel, string: editorString }, fret, duration)
      })
      notify(`Acorde ${chord.name} incrustado en el compás ${gridSel.measure + 1}`)
      return
    }
    notify('Selecciona antes una casilla o una sección de acordes')
  }

  const insertFragment = (measures: MeasureDTO[]) => {
    const active = doc.sections.find((s) => s.id === activeSectionId && s.kind !== 'chords')
    const targetId =
      active?.id ?? doc.sections.find((s) => s.kind !== 'chords')?.id ?? null
    if (!targetId) {
      notify('No hay ninguna sección de tablatura donde insertar')
      return
    }
    const insertAt =
      active && gridSel !== null ? gridSel.measure + 1 : undefined
    updateTabSection(targetId, (s) => {
      const converted = measures.map(measureFromDto)
      const result = [...s.measures]
      result.splice(insertAt ?? result.length, 0, ...converted)
      return { ...s, measures: result }
    })
    notify('Fragmento incrustado')
  }

  // ------------------------------------------------------------------
  // Reproducción
  // ------------------------------------------------------------------

  const togglePlay = () => {
    if (playing) {
      player.current?.stop()
      setPlaying(false)
      setPlayPos(null)
      return
    }
    setPlaying(true)
    player.current?.play(doc, {
      metronome,
      onTick: setPlayPos,
      onEnd: () => {
        setPlaying(false)
        setPlayPos(null)
      },
    })
  }

  // ------------------------------------------------------------------
  // Render
  // ------------------------------------------------------------------

  const activeSection = doc.sections.find((s) => s.id === activeSectionId)
  const selectedCell =
    activeSection && activeSection.kind !== 'chords' && gridSel !== null
      ? (activeSection.measures[gridSel.measure]?.cells[gridSel.string]?.[gridSel.slot] ?? null)
      : null

  const toggleNotes = (sectionId: string) => {
    setOpenNotes((prev) => {
      const next = new Set(prev)
      if (next.has(sectionId)) next.delete(sectionId)
      else next.add(sectionId)
      return next
    })
  }

  return (
    <div className="tab-editor">
      <div className="tab-editor-meta">
        <label>
          Título
          <input
            type="text"
            value={doc.title}
            placeholder="Mi ejercicio"
            onChange={(e) => setDoc({ ...doc, title: e.target.value })}
          />
        </label>
        <label>
          Categoría
          <input
            type="text"
            list="categories"
            value={doc.category ?? ''}
            placeholder="tecnica, ritmo…"
            onChange={(e) => setDoc({ ...doc, category: e.target.value || null })}
          />
          <datalist id="categories">
            <option value="tecnica" />
            <option value="ritmo" />
            <option value="acordes" />
            <option value="arpegios" />
            <option value="repertorio" />
            <option value="calentamiento" />
          </datalist>
        </label>
        <label>
          BPM base
          <input
            type="number"
            min={20}
            max={300}
            value={doc.baseBpm}
            onChange={(e) => setDoc({ ...doc, baseBpm: Number(e.target.value) })}
          />
        </label>
        <span className="tab-editor-tuning">
          {doc.tuning.join(' ')} · {doc.timeSignature}
        </span>
      </div>

      <div className="tab-editor-toolbar">
        <div className="duration-group" role="group" aria-label="Duración">
          {DURATIONS.map((d) => (
            <button
              key={d.value}
              type="button"
              className={duration === d.value ? 'active' : ''}
              title={`${d.label} (nuevas notas)`}
              onClick={() => setDuration(d.value)}
            >
              {d.symbol}
            </button>
          ))}
        </div>

        <button type="button" className={playing ? 'active' : ''} onClick={togglePlay}>
          {playing ? '⏹ Parar' : '▶ Reproducir'}
        </button>
        <label className="check">
          <input
            type="checkbox"
            checked={metronome}
            onChange={(e) => setMetronome(e.target.checked)}
          />
          Metrónomo
        </label>

        <span className="toolbar-sep" />

        <button type="button" onClick={() => setShowImport(true)}>⇪ Importar</button>
        <div className="export-menu">
          <button type="button" onClick={() => setShowExport((v) => !v)}>⇩ Exportar</button>
          {showExport && (
            <div className="export-dropdown">
              <button
                type="button"
                onClick={() => {
                  exportText([doc])
                  setShowExport(false)
                }}
              >
                Texto (.txt)
              </button>
              <button
                type="button"
                onClick={() => {
                  exportPng(doc).catch((err) => notify(err.message))
                  setShowExport(false)
                }}
              >
                Imagen (.png)
              </button>
              <button
                type="button"
                onClick={() => {
                  try {
                    exportPdf([doc])
                  } catch (err) {
                    notify(err instanceof Error ? err.message : 'Error')
                  }
                  setShowExport(false)
                }}
              >
                PDF (imprimir)
              </button>
            </div>
          )}
        </div>
        <button type="button" onClick={() => setShowLibrary((v) => !v)}>
          ⧉ Incrustar
        </button>
        <button type="button" onClick={() => setShowPreview((v) => !v)}>
          {showPreview ? 'Ocultar vista previa' : 'Vista previa'}
        </button>

        <span className="toolbar-sep" />

        <button type="button" className="primary" disabled={saving} onClick={() => onSave(doc)}>
          {saving ? 'Guardando…' : 'Guardar'}
        </button>
      </div>

      {activeSection && activeSection.kind !== 'chords' && gridSel !== null && (
        <div className="annotation-bar">
          <span className="muted">Mano derecha:</span>
          {FINGERS.map((f) => (
            <button
              key={f.value}
              type="button"
              className={'finger-btn' + (selectedCell?.finger === f.value ? ' active' : '')}
              style={{ '--finger-color': f.color } as React.CSSProperties}
              title={`${f.label} (tecla ${f.key})`}
              disabled={selectedCell === null}
              onClick={() =>
                updateNote(activeSection.id, gridSel, (cell) => ({
                  ...cell,
                  finger: cell.finger === f.value ? null : f.value,
                }))
              }
            >
              {f.value}
            </button>
          ))}
          <span className="toolbar-sep" />
          <span className="muted">Técnica:</span>
          {TECHNIQUES.map((t) => (
            <button
              key={t.value}
              type="button"
              className={selectedCell?.technique === t.value ? 'active' : ''}
              title={`${t.label}: ${t.hint} (tecla ${t.key})`}
              disabled={selectedCell === null}
              onClick={() =>
                updateNote(activeSection.id, gridSel, (cell) => ({
                  ...cell,
                  technique: cell.technique === t.value ? null : t.value,
                }))
              }
            >
              {t.symbol}
            </button>
          ))}
          {selectedCell === null && (
            <span className="muted annotation-hint">— escribe antes un traste en la casilla</span>
          )}
        </div>
      )}

      {toast && <p className="toast">{toast}</p>}

      {showLibrary && (
        <InsertFromLibrary
          onInsertChord={insertChord}
          onInsertFragment={insertFragment}
          onClose={() => setShowLibrary(false)}
        />
      )}

      <p className="tab-editor-help">
        Clic en una casilla y teclea el traste (0–24). Flechas para moverte,
        Retroceso para borrar. La regla superior marca los pulsos y debajo de
        cada compás se ven las duraciones. Cada sección puede tener su propio BPM.
      </p>

      {doc.sections.map((section, si) => (
        <div className="section" key={section.id}>
          <div className="section-header">
            <span className={`badge badge-${section.kind}`}>
              {sectionKindLabel(section.kind)}
            </span>
            <input
              type="text"
              className="section-name"
              value={section.name}
              onChange={(e) =>
                updateSection(section.id, (s) => ({ ...s, name: e.target.value }))
              }
            />
            <label className="section-bpm" title="BPM propio; vacío = BPM base">
              BPM
              <input
                type="number"
                min={20}
                max={300}
                placeholder={String(doc.baseBpm)}
                value={section.bpm ?? ''}
                onChange={(e) =>
                  updateSection(section.id, (s) => ({
                    ...s,
                    bpm: e.target.value ? Number(e.target.value) : null,
                  }))
                }
              />
            </label>

            {section.kind !== 'chords' && (
              <>
                <button type="button" onClick={() => addMeasure(section.id)}>+ Compás</button>
                <button
                  type="button"
                  disabled={section.measures.length <= 1}
                  onClick={() => removeLastMeasure(section.id)}
                >
                  − Compás
                </button>
                <button
                  type="button"
                  disabled={activeSectionId !== section.id || gridSel === null}
                  title="Duplica el compás seleccionado"
                  onClick={() => duplicateSelectedMeasure(section.id)}
                >
                  ⧉ Duplicar
                </button>
                <button
                  type="button"
                  disabled={activeSectionId !== section.id || gridSel === null}
                  title="Guarda el compás seleccionado como fragmento reutilizable"
                  onClick={() => saveMeasureAsSnippet(section)}
                >
                  ♥ Compás → snippet
                </button>
              </>
            )}

            <button
              type="button"
              className={openNotes.has(section.id) || section.notes ? 'active' : ''}
              title="Notas de estudio: cómo tocar esta sección"
              onClick={() => toggleNotes(section.id)}
            >
              📝 Notas
            </button>

            <span className="section-spacer" />
            <button type="button" disabled={si === 0} onClick={() => moveSection(section.id, -1)}>
              ↑
            </button>
            <button
              type="button"
              disabled={si === doc.sections.length - 1}
              onClick={() => moveSection(section.id, 1)}
            >
              ↓
            </button>
            <button
              type="button"
              disabled={doc.sections.length <= 1}
              title="Eliminar sección"
              onClick={() => removeSection(section.id)}
            >
              ✕
            </button>
          </div>

          {(openNotes.has(section.id) || section.notes) && (
            <textarea
              className="section-notes"
              rows={2}
              placeholder="Notas de estudio: postura, qué practicar despacio, en qué fijarse…"
              value={section.notes ?? ''}
              onChange={(e) =>
                updateSection(section.id, (s) => ({ ...s, notes: e.target.value || null }))
              }
            />
          )}

          {section.kind === 'chords' ? (
            <ChordSectionEditor
              section={section}
              playChordIndex={
                playPos?.sectionIndex === si ? (playPos.chordIndex ?? null) : null
              }
              onChange={(chords) =>
                updateSection(section.id, (s) =>
                  s.kind === 'chords' ? { ...s, chords } : s,
                )
              }
              onSaveChord={saveChordToLibrary}
            />
          ) : (
            <GridSection
              section={section}
              tuning={doc.tuning}
              selection={activeSectionId === section.id ? gridSel : null}
              playPos={
                playPos?.sectionIndex === si && playPos.measureIndex !== undefined
                  ? { measureIndex: playPos.measureIndex, slot: playPos.slot ?? 0 }
                  : null
              }
              onSelect={(sel) => {
                setActiveSectionId(section.id)
                setGridSel(sel)
              }}
              onKeyDown={handleGridKey(section)}
            />
          )}

          {section.kind === 'fingerstyle' && <FingerstyleLegend />}
        </div>
      ))}

      <div className="add-section">
        <span className="muted">Añadir sección:</span>
        <button type="button" onClick={() => addSection('tab')}>+ Tablatura</button>
        <button type="button" onClick={() => addSection('chords')}>+ Acordes</button>
        <button type="button" onClick={() => addSection('arpeggio')}>+ Arpegio</button>
        <button type="button" onClick={() => addSection('fingerstyle')}>+ Fingerstyle</button>
      </div>

      {showPreview && (
        <div className="preview-panel">
          <TabSvg doc={doc} ink="#e5e7eb" background="#16171d" />
        </div>
      )}

      {showImport && (
        <ImportDialog
          onClose={() => setShowImport(false)}
          onImport={(imported, mode) => {
            setDoc((prev) =>
              mode === 'replace'
                ? { ...imported, title: imported.title || prev.title }
                : { ...prev, sections: [...prev.sections, ...imported.sections] },
            )
            setShowImport(false)
            setActiveSectionId(null)
            setGridSel(null)
            notify(mode === 'replace' ? 'Documento importado' : 'Secciones añadidas')
          }}
        />
      )}
    </div>
  )
}
