import { renderToStaticMarkup } from 'react-dom/server'
import TabSvg from '../components/tab/TabSvg'
import { documentToAscii, type EditorDocument } from '../components/tab/tabModel'

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

export function documentSvgMarkup(doc: EditorDocument): string {
  return renderToStaticMarkup(<TabSvg doc={doc} />)
}

export function exportText(docs: EditorDocument[], name?: string) {
  const text = docs.map(documentToAscii).join('\n\n' + '='.repeat(60) + '\n\n')
  downloadBlob(
    new Blob([text], { type: 'text/plain;charset=utf-8' }),
    `${safeFilename(name ?? docs[0]?.title ?? 'tablatura')}.txt`,
  )
}

export async function exportPng(doc: EditorDocument, scale = 2): Promise<void> {
  const markup = documentSvgMarkup(doc)
  const svgBlob = new Blob([markup], { type: 'image/svg+xml;charset=utf-8' })
  const url = URL.createObjectURL(svgBlob)
  try {
    const img = new Image()
    await new Promise<void>((resolve, reject) => {
      img.onload = () => resolve()
      img.onerror = () => reject(new Error('No se pudo renderizar el SVG'))
      img.src = url
    })
    const canvas = document.createElement('canvas')
    canvas.width = img.width * scale
    canvas.height = img.height * scale
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('Canvas no disponible')
    ctx.scale(scale, scale)
    ctx.drawImage(img, 0, 0)
    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, 'image/png'),
    )
    if (!blob) throw new Error('No se pudo generar el PNG')
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
export function exportPdf(docs: EditorDocument[], title?: string) {
  const pages = docs
    .map((doc) => `<div class="page">${documentSvgMarkup(doc)}</div>`)
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
    throw new Error('El navegador bloqueó la ventana de impresión')
  }
  win.document.write(html)
  win.document.close()
}
