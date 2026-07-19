import { FINGERS, TECHNIQUES } from './tabModel'
import { useI18n } from '../../i18n/I18nContext'

/**
 * Leyenda para principiantes: qué dedo de la mano derecha es cada color y
 * qué significa cada símbolo de técnica, con su atajo de teclado.
 */
export default function FingerstyleLegend() {
  const { t } = useI18n()
  return (
    <div className="fingerstyle-legend">
      <div className="legend-group">
        <strong>{t('legend.rightHand')}</strong>
        {FINGERS.map((f) => (
          <span key={f.value} className="legend-item" title={t('legend.keyTitle', { key: f.key })}>
            <span className="legend-finger" style={{ color: f.color }}>
              {f.value}
            </span>
            {t(`fingers.${f.value}`)}
          </span>
        ))}
      </div>
      <div className="legend-group">
        <strong>{t('legend.techniques')}</strong>
        {TECHNIQUES.map((tech) => (
          <span
            key={tech.value}
            className="legend-item"
            title={t('legend.hintKey', { hint: t(`techniques.${tech.value}.hint`), key: tech.key })}
          >
            <span className="legend-symbol">{tech.symbol}</span>
            {t(`techniques.${tech.value}.label`)}
          </span>
        ))}
      </div>
      <p className="muted legend-help">{t('legend.help')}</p>
    </div>
  )
}
