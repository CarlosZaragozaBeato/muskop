import type {
  ChordSectionDTO,
  ChordShape,
  Duration,
  Finger,
  GuitarTabV1,
  MeasureDTO,
  NoteDTO,
  SectionDTO,
  TabDocument,
  TabSectionDTO,
  Technique,
} from '../../types/tab'

export const STRING_COUNT = 6

/**
 * El editor trabaja sobre una rejilla de semicorcheas: en 4/4 son 16
 * columnas por compás. Cada columna equivale a 0.25 pulsos.
 */
export const SLOTS_PER_MEASURE = 16
export const SLOTS_PER_BEAT = 4
export const BEATS_PER_MEASURE = 4

export type { Duration, Finger, Technique }

export const DURATIONS: { value: Duration; label: string; symbol: string; slots: number }[] = [
  { value: 'whole', label: 'Redonda', symbol: '𝅝', slots: 16 },
  { value: 'half', label: 'Blanca', symbol: '𝅗𝅥', slots: 8 },
  { value: 'quarter', label: 'Negra', symbol: '♩', slots: 4 },
  { value: 'eighth', label: 'Corchea', symbol: '♪', slots: 2 },
  { value: 'sixteenth', label: 'Semicorchea', symbol: '𝅘𝅥𝅯', slots: 1 },
]

export function durationSymbol(duration: Duration | string): string {
  return DURATIONS.find((d) => d.value === duration)?.symbol ?? '♩'
}

export const MAX_FRET = 24

export const STANDARD_TUNING = ['E', 'A', 'D', 'G', 'B', 'E']

// ==========================================================================
// Fingerstyle: dedos de la mano derecha (PIMA) y técnicas
// ==========================================================================

export const FINGERS: { value: Finger; label: string; color: string; key: string }[] = [
  { value: 'p', label: 'Pulgar', color: '#f87171', key: 'p' },
  { value: 'i', label: 'Índice', color: '#60a5fa', key: 'i' },
  { value: 'm', label: 'Medio', color: '#4ade80', key: 'm' },
  { value: 'a', label: 'Anular', color: '#fbbf24', key: 'a' },
]

export const TECHNIQUES: {
  value: Technique
  label: string
  symbol: string
  key: string
  hint: string
}[] = [
  { value: 'hammer', label: 'Hammer-on', symbol: 'h', key: 'h', hint: 'Ligado ascendente: golpea el traste sin volver a pulsar' },
  { value: 'pull', label: 'Pull-off', symbol: 'p', key: 'o', hint: 'Ligado descendente: suelta el dedo tirando de la cuerda' },
  { value: 'slide', label: 'Slide', symbol: '/', key: 's', hint: 'Arrastra el dedo por el mástil hasta el traste' },
  { value: 'bend', label: 'Bend', symbol: 'b', key: 'b', hint: 'Estira la cuerda para subir la afinación' },
  { value: 'vibrato', label: 'Vibrato', symbol: '~', key: 'v', hint: 'Oscila el dedo sobre el traste' },
  { value: 'palmMute', label: 'Palm mute', symbol: 'x', key: 'x', hint: 'Apaga la cuerda apoyando la palma en el puente' },
  { value: 'harmonic', label: 'Armónico', symbol: '◇', key: 'n', hint: 'Roza la cuerda sobre el traste (12, 7, 5) sin presionar' },
]

export function fingerColor(finger: Finger): string {
  return FINGERS.find((f) => f.value === finger)?.color ?? '#fff'
}

export function techniqueSymbol(technique: Technique): string {
  return TECHNIQUES.find((t) => t.value === technique)?.symbol ?? ''
}

// ==========================================================================
// Estado del editor
// ==========================================================================

export interface EditorCell {
  fret: number
  finger: Finger | null
  technique: Technique | null
}

export interface EditorMeasure {
  /**
   * cells[cuerda][columna] = nota (traste + digitación) o null.
   * Índice de cuerda 0 = mi agudo (línea superior), 5 = mi grave.
   */
  cells: (EditorCell | null)[][]
  /** Duración del evento que empieza en cada columna (null si no hay evento). */
  durations: (Duration | null)[]
}

export type TabKind = 'tab' | 'arpeggio' | 'fingerstyle'

export interface EditorTabSection {
  id: string
  kind: TabKind
  name: string
  bpm: number | null
  notes: string | null
  measures: EditorMeasure[]
}

export interface EditorChordSection {
  id: string
  kind: 'chords'
  name: string
  bpm: number | null
  notes: string | null
  chords: ChordShape[]
}

export type EditorSection = EditorTabSection | EditorChordSection

export interface EditorDocument {
  title: string
  category: string | null
  tuning: string[]
  timeSignature: string
  baseBpm: number
  sections: EditorSection[]
}

let idCounter = 0
export function newId(): string {
  idCounter += 1
  return `s${Date.now().toString(36)}${idCounter}`
}

export function emptyMeasure(): EditorMeasure {
  return {
    cells: Array.from({ length: STRING_COUNT }, () =>
      Array<EditorCell | null>(SLOTS_PER_MEASURE).fill(null),
    ),
    durations: Array<Duration | null>(SLOTS_PER_MEASURE).fill(null),
  }
}

const TAB_KIND_NAMES: Record<TabKind, string> = {
  tab: 'Sección',
  arpeggio: 'Arpegio',
  fingerstyle: 'Fingerstyle',
}

export function newTabSection(kind: TabKind, name?: string): EditorTabSection {
  return {
    id: newId(),
    kind,
    name: name ?? TAB_KIND_NAMES[kind],
    bpm: null,
    notes: null,
    measures: [emptyMeasure()],
  }
}

export function newChordSection(name?: string): EditorChordSection {
  return { id: newId(), kind: 'chords', name: name ?? 'Acordes', bpm: null, notes: null, chords: [] }
}

export function emptyDocument(): EditorDocument {
  return {
    title: '',
    category: null,
    tuning: [...STANDARD_TUNING],
    timeSignature: '4/4',
    baseBpm: 120,
    sections: [newTabSection('tab', 'Sección 1')],
  }
}

/** Copia profunda de un compás (para duplicar/insertar snippets). */
export function cloneMeasure(measure: EditorMeasure): EditorMeasure {
  return {
    cells: measure.cells.map((row) => row.map((cell) => (cell ? { ...cell } : null))),
    durations: [...measure.durations],
  }
}

// ==========================================================================
// Conversión editor <-> documento
// ==========================================================================

const VALID_FINGERS = new Set<string>(['p', 'i', 'm', 'a'])
const VALID_TECHNIQUES = new Set<string>(TECHNIQUES.map((t) => t.value))

export function measureToDto(measure: EditorMeasure, index: number): MeasureDTO {
  const events = []
  for (let slot = 0; slot < SLOTS_PER_MEASURE; slot++) {
    const notes: NoteDTO[] = []
    for (let s = 0; s < STRING_COUNT; s++) {
      const cell = measure.cells[s][slot]
      if (cell !== null) {
        const note: NoteDTO = { string: s + 1, fret: cell.fret }
        if (cell.finger) note.finger = cell.finger
        if (cell.technique) note.technique = cell.technique
        notes.push(note)
      }
    }
    if (notes.length > 0) {
      events.push({
        beat: 1 + slot / SLOTS_PER_BEAT,
        duration: measure.durations[slot] ?? 'quarter',
        notes,
      })
    }
  }
  return { index, events }
}

export function measureFromDto(dto: MeasureDTO): EditorMeasure {
  const measure = emptyMeasure()
  for (const event of dto.events ?? []) {
    const slot = Math.round((event.beat - 1) * SLOTS_PER_BEAT)
    if (slot < 0 || slot >= SLOTS_PER_MEASURE) continue
    measure.durations[slot] = (event.duration as Duration) ?? 'quarter'
    for (const note of event.notes ?? []) {
      const stringIdx = note.string - 1
      if (stringIdx >= 0 && stringIdx < STRING_COUNT) {
        measure.cells[stringIdx][slot] = {
          fret: note.fret,
          finger: note.finger && VALID_FINGERS.has(note.finger) ? note.finger : null,
          technique:
            note.technique && VALID_TECHNIQUES.has(note.technique)
              ? (note.technique as Technique)
              : null,
        }
      }
    }
  }
  return measure
}

export function toDocument(doc: EditorDocument): TabDocument {
  const sections: SectionDTO[] = doc.sections.map((section) => {
    if (section.kind === 'chords') {
      const dto: ChordSectionDTO = {
        id: section.id,
        kind: 'chords',
        name: section.name,
        chords: section.chords,
      }
      if (section.bpm) dto.bpm = section.bpm
      if (section.notes) dto.notes = section.notes
      return dto
    }
    const dto: TabSectionDTO = {
      id: section.id,
      kind: section.kind,
      name: section.name,
      measures: section.measures.map(measureToDto),
    }
    if (section.bpm) dto.bpm = section.bpm
    if (section.notes) dto.notes = section.notes
    return dto
  })
  return {
    version: 2,
    title: doc.title,
    category: doc.category,
    tuning: doc.tuning,
    timeSignature: doc.timeSignature,
    baseBpm: doc.baseBpm,
    sections,
  }
}

export function fromDocument(dto: TabDocument): EditorDocument {
  return {
    title: dto.title ?? '',
    category: dto.category ?? null,
    tuning: dto.tuning?.length === STRING_COUNT ? dto.tuning : [...STANDARD_TUNING],
    timeSignature: dto.timeSignature ?? '4/4',
    baseBpm: dto.baseBpm ?? 120,
    sections: (dto.sections ?? []).map((section) => {
      if (section.kind === 'chords') {
        return {
          id: section.id ?? newId(),
          kind: 'chords' as const,
          name: section.name ?? 'Acordes',
          bpm: section.bpm ?? null,
          notes: section.notes ?? null,
          chords: (section.chords ?? []).map(normalizeChord),
        }
      }
      const kind: TabKind =
        section.kind === 'arpeggio' || section.kind === 'fingerstyle' ? section.kind : 'tab'
      return {
        id: section.id ?? newId(),
        kind,
        name: section.name ?? 'Sección',
        bpm: section.bpm ?? null,
        notes: section.notes ?? null,
        measures:
          section.measures?.length > 0
            ? section.measures.map(measureFromDto)
            : [emptyMeasure()],
      }
    }),
  }
}

export function normalizeChord(chord: ChordShape): ChordShape {
  const frets = Array.from({ length: STRING_COUNT }, (_, i) => {
    const v = chord.frets?.[i]
    return typeof v === 'number' && v >= 0 ? v : null
  })
  return {
    name: chord.name ?? '',
    frets,
    baseFret: chord.baseFret && chord.baseFret > 1 ? chord.baseFret : 1,
    beats: chord.beats && chord.beats > 0 ? chord.beats : undefined,
  }
}

/** Convierte el formato v1 (metadata + measures planas) al documento v2. */
export function fromV1(v1: GuitarTabV1): TabDocument {
  return {
    version: 2,
    title: v1.metadata?.title ?? '',
    category: null,
    tuning: v1.metadata?.tuning ?? [...STANDARD_TUNING],
    timeSignature: v1.metadata?.timeSignature ?? '4/4',
    baseBpm: v1.metadata?.baseBpm ?? 120,
    sections: [
      { id: newId(), kind: 'tab', name: 'Sección 1', measures: v1.measures ?? [] },
    ],
  }
}

/** Interpreta el contenido de un recurso TAB, sea v1 o v2. */
export function parseTabContent(content: unknown): TabDocument {
  const obj = content as Record<string, unknown>
  if (obj && typeof obj === 'object' && Array.isArray(obj.sections)) {
    return obj as unknown as TabDocument
  }
  if (obj && typeof obj === 'object' && obj.metadata && Array.isArray(obj.measures)) {
    return fromV1(obj as unknown as GuitarTabV1)
  }
  throw new Error('El contenido no tiene formato de tablatura reconocible')
}

// ==========================================================================
// Utilidades de presentación
// ==========================================================================

/** Etiquetas de cuerda de arriba (aguda) a abajo (grave) para pintar el mástil. */
export function stringLabels(tuning: string[]): string[] {
  const labels = [...tuning].reverse()
  // convención de tablatura: la cuerda más aguda se escribe en minúscula
  labels[0] = labels[0].toLowerCase()
  return labels
}

export function sectionKindLabel(kind: EditorSection['kind']): string {
  switch (kind) {
    case 'chords':
      return 'Acordes'
    case 'arpeggio':
      return 'Arpegio'
    case 'fingerstyle':
      return 'Fingerstyle'
    default:
      return 'Tablatura'
  }
}

// ==========================================================================
// Etiquetas traducibles para el render (TabSvg) y el export (ASCII/PDF/PNG).
// Como TabSvg también se renderiza fuera del árbol de React (export con
// renderToStaticMarkup), las etiquetas se pasan como datos, no vía hook.
// ==========================================================================

export interface TabLabels {
  untitled: string
  tuning: string
  rightHand: string
  techniques: string
  kinds: Record<'tab' | 'arpeggio' | 'fingerstyle' | 'chords', string>
  fingerNames: Record<Finger, string>
  techNames: Record<Technique, string>
}

/** Etiquetas por defecto en inglés (idioma por defecto de la app). */
export const DEFAULT_TAB_LABELS: TabLabels = {
  untitled: 'Untitled',
  tuning: 'Tuning',
  rightHand: 'Right hand',
  techniques: 'Techniques',
  kinds: { tab: 'Tab', arpeggio: 'Arpeggio', fingerstyle: 'Fingerstyle', chords: 'Chords' },
  fingerNames: { p: 'thumb', i: 'index', m: 'middle', a: 'ring' },
  techNames: {
    hammer: 'hammer-on',
    pull: 'pull-off',
    slide: 'slide',
    bend: 'bend',
    vibrato: 'vibrato',
    palmMute: 'palm mute',
    harmonic: 'harmonic',
  },
}

/** Construye las etiquetas del render a partir de la función de traducción. */
export function buildTabLabels(t: (key: string) => string): TabLabels {
  return {
    untitled: t('tab.untitled'),
    tuning: t('tab.tuning'),
    rightHand: t('legend.rightHand'),
    techniques: t('legend.techniques'),
    kinds: {
      tab: t('tabEditor.kindTab'),
      arpeggio: t('tabEditor.kindArpeggio'),
      fingerstyle: t('tabEditor.kindFingerstyle'),
      chords: t('tabEditor.kindChords'),
    },
    fingerNames: {
      p: t('fingers.p').toLowerCase(),
      i: t('fingers.i').toLowerCase(),
      m: t('fingers.m').toLowerCase(),
      a: t('fingers.a').toLowerCase(),
    },
    techNames: Object.fromEntries(
      TECHNIQUES.map((tech) => [tech.value, t(`techniques.${tech.value}.label`).toLowerCase()]),
    ) as Record<Technique, string>,
  }
}

function kindLabel(labels: TabLabels, kind: EditorSection['kind']): string {
  return labels.kinds[kind]
}

/** Descripción compacta de un acorde: x32010 (los trastes >9 van entre paréntesis). */
export function chordFretsLabel(chord: ChordShape): string {
  return chord.frets
    .map((f) => (f === null ? 'x' : f > 9 ? `(${f})` : String(f)))
    .join('')
}

/** ¿La sección usa digitación PIMA o técnicas? (para leyendas en exports) */
export function sectionUsesFingerstyle(section: EditorTabSection): boolean {
  return section.measures.some((m) =>
    m.cells.some((row) => row.some((cell) => cell && (cell.finger || cell.technique))),
  )
}

// ==========================================================================
// Export ASCII
// ==========================================================================

function cellText(cell: EditorCell): string {
  const technique = cell.technique ? techniqueSymbol(cell.technique) : ''
  return technique + String(cell.fret)
}

function measuresToAscii(tuning: string[], measures: EditorMeasure[], withFingers: boolean): string {
  const labels = stringLabels(tuning)
  const lines = labels.map((label) => `${label.padEnd(2, ' ')}|`)
  let fingersLine = withFingers ? 'MD ' : ''

  for (const measure of measures) {
    for (let slot = 0; slot < SLOTS_PER_MEASURE; slot++) {
      let width = 1
      for (let s = 0; s < STRING_COUNT; s++) {
        const cell = measure.cells[s][slot]
        if (cell !== null && cellText(cell).length > width) {
          width = cellText(cell).length
        }
      }
      for (let s = 0; s < STRING_COUNT; s++) {
        const cell = measure.cells[s][slot]
        const text = cell === null ? '' : cellText(cell)
        lines[s] += text.padEnd(width, '-') + '-'
      }
      if (withFingers) {
        // dedos de la mano derecha de la columna (de aguda a grave)
        const fingers = [
          ...new Set(
            measure.cells
              .map((row) => row[slot]?.finger)
              .filter((f): f is Finger => !!f),
          ),
        ]
        const text = fingers.length > 1 ? '*' : (fingers[0] ?? '')
        fingersLine += text.padEnd(width, ' ') + ' '
      }
    }
    for (let s = 0; s < STRING_COUNT; s++) {
      lines[s] += '|'
    }
    if (withFingers) fingersLine += ' '
  }
  const result = lines.join('\n')
  return withFingers ? result + '\n' + fingersLine.trimEnd() + '   (* = varios dedos)' : result
}

export function documentToAscii(doc: EditorDocument, labels: TabLabels = DEFAULT_TAB_LABELS): string {
  const parts: string[] = []
  parts.push(`${doc.title || labels.untitled}`)
  parts.push(
    `${labels.tuning}: ${doc.tuning.join(' ')} · ${doc.timeSignature} · ${doc.baseBpm} bpm` +
      (doc.category ? ` · [${doc.category}]` : ''),
  )
  let usesTechniques = false
  for (const section of doc.sections) {
    const bpmNote = section.bpm ? ` @ ${section.bpm} bpm` : ''
    parts.push('')
    parts.push(`== ${section.name} (${kindLabel(labels, section.kind)}${bpmNote}) ==`)
    if (section.notes) {
      parts.push(`   ${section.notes.replace(/\n/g, '\n   ')}`)
    }
    if (section.kind === 'chords') {
      for (const chord of section.chords) {
        const base = chord.baseFret && chord.baseFret > 1 ? ` +${chord.baseFret}` : ''
        parts.push(`  ${chord.name.padEnd(8)} ${chordFretsLabel(chord)}${base}`)
      }
    } else {
      const withFingers = section.kind === 'fingerstyle' || sectionUsesFingerstyle(section)
      if (withFingers) usesTechniques = true
      parts.push(measuresToAscii(doc.tuning, section.measures, withFingers))
    }
  }
  if (usesTechniques) {
    parts.push('')
    parts.push(
      `${labels.rightHand}: ` + FINGERS.map((f) => `${f.value}=${labels.fingerNames[f.value]}`).join(' '),
    )
    parts.push(
      `${labels.techniques}: ` +
        TECHNIQUES.map((tech) => `${tech.symbol}=${labels.techNames[tech.value]}`).join(' '),
    )
  }
  return parts.join('\n')
}
