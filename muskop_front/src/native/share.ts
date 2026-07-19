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
