import type { ReactNode } from 'react'
import type { ChordShape } from '../../types/tab'
import {
  DEFAULT_TAB_LABELS,
  FINGERS,
  SLOTS_PER_BEAT,
  SLOTS_PER_MEASURE,
  STRING_COUNT,
  TECHNIQUES,
  durationSymbol,
  fingerColor,
  sectionUsesFingerstyle,
  stringLabels,
  techniqueSymbol,
  type EditorDocument,
  type EditorMeasure,
  type TabLabels,
} from './tabModel'

// ==========================================================================
// Render SVG del documento completo: se usa para la vista previa y para las
// exportaciones a PNG y PDF (impresión).
// ==========================================================================

const PAGE_W = 1000
const MARGIN = 28
const SLOT_W = 14
const STRING_GAP = 15
const MEASURES_PER_ROW = 4
const MEASURE_W = SLOTS_PER_MEASURE * SLOT_W
const STAFF_H = (STRING_COUNT - 1) * STRING_GAP
const RULER_H = 16
const DURATION_H = 20
const ROW_H = RULER_H + STAFF_H + DURATION_H + 26
const LABELS_W = 26
const CHORD_W = 104
const CHORD_H = 150
const NOTES_LINE_H = 15

interface TabSvgProps {
  doc: EditorDocument
  ink?: string
  background?: string
  /** Etiquetas traducibles del render (por defecto, inglés) */
  labels?: TabLabels
}

function notesLines(notes: string | null): string[] {
  return notes ? notes.split('\n').filter((l) => l.trim() !== '') : []
}

function docUsesFingerstyle(doc: EditorDocument): boolean {
  return doc.sections.some((s) => s.kind !== 'chords' && sectionUsesFingerstyle(s))
}

export function svgHeight(doc: EditorDocument): number {
  let y = 78
  for (const section of doc.sections) {
    y += 30
    y += notesLines(section.notes).length * NOTES_LINE_H
    if (section.kind === 'chords') {
      const perRow = Math.floor((PAGE_W - MARGIN * 2) / CHORD_W)
      const rows = Math.max(1, Math.ceil(section.chords.length / perRow))
      y += rows * CHORD_H
    } else {
      const rows = Math.max(1, Math.ceil(section.measures.length / MEASURES_PER_ROW))
      y += rows * ROW_H
    }
    y += 14
  }
  if (docUsesFingerstyle(doc)) {
    y += 40
  }
  return y + MARGIN
}

export default function TabSvg({
  doc,
  ink = '#111',
  background = '#fff',
  labels = DEFAULT_TAB_LABELS,
}: TabSvgProps) {
  const height = svgHeight(doc)
  const children: ReactNode[] = []
  let y = MARGIN

  // Cabecera
  children.push(
    <text key="title" x={MARGIN} y={y + 18} fontSize={22} fontWeight={700} fill={ink}>
      {doc.title || labels.untitled}
    </text>,
  )
  const meta = [
    `${labels.tuning}: ${doc.tuning.join(' ')}`,
    doc.timeSignature,
    `${doc.baseBpm} bpm`,
    ...(doc.category ? [doc.category] : []),
  ].join('  ·  ')
  children.push(
    <text key="meta" x={MARGIN} y={y + 40} fontSize={12} fill={ink} opacity={0.65}>
      {meta}
    </text>,
  )
  y += 50

  doc.sections.forEach((section, si) => {
    const bpmNote = section.bpm ? ` · ${section.bpm} bpm` : ''
    children.push(
      <text key={`h${si}`} x={MARGIN} y={y + 18} fontSize={14} fontWeight={600} fill={ink}>
        {section.name}
        <tspan opacity={0.55} fontWeight={400}>
          {'  '}({labels.kinds[section.kind]}{bpmNote})
        </tspan>
      </text>,
    )
    y += 30

    // notas de estudio de la sección
    notesLines(section.notes).forEach((line, li) => {
      children.push(
        <text
          key={`n${si}-${li}`}
          x={MARGIN}
          y={y + 4}
          fontSize={11}
          fontStyle="italic"
          fill={ink}
          opacity={0.7}
        >
          {line}
        </text>,
      )
      y += NOTES_LINE_H
    })

    if (section.kind === 'chords') {
      const perRow = Math.floor((PAGE_W - MARGIN * 2) / CHORD_W)
      section.chords.forEach((chord, ci) => {
        const cx = MARGIN + (ci % perRow) * CHORD_W
        const cy = y + Math.floor(ci / perRow) * CHORD_H
        children.push(
          <g key={`c${si}-${ci}`}>
            {renderChord(chord, cx, cy, ink)}
          </g>,
        )
      })
      const rows = Math.max(1, Math.ceil(section.chords.length / perRow))
      y += rows * CHORD_H + 14
    } else {
      section.measures.forEach((measure, mi) => {
        const rowIdx = Math.floor(mi / MEASURES_PER_ROW)
        const colIdx = mi % MEASURES_PER_ROW
        const mx = MARGIN + LABELS_W + colIdx * MEASURE_W
        const my = y + rowIdx * ROW_H
        children.push(
          <g key={`m${si}-${mi}`}>
            {renderMeasure(measure, doc, mx, my, ink, background, colIdx === 0)}
          </g>,
        )
      })
      const rows = Math.max(1, Math.ceil(section.measures.length / MEASURES_PER_ROW))
      y += rows * ROW_H + 14
    }
  })

  // leyenda fingerstyle para el que lee la pieza
  if (docUsesFingerstyle(doc)) {
    const fingersText = FINGERS.map((f) => `${f.value}=${labels.fingerNames[f.value]}`).join('  ')
    const techniquesText = TECHNIQUES.map((tech) => `${tech.symbol}=${labels.techNames[tech.value]}`).join('  ')
    children.push(
      <g key="legend">
        <text x={MARGIN} y={y + 12} fontSize={11} fontWeight={600} fill={ink} opacity={0.8}>
          {labels.rightHand}:{' '}
          {FINGERS.map((f) => (
            <tspan key={f.value} fill={f.color} fontWeight={700}>
              {` ${f.value}`}
              <tspan fill={ink} fontWeight={400} opacity={0.75}>
                ={labels.fingerNames[f.value]}
              </tspan>
            </tspan>
          ))}
        </text>
        <text x={MARGIN} y={y + 28} fontSize={11} fill={ink} opacity={0.75}>
          {labels.techniques}: {techniquesText}
        </text>
        <desc>{fingersText}</desc>
      </g>,
    )
    y += 40
  }

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={PAGE_W}
      height={height}
      viewBox={`0 0 ${PAGE_W} ${height}`}
      fontFamily="system-ui, sans-serif"
    >
      <rect width={PAGE_W} height={height} fill={background} />
      {children}
    </svg>
  )
}

function renderMeasure(
  measure: EditorMeasure,
  doc: EditorDocument,
  x: number,
  y: number,
  ink: string,
  background: string,
  firstOfRow: boolean,
): ReactNode {
  const parts: ReactNode[] = []
  const staffTop = y + RULER_H

  // etiquetas de afinación al inicio de cada fila de compases
  if (firstOfRow) {
    stringLabels(doc.tuning).forEach((label, s) => {
      parts.push(
        <text
          key={`l${s}`}
          x={x - 10}
          y={staffTop + s * STRING_GAP + 4}
          fontSize={10}
          textAnchor="end"
          fontFamily="monospace"
          fill={ink}
          opacity={0.6}
        >
          {label}
        </text>,
      )
    })
  }

  // números de pulso
  for (let b = 0; b < SLOTS_PER_MEASURE / SLOTS_PER_BEAT; b++) {
    parts.push(
      <text
        key={`b${b}`}
        x={x + b * SLOTS_PER_BEAT * SLOT_W + SLOT_W / 2}
        y={y + 10}
        fontSize={9}
        textAnchor="middle"
        fill={ink}
        opacity={0.45}
      >
        {b + 1}
      </text>,
    )
  }

  // líneas de cuerda
  for (let s = 0; s < STRING_COUNT; s++) {
    parts.push(
      <line
        key={`s${s}`}
        x1={x}
        y1={staffTop + s * STRING_GAP}
        x2={x + MEASURE_W}
        y2={staffTop + s * STRING_GAP}
        stroke={ink}
        strokeWidth={0.7}
        opacity={0.55}
      />,
    )
  }

  // barras de compás
  for (const bx of [x, x + MEASURE_W]) {
    parts.push(
      <line
        key={`bar${bx}`}
        x1={bx}
        y1={staffTop}
        x2={bx}
        y2={staffTop + STAFF_H}
        stroke={ink}
        strokeWidth={1.4}
      />,
    )
  }

  // notas (traste + técnica + dedo) y símbolos de duración
  for (let slot = 0; slot < SLOTS_PER_MEASURE; slot++) {
    const cx = x + slot * SLOT_W + SLOT_W / 2
    for (let s = 0; s < STRING_COUNT; s++) {
      const cell = measure.cells[s][slot]
      if (cell === null) continue
      const text = (cell.technique ? techniqueSymbol(cell.technique) : '') + cell.fret
      parts.push(
        <text
          key={`f${s}-${slot}`}
          x={cx}
          y={staffTop + s * STRING_GAP + 4}
          fontSize={11}
          fontWeight={600}
          textAnchor="middle"
          fontFamily="monospace"
          fill={ink}
          stroke={background}
          strokeWidth={4}
          paintOrder="stroke"
        >
          {text}
        </text>,
      )
      if (cell.finger) {
        parts.push(
          <text
            key={`fg${s}-${slot}`}
            x={cx + SLOT_W / 2 - 1}
            y={staffTop + s * STRING_GAP + 8}
            fontSize={7.5}
            fontWeight={700}
            textAnchor="middle"
            fontFamily="monospace"
            fill={fingerColor(cell.finger)}
            stroke={background}
            strokeWidth={2.5}
            paintOrder="stroke"
          >
            {cell.finger}
          </text>,
        )
      }
    }
    const duration = measure.durations[slot]
    if (duration) {
      parts.push(
        <text
          key={`d${slot}`}
          x={cx}
          y={staffTop + STAFF_H + 16}
          fontSize={12}
          textAnchor="middle"
          fill={ink}
          opacity={0.7}
        >
          {durationSymbol(duration)}
        </text>,
      )
    }
  }

  return parts
}

function renderChord(chord: ChordShape, x: number, y: number, ink: string): ReactNode {
  const parts: ReactNode[] = []
  const stringGap = 13
  const fretGap = 16
  const left = x + 14
  const top = y + 26
  const gridW = stringGap * 5
  const fretsShown = 4
  const baseFret = chord.baseFret ?? 1

  parts.push(
    <text
      key="name"
      x={left + gridW / 2}
      y={y + 10}
      fontSize={13}
      fontWeight={600}
      textAnchor="middle"
      fill={ink}
    >
      {chord.name || '—'}
    </text>,
  )

  if (baseFret === 1) {
    parts.push(
      <rect key="nut" x={left - 1} y={top - 3} width={gridW + 2} height={3} fill={ink} />,
    )
  } else {
    parts.push(
      <text key="base" x={left - 6} y={top + fretGap * 0.68} fontSize={10} textAnchor="end" fill={ink}>
        {baseFret}
      </text>,
    )
  }

  for (let f = 0; f <= fretsShown; f++) {
    parts.push(
      <line
        key={`f${f}`}
        x1={left}
        y1={top + f * fretGap}
        x2={left + gridW}
        y2={top + f * fretGap}
        stroke={ink}
        strokeWidth={0.7}
        opacity={0.6}
      />,
    )
  }
  for (let s = 0; s < 6; s++) {
    parts.push(
      <line
        key={`s${s}`}
        x1={left + s * stringGap}
        y1={top}
        x2={left + s * stringGap}
        y2={top + fretsShown * fretGap}
        stroke={ink}
        strokeWidth={0.7}
        opacity={0.6}
      />,
    )
  }

  chord.frets.forEach((fret, s) => {
    const sx = left + s * stringGap
    if (fret === null || fret === 0) {
      parts.push(
        <text
          key={`m${s}`}
          x={sx}
          y={top - 7}
          fontSize={9}
          textAnchor="middle"
          fill={ink}
          opacity={fret === null ? 0.65 : 1}
        >
          {fret === null ? '✕' : '○'}
        </text>,
      )
      return
    }
    const offset = fret - baseFret
    if (offset < 0 || offset >= fretsShown) return
    parts.push(
      <circle
        key={`d${s}`}
        cx={sx}
        cy={top + offset * fretGap + fretGap / 2}
        r={4.6}
        fill={ink}
      />,
    )
  })

  return parts
}
