import { renderToStaticMarkup } from 'react-dom/server'
import TabSvg from '../components/tab/TabSvg'
import {
  DEFAULT_TAB_LABELS,
  documentToAscii,
  type EditorDocument,
  type TabLabels,
} from '../components/tab/tabModel'
import { translate as tr } from '../i18n/translate'
import { isNativePlatform, saveFile, saveText } from '../native/share'

// ==========================================================================
// Exportación: texto (.txt), imagen (PNG) y PDF. En web el PDF se genera con
// el diálogo de impresión (vectorial, sin dependencias); en Android se
// comparte el HTML de las páginas (imprimible a PDF desde un visor).
// ==========================================================================

function safeFilename(name: string): string {
  return (name || 'tablatura').replace(/[^\p{L}\p{N}\-_ ]/gu, '').trim().replace(/\s+/g, '-')
}

export function documentSvgMarkup(doc: EditorDocument, labels: TabLabels = DEFAULT_TAB_LABELS): string {
  return renderToStaticMarkup(<TabSvg doc={doc} labels={labels} />)
}

export function exportText(docs: EditorDocument[], name?: string, labels: TabLabels = DEFAULT_TAB_LABELS) {
  const text = docs.map((doc) => documentToAscii(doc, labels)).join('\n\n' + '='.repeat(60) + '\n\n')
  return saveText(
    `${safeFilename(name ?? docs[0]?.title ?? 'tab')}.txt`,
    text,
    'text/plain;charset=utf-8',
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
    await saveFile(`${safeFilename(doc.title)}.png`, blob, { text: false, title: doc.title })
  } finally {
    URL.revokeObjectURL(url)
  }
}

/**
 * Genera un documento con las páginas renderizadas (una por sección). En web
 * abre el diálogo de impresión ("Guardar como PDF" vectorial); en Android
 * comparte el .html, imprimible a PDF desde el navegador/visor del sistema.
 */
export async function exportPdf(docs: EditorDocument[], title?: string, labels: TabLabels = DEFAULT_TAB_LABELS) {
  const pages = docs
    .map((doc) => `<div class="page">${documentSvgMarkup(doc, labels)}</div>`)
    .join('\n')
  const name = title ?? docs[0]?.title ?? 'Tablaturas'
  const printScript = isNativePlatform()
    ? ''
    : `<script>window.addEventListener('load', function(){ window.print(); });</script>`
  const html = `<!doctype html>
<html>
<head>
<meta charset="utf-8">
<title>${name}</title>
<style>
  body { margin: 0; }
  .page { page-break-after: always; display: flex; justify-content: center; }
  .page svg { max-width: 100%; height: auto; }
  @media print { .page { page-break-inside: avoid; } }
</style>
</head>
<body>${pages}
${printScript}
</body>
</html>`
  if (isNativePlatform()) {
    await saveText(`${safeFilename(name)}.html`, html, 'text/html;charset=utf-8', name)
    return
  }
  const win = window.open('', '_blank')
  if (!win) {
    throw new Error(tr('errors.printBlocked'))
  }
  win.document.write(html)
  win.document.close()
}
