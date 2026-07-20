import {
  SLOTS_PER_BEAT,
  SLOTS_PER_MEASURE,
  durationSymbol,
  fingerColor,
  stringLabels,
  techniqueSymbol,
  type EditorTabSection,
} from './tabModel'

export interface GridSelection {
  measure: number
  string: number
  slot: number
}

interface GridSectionProps {
  section: EditorTabSection
  tuning: string[]
  selection: GridSelection | null
  playPos: { measureIndex: number; slot: number } | null
  onSelect: (sel: GridSelection) => void
  onKeyDown: (e: React.KeyboardEvent) => void
}

/**
 * Rejilla de edición de una sección de tablatura/arpegio/fingerstyle: regla
 * de pulsos arriba, 6 cuerdas con digitación PIMA y técnicas por nota, y
 * fila de duraciones debajo de cada compás.
 */
export default function GridSection({
  section,
  tuning,
  selection,
  playPos,
  onSelect,
  onKeyDown,
}: GridSectionProps) {
  const labels = stringLabels(tuning)

  return (
    <div className="tab-editor-staff" tabIndex={0} onKeyDown={onKeyDown}>
      <div className="string-labels">
        <span className="ruler-spacer" />
        {labels.map((label, i) => (
          <span key={i}>{label}</span>
        ))}
        <span className="duration-spacer" />
      </div>
      <div className="measures">
        {section.measures.map((measure, mi) => {
          const playSlot = playPos?.measureIndex === mi ? playPos.slot : null
          return (
            <div className="measure" key={mi}>
              <div className="ruler-row">
                {Array.from({ length: SLOTS_PER_MEASURE }, (_, slot) => (
                  <span
                    key={slot}
                    className={'ruler-cell' + (slot % SLOTS_PER_BEAT === 0 ? ' beat' : '')}
                  >
                    {slot % SLOTS_PER_BEAT === 0 ? slot / SLOTS_PER_BEAT + 1 : '·'}
                  </span>
                ))}
              </div>
              {measure.cells.map((row, si) => (
                <div className="string-row" key={si}>
                  {row.map((cell, slot) => {
                    const isSelected =
                      selection !== null &&
                      selection.measure === mi &&
                      selection.string === si &&
                      selection.slot === slot
                    return (
                      <button
                        type="button"
                        key={slot}
                        className={
                          'cell' +
                          (isSelected ? ' selected' : '') +
                          (slot % SLOTS_PER_BEAT === 0 ? ' beat-start' : '') +
                          (playSlot === slot ? ' playing' : '')
                        }
                        onClick={() => onSelect({ measure: mi, string: si, slot })}
                      >
                        {cell !== null && (
                          <>
                            <span className="fret">
                              {cell.technique && (
                                <span className="tech-mark">
                                  {techniqueSymbol(cell.technique)}
                                </span>
                              )}
                              {cell.fret}
                            </span>
                            {cell.finger && (
                              <span
                                className="finger-mark"
                                style={{ color: fingerColor(cell.finger) }}
                              >
                                {cell.finger}
                              </span>
                            )}
                          </>
                        )}
                      </button>
                    )
                  })}
                </div>
              ))}
              <div className="duration-row">
                {measure.durations.map((duration, slot) => (
                  <span key={slot} className="duration-cell">
                    {duration ? durationSymbol(duration) : ''}
                  </span>
                ))}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
