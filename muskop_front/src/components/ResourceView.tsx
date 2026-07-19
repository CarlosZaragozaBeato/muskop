import { useEffect, useMemo, useRef, useState } from 'react'
import { TabPlayer } from '../audio/player'
import type {
  ChordContent,
  ResourceDetail,
  SnippetContent,
  TabDocument,
  TheoryContent,
} from '../types/tab'
import ChordDiagram from './tab/ChordDiagram'
import TabSvg from './tab/TabSvg'
import TheoryView from './TheoryView'
import { useI18n } from '../i18n/I18nContext'
import {
  buildTabLabels,
  fromDocument,
  parseTabContent,
  STANDARD_TUNING,
  type EditorDocument,
} from './tab/tabModel'

/**
 * Visor de solo lectura de un recurso de la librería (para el modo práctica):
 * tablaturas y fragmentos se renderizan con el SVG, los acordes con su
 * diagrama. Las tablaturas se pueden escuchar.
 */
export default function ResourceView({ detail }: { detail: ResourceDetail }) {
  const { t } = useI18n()
  const [playing, setPlaying] = useState(false)
  const player = useRef<TabPlayer | null>(null)

  useEffect(() => {
    player.current = new TabPlayer()
    return () => player.current?.stop()
  }, [])

  const doc: EditorDocument | null = useMemo(() => {
    try {
      const type = detail.type.toUpperCase()
      if (type === 'TAB') {
        return fromDocument(parseTabContent(detail.content))
      }
      if (type === 'SNIPPET') {
        const content = detail.content as SnippetContent
        const asDoc: TabDocument = {
          version: 2,
          title: detail.title,
          category: null,
          tuning: content.tuning ?? [...STANDARD_TUNING],
          timeSignature: '4/4',
          baseBpm: 100,
          sections: [{ kind: 'tab', name: detail.title, measures: content.measures ?? [] }],
        }
        return fromDocument(asDoc)
      }
      return null
    } catch {
      return null
    }
  }, [detail])

  // parar la reproducción al cambiar de recurso
  useEffect(() => {
    player.current?.stop()
    setPlaying(false)
  }, [detail.id])

  const togglePlay = () => {
    if (!doc) return
    if (playing) {
      player.current?.stop()
      setPlaying(false)
      return
    }
    setPlaying(true)
    player.current?.play(doc, { metronome: false, onEnd: () => setPlaying(false) })
  }

  if (detail.type.toUpperCase() === 'THEORY') {
    return <TheoryView body={(detail.content as TheoryContent).body ?? ''} />
  }

  if (detail.type.toUpperCase() === 'CHORD') {
    const chord = (detail.content as ChordContent).chord
    return (
      <div className="resource-view resource-view-chord">
        <ChordDiagram chord={chord} width={170} />
        <strong>{chord.name || detail.title}</strong>
      </div>
    )
  }

  if (!doc) {
    return <p className="muted">{t('resourceView.cannotPreview')}</p>
  }

  return (
    <div className="resource-view">
      <button type="button" className={playing ? 'active' : ''} onClick={togglePlay}>
        {playing ? t('resourceView.stop') : t('resourceView.listen')}
      </button>
      <div className="resource-view-svg">
        <TabSvg doc={doc} ink="#e5e7eb" background="#16171d" labels={buildTabLabels(t)} />
      </div>
    </div>
  )
}
