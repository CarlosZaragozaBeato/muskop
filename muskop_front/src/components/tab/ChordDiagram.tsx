import type { ChordShape } from '../../types/tab'
import { useI18n } from '../../i18n/I18nContext'

const FRETS_SHOWN = 4

interface ChordDiagramProps {
  chord: ChordShape
  /** Ancho en px del diagrama (alto proporcional) */
  width?: number
  /**
   * Si se pasa, el diagrama es editable: clic en una casilla pone/quita el
   * dedo; clic sobre la cejuela alterna cuerda al aire (o) / muteada (x).
   */
  onChange?: (chord: ChordShape) => void
}

/**
 * Diagrama de acorde clásico: cuerdas verticales (grave a la izquierda),
 * trastes horizontales, puntos para los dedos y x/o sobre la cejuela.
 */
export default function ChordDiagram({ chord, width = 96, onChange }: ChordDiagramProps) {
  const { t } = useI18n()
  const stringGap = width / 7
  const fretGap = stringGap * 1.2
  const left = stringGap
  const top = stringGap * 1.4
  const gridW = stringGap * 5
  const gridH = fretGap * FRETS_SHOWN
  const height = top + gridH + stringGap * 0.6
  const baseFret = chord.baseFret ?? 1

  const toggleOpenMute = (s: number) => {
    if (!onChange) return
    const frets = [...chord.frets]
    frets[s] = frets[s] === 0 ? null : 0
    onChange({ ...chord, frets })
  }

  const setFinger = (s: number, fretOffset: number) => {
    if (!onChange) return
    const absolute = baseFret + fretOffset
    const frets = [...chord.frets]
    frets[s] = frets[s] === absolute ? null : absolute
    onChange({ ...chord, frets })
  }

  return (
    <svg
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      className={'chord-diagram' + (onChange ? ' editable' : '')}
      role="img"
      aria-label={t('chordEditor.diagramAria', { name: chord.name || t('chordEditor.unnamed') })}
    >
      {/* cejuela o número de traste base */}
      {baseFret === 1 ? (
        <rect x={left - 1} y={top - 3} width={gridW + 2} height={3} fill="currentColor" />
      ) : (
        <text
          x={left - stringGap * 0.4}
          y={top + fretGap * 0.65}
          fontSize={stringGap * 0.8}
          textAnchor="end"
          fill="currentColor"
        >
          {baseFret}
        </text>
      )}

      {/* trastes */}
      {Array.from({ length: FRETS_SHOWN + 1 }, (_, f) => (
        <line
          key={`f${f}`}
          x1={left}
          y1={top + f * fretGap}
          x2={left + gridW}
          y2={top + f * fretGap}
          stroke="currentColor"
          strokeWidth={0.8}
          opacity={0.7}
        />
      ))}

      {/* cuerdas */}
      {Array.from({ length: 6 }, (_, s) => (
        <line
          key={`s${s}`}
          x1={left + s * stringGap}
          y1={top}
          x2={left + s * stringGap}
          y2={top + gridH}
          stroke="currentColor"
          strokeWidth={0.8}
          opacity={0.7}
        />
      ))}

      {/* marcadores x / o y puntos */}
      {chord.frets.map((fret, s) => {
        const x = left + s * stringGap
        if (fret === null || fret === 0) {
          return (
            <text
              key={`m${s}`}
              x={x}
              y={top - 6}
              fontSize={stringGap * 0.75}
              textAnchor="middle"
              fill="currentColor"
              opacity={fret === null ? 0.7 : 1}
            >
              {fret === null ? '✕' : '○'}
            </text>
          )
        }
        const offset = fret - baseFret
        if (offset < 0 || offset >= FRETS_SHOWN) return null
        return (
          <circle
            key={`d${s}`}
            cx={x}
            cy={top + offset * fretGap + fretGap / 2}
            r={stringGap * 0.38}
            fill="currentColor"
          />
        )
      })}

      {/* zonas clicables para edición */}
      {onChange && (
        <>
          {Array.from({ length: 6 }, (_, s) => (
            <rect
              key={`t${s}`}
              x={left + s * stringGap - stringGap / 2}
              y={0}
              width={stringGap}
              height={top - 2}
              fill="transparent"
              style={{ cursor: 'pointer' }}
              onClick={() => toggleOpenMute(s)}
            />
          ))}
          {Array.from({ length: 6 }, (_, s) =>
            Array.from({ length: FRETS_SHOWN }, (_, f) => (
              <rect
                key={`c${s}-${f}`}
                x={left + s * stringGap - stringGap / 2}
                y={top + f * fretGap}
                width={stringGap}
                height={fretGap}
                fill="transparent"
                style={{ cursor: 'pointer' }}
                onClick={() => setFinger(s, f)}
              />
            )),
          )}
        </>
      )}
    </svg>
  )
}
