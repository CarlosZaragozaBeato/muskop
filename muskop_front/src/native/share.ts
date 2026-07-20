import { Capacitor } from '@capacitor/core'
import { Directory, Encoding, Filesystem } from '@capacitor/filesystem'
import { Share } from '@capacitor/share'

// ==========================================================================
// Guardado/compartición de archivos, agnóstico de plataforma.
//   - Web: descarga clásica (<a download>).
//   - Android (Capacitor): escribe en el almacenamiento de la app y abre la
//     hoja de compartir del sistema (guardar en Archivos, enviar, etc.).
// Se usa para exportar sesiones (.muskop.json), rutinas, packs y tablaturas
// (.txt/.png/.html), reutilizando la misma lógica que la versión web.
// ==========================================================================

export function isNativePlatform(): boolean {
  return Capacitor.isNativePlatform()
}

function webDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

function blobToBase64(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onerror = () => reject(reader.error ?? new Error('read error'))
    reader.onloadend = () => resolve(String(reader.result).split(',')[1] ?? '')
    reader.readAsDataURL(blob)
  })
}

interface SaveOptions {
  /** true si el contenido es texto UTF-8 (json/txt/html); false para binario (png) */
  text?: boolean
  /** Título para la hoja de compartir en nativo */
  title?: string
}

/**
 * Guarda un archivo: en web lo descarga; en Android lo escribe en el
 * almacenamiento de la app y abre la hoja de compartir.
 */
export async function saveFile(
  filename: string,
  blob: Blob,
  { text = true, title }: SaveOptions = {},
): Promise<void> {
  if (!isNativePlatform()) {
    webDownload(blob, filename)
    return
  }
  await Filesystem.writeFile(
    text
      ? { path: filename, data: await blob.text(), directory: Directory.Cache, encoding: Encoding.UTF8 }
      : { path: filename, data: await blobToBase64(blob), directory: Directory.Cache },
  )
  const { uri } = await Filesystem.getUri({ directory: Directory.Cache, path: filename })
  await Share.share({ title: title ?? filename, url: uri, dialogTitle: title ?? filename })
}

/** Atajo para guardar texto (json/txt/html). */
export function saveText(
  filename: string,
  content: string,
  mime = 'application/json;charset=utf-8',
  title?: string,
): Promise<void> {
  return saveFile(filename, new Blob([content], { type: mime }), { text: true, title })
}

/**
 * Resultado de compartir:
 *   - 'shared': se abrió la hoja de compartir (nativo o Web Share API).
 *   - 'download': no había forma de adjuntar; se descargó el archivo y se
 *     abrió el cliente de correo para que el usuario lo adjunte a mano.
 */
export type ShareResult = 'shared' | 'download'

interface ShareOptions {
  title?: string
  /** Cuerpo/mensaje (asunto en nativo, cuerpo del correo en el fallback web) */
  text?: string
  /** Destinatario opcional para el `mailto:` de fallback */
  email?: string
}

/**
 * Comparte un archivo. En Android abre la hoja de compartir del sistema (con
 * el archivo adjunto, elegible por correo). En web usa la Web Share API con
 * archivos si está disponible; si no, descarga el archivo y abre el cliente de
 * correo (`mailto:`) para adjuntarlo manualmente.
 */
export async function shareFile(
  filename: string,
  blob: Blob,
  { title, text, email }: ShareOptions = {},
): Promise<ShareResult> {
  if (isNativePlatform()) {
    await Filesystem.writeFile({
      path: filename,
      data: await blob.text(),
      directory: Directory.Cache,
      encoding: Encoding.UTF8,
    })
    const { uri } = await Filesystem.getUri({ directory: Directory.Cache, path: filename })
    await Share.share({ title: title ?? filename, text, url: uri, dialogTitle: title ?? filename })
    return 'shared'
  }

  // Web Share API con archivos (móviles y algunos navegadores de escritorio)
  const file = new File([blob], filename, { type: blob.type })
  const nav = navigator as Navigator & { canShare?: (data: unknown) => boolean }
  if (typeof nav.share === 'function' && nav.canShare?.({ files: [file] })) {
    try {
      await nav.share({ files: [file], title, text })
      return 'shared'
    } catch (err) {
      // el usuario canceló la hoja de compartir: no hacemos fallback
      if (err instanceof DOMException && err.name === 'AbortError') return 'shared'
    }
  }

  // Fallback: descargar el archivo y abrir el cliente de correo
  webDownload(blob, filename)
  const params = new URLSearchParams()
  if (title) params.set('subject', title)
  if (text) params.set('body', text)
  const query = params.toString()
  window.open(`mailto:${email ?? ''}${query ? `?${query}` : ''}`, '_blank')
  return 'download'
}

/** Atajo para compartir texto (json/txt/html). */
export function shareText(
  filename: string,
  content: string,
  options: ShareOptions = {},
  mime = 'application/json;charset=utf-8',
): Promise<ShareResult> {
  return shareFile(filename, new Blob([content], { type: mime }), options)
}
