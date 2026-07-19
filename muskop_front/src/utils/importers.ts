import {
  MAX_FRET,
  SLOTS_PER_MEASURE,
  STANDARD_TUNING,
  STRING_COUNT,
  TECHNIQUES,
  emptyMeasure,
  fromDocument,
  newId,
  parseTabContent,
  type EditorDocument,
  type EditorMeasure,
} from '../components/tab/tabModel'
import type { TabDocument } from '../types/tab'
import { translate as tr } from '../i18n/translate'

const TECHNIQUE_VALUES = new Set<string>(TECHNIQUES.map((t) => t.value))

// ==========================================================================
// Importación: JSON (v1/v2, pegado o archivo) y ASCII best-effort.
// ==========================================================================

export function importFromJson(raw: string): EditorDocument {
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    throw new Error(tr('errors.notJson'))
  }
  const doc = parseTabContent(parsed)
  validateDocument(doc)
  return fromDocument(doc)
}

function validateDocument(doc: TabDocument) {
  if (!Array.isArray(doc.sections)) {
    throw new Error(tr('errors.missingSections'))
  }
  doc.sections.forEach((section, i) => {
    const name = section.name ?? tr('errors.unnamedSection')
    if (section.kind === 'chords') {
      if (!Array.isArray(section.chords)) {
        throw new Error(tr('errors.sectionNoChords', { n: i + 1, name }))
      }
      return
    }
    if (!Array.isArray(section.measures)) {
      throw new Error(tr('errors.sectionNoMeasures', { n: i + 1, name }))
    }
    section.measures.forEach((measure, mi) => {
      const s = i + 1
      const m = mi + 1
      for (const event of measure.events ?? []) {
        if (typeof event.beat !== 'number' || event.beat < 1 || event.beat >= 5) {
          throw new Error(tr('errors.beatRange', { s, m, beat: event.beat }))
        }
        for (const note of event.notes ?? []) {
          if (note.string < 1 || note.string > STRING_COUNT) {
            throw new Error(tr('errors.stringRange', { s, m, string: note.string }))
          }
          if (note.fret < 0 || note.fret > MAX_FRET) {
            throw new Error(tr('errors.fretRange', { s, m, fret: note.fret, max: MAX_FRET }))
          }
          if (note.finger && !['p', 'i', 'm', 'a'].includes(note.finger)) {
            throw new Error(tr('errors.fingerInvalid', { s, m, finger: note.finger }))
          }
          if (note.technique && !TECHNIQUE_VALUES.has(note.technique)) {
            throw new Error(
              tr('errors.techniqueInvalid', {
                s,
                m,
                technique: note.technique,
                list: [...TECHNIQUE_VALUES].join(', '),
              }),
            )
          }
        }
      }
    })
  })
}

/**
 * Importa tablatura ASCII de forma aproximada: espera bloques de 6 líneas
 * (aguda arriba) con compases separados por "|"; cada carácter dentro del
 * compás se trata como una semicorchea.
 */
export function importFromAscii(raw: string): EditorDocument {
  const lines = raw
    .split('\n')
    .map((l) => l.trimEnd())
    .filter((l) => /\|/.test(l) && /-/.test(l))

  if (lines.length < STRING_COUNT) {
    throw new Error(tr('errors.noAsciiLines'))
  }

  const block = lines.slice(0, STRING_COUNT).map((line) => {
    const start = line.indexOf('|')
    return line.slice(start + 1)
  })

  // divide en compases por las barras
  const measureChunks: string[][] = []
  let position = 0
  const length = Math.min(...block.map((l) => l.length))
  let chunkStart = 0
  while (position <= length) {
    const isBar = position === length || block.every((l) => l[position] === '|')
    if (isBar) {
      if (position > chunkStart) {
        measureChunks.push(block.map((l) => l.slice(chunkStart, position)))
      }
      chunkStart = position + 1
    }
    position++
  }

  const measures: EditorMeasure[] = measureChunks.map((chunk) => {
    const measure = emptyMeasure()
    const chunkLen = Math.max(...chunk.map((c) => c.length))
    for (let s = 0; s < STRING_COUNT; s++) {
      const text = chunk[s] ?? ''
      let i = 0
      while (i < text.length) {
        if (/\d/.test(text[i])) {
          let j = i
          while (j + 1 < text.length && /\d/.test(text[j + 1])) j++
          const fret = Number(text.slice(i, j + 1))
          const slot = Math.min(
            SLOTS_PER_MEASURE - 1,
            Math.round((i / chunkLen) * SLOTS_PER_MEASURE),
          )
          if (fret <= MAX_FRET) {
            measure.cells[s][slot] = { fret, finger: null, technique: null }
            measure.durations[slot] = measure.durations[slot] ?? 'sixteenth'
          }
          i = j + 1
        } else {
          i++
        }
      }
    }
    return measure
  })

  if (measures.length === 0) {
    throw new Error(tr('errors.noMeasuresExtracted'))
  }

  return {
    title: '',
    category: null,
    tuning: [...STANDARD_TUNING],
    timeSignature: '4/4',
    baseBpm: 120,
    sections: [
      { id: newId(), kind: 'tab', name: tr('errors.importedSection'), bpm: null, notes: null, measures },
    ],
  }
}

export function importAuto(raw: string): EditorDocument {
  const trimmed = raw.trim()
  if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
    return importFromJson(trimmed)
  }
  return importFromAscii(raw)
}

/** Ejemplo de formato JSON, pensado para copiárselo a un agente. */
export const EXAMPLE_JSON = JSON.stringify(
  {
    version: 2,
    title: 'Mi ejercicio',
    category: 'tecnica',
    tuning: ['E', 'A', 'D', 'G', 'B', 'E'],
    timeSignature: '4/4',
    baseBpm: 100,
    sections: [
      {
        kind: 'tab',
        name: 'Riff principal',
        bpm: 90,
        measures: [
          {
            events: [
              { beat: 1, duration: 'quarter', notes: [{ string: 6, fret: 0 }] },
              { beat: 2, duration: 'eighth', notes: [{ string: 5, fret: 2 }] },
              { beat: 2.5, duration: 'eighth', notes: [{ string: 4, fret: 2 }] },
              {
                beat: 3,
                duration: 'half',
                notes: [
                  { string: 3, fret: 1 },
                  { string: 2, fret: 0 },
                ],
              },
            ],
          },
        ],
      },
      {
        kind: 'fingerstyle',
        name: 'Patrón fingerstyle',
        bpm: 70,
        notes: 'El pulgar lleva el bajo; deja sonar las cuerdas al aire.',
        measures: [
          {
            events: [
              { beat: 1, duration: 'eighth', notes: [{ string: 6, fret: 0, finger: 'p' }] },
              { beat: 1.5, duration: 'eighth', notes: [{ string: 3, fret: 0, finger: 'i' }] },
              { beat: 2, duration: 'eighth', notes: [{ string: 2, fret: 1, finger: 'm' }] },
              { beat: 2.5, duration: 'eighth', notes: [{ string: 1, fret: 0, finger: 'a' }] },
              {
                beat: 3,
                duration: 'quarter',
                notes: [{ string: 2, fret: 3, finger: 'm', technique: 'hammer' }],
              },
              {
                beat: 4,
                duration: 'quarter',
                notes: [{ string: 6, fret: 12, finger: 'p', technique: 'harmonic' }],
              },
            ],
          },
        ],
      },
      {
        kind: 'chords',
        name: 'Acordes del estribillo',
        chords: [
          { name: 'Am', frets: [null, 0, 2, 2, 1, 0], beats: 4 },
          { name: 'F', frets: [1, 3, 3, 2, 1, 1], beats: 4 },
          { name: 'C', frets: [null, 3, 2, 0, 1, 0], beats: 4 },
          { name: 'G', frets: [3, 2, 0, 0, 0, 3], beats: 4 },
        ],
      },
    ],
  },
  null,
  2,
)
