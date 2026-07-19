import { renderToStaticMarkup } from 'react-dom/server'
import TabSvg from '../components/tab/TabSvg'
import {
  DEFAULT_TAB_LABELS,
  documentToAscii,
  type EditorDocument,
  type TabLabels,
} from '../components/tab/tabModel'
import { translate as tr } from '../i18n/translate'

// ==========================================================================
// Exportación: texto (.txt), imagen (PNG) y PDF (vía diálogo de impresión,
// que produce PDF vectorial sin dependencias externas).
// ==========================================================================

function safeFilename(name: string): string {
  return (name || 'tablatura').replace(/[^\p{L}\p{N}\-_ ]/gu, '').trim().replace(/\s+/g, '-')
}

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export function documentSvgMarkup(doc: EditorDocument, labels: TabLabels = DEFAULT_TAB_LABELS): string {
  return renderToStaticMarkup(<TabSvg doc={doc} labels={labels} />)
}

export function exportText(docs: EditorDocument[], name?: string, labels: TabLabels = DEFAULT_TAB_LABELS) {
  const text = docs.map((doc) => documentToAscii(doc, labels)).join('\n\n' + '='.repeat(60) + '\n\n')
  downloadBlob(
    new Blob([text], { type: 'text/plain;charset=utf-8' }),
    `${safeFilename(name ?? docs[0]?.title ?? 'tab')}.txt`,
  )
}

export async function exportPng(doc: EditorDocument, labels: TabLabels = DEFAULT_TAB_LABELS, scale = 2): Promise<void> {
  const markup = documentSvgMarkup(doc, labels)
  const svgBlob = new Blob([markup], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(svgBlob)
  try {
    const img = new Image()
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve()
      img.onerror = () => reject(new Error(tr('errors.svgRender')))
      img.src = url
    })
    const canvas = document.createElement('canvas')
    canvas.width = img.width * scale
    canvas.height = img.height * scale
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error(tr('errors.canvasUnavailable'))
    ctx.scale(scale, scale)
    ctx.drawImage(img, 0, 0)
    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, 'image/png'),
    )
    if (!blob) throw new Error(tr('errors.pngFailed'))
    downloadBlob(blob, `${safeFilename(doc.title)}.png`)
  } finally {
    URL.revokeObjectURL(url)
  }
}

/**
 * Abre una ventana de impresión con los documentos renderizados (uno por
 * página). Desde ahí el navegador permite "Guardar como PDF" con calidad
 * vectorial.
 */
export function exportPdf(docs: EditorDocument[], title?: string, labels: TabLabels = DEFAULT_TAB_LABELS) {
  const pages = docs
    .map((doc) => `<div class="page">${documentSvgMarkup(doc, labels)}</div>`)
    .join('\n')
  const html = `<!doctype html>
<html>
<head>
<meta charset="utf-8">
<title>${title ?? docs[0]?.title ?? 'Tablaturas'}</title>
<style>
  body { margin: 0; }
  .page { page-break-after: always; display: flex; justify-content: center; }
  .page svg { max-width: 100%; height: auto; }
  @media print { .page { page-break-inside: avoid; } }
</style>
</head>
<body>${pages}
<script>window.addEventListener('load', function(){ window.print(); });</script>
</body>
</html>`
  const win = window.open('', '_blank')
  if (!win) {
    throw new Error(tr('errors.printBlocked'))
  }
  win.document.write(html)
  win.document.close()
}
