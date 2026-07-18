import type { ReactNode } from 'react'

// ==========================================================================
// Visor de teoría: renderiza el markdown sencillo de los artículos
// (#/## títulos, - listas, **negrita**) sin dependencias externas.
// ==========================================================================

function renderInline(text: string): ReactNode[] {
  const parts = text.split(/\*\*(.+?)\*\*/g)
  return parts.map((part, i) => (i % 2 === 1 ? <strong key={i}>{part}</strong> : part))
}

export default function TheoryView({ body }: { body: string }) {
  const lines = body.split('\n')
  const blocks: ReactNode[] = []
  let paragraph: string[] = []
  let list: string[] = []
  let key = 0

  const flushParagraph = () => {
    if (paragraph.length > 0) {
      blocks.push(<p key={key++}>{renderInline(paragraph.join(' '))}</p>)
      paragraph = []
    }
  }
  const flushList = () => {
    if (list.length > 0) {
      blocks.push(
        <ul key={key++}>
          {list.map((item, i) => (
            <li key={i}>{renderInline(item)}</li>
          ))}
        </ul>,
      )
      list = []
    }
  }

  for (const raw of lines) {
    const line = raw.trimEnd()
    if (line.startsWith('## ')) {
      flushParagraph()
      flushList()
      blocks.push(<h4 key={key++}>{renderInline(line.slice(3))}</h4>)
    } else if (line.startsWith('# ')) {
      flushParagraph()
      flushList()
      blocks.push(<h3 key={key++}>{renderInline(line.slice(2))}</h3>)
    } else if (line.startsWith('- ')) {
      flushParagraph()
      list.push(line.slice(2))
    } else if (line.trim() === '') {
      flushParagraph()
      flushList()
    } else {
      flushList()
      paragraph.push(line)
    }
  }
  flushParagraph()
  flushList()

  return <div className="theory-view">{blocks}</div>
}
