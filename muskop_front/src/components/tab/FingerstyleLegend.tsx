import { FINGERS, TECHNIQUES } from './tabModel'

/**
 * Leyenda para principiantes: qué dedo de la mano derecha es cada color y
 * qué significa cada símbolo de técnica, con su atajo de teclado.
 */
export default function FingerstyleLegend() {
  return (
    <div className="fingerstyle-legend">
      <div className="legend-group">
        <strong>Mano derecha</strong>
        {FINGERS.map((f) => (
          <span key={f.value} className="legend-item" title={`Tecla: ${f.key}`}>
            <span className="legend-finger" style={{ color: f.color }}>
              {f.value}
            </span>
            {f.label}
          </span>
        ))}
      </div>
      <div className="legend-group">
        <strong>Técnicas</strong>
        {TECHNIQUES.map((t) => (
          <span key={t.value} className="legend-item" title={`${t.hint} · Tecla: ${t.key}`}>
            <span className="legend-symbol">{t.symbol}</span>
            {t.label}
          </span>
        ))}
      </div>
      <p className="muted legend-help">
        Selecciona una nota y pulsa la tecla del dedo (p, i, m, a) o de la
        técnica (h, o, s, b, v, x, n) para marcarla. Repite la tecla para quitarla.
      </p>
    </div>
  )
}
