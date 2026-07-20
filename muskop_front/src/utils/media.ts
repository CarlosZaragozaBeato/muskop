import type { MediaKind } from '../types/tab'

// ==========================================================================
// Utilidades para recursos multimedia (imágenes, audio y vídeo).
//
// Los archivos se guardan como data URL base64 dentro de la sesión, por lo que
// hay un límite de tamaño: los binarios grandes inflarían la sesión (que se
// exporta como un único JSON) y el almacenamiento local del dispositivo.
// ==========================================================================

/** Tamaño máximo por archivo (bytes). El base64 infla ~33% el tamaño real. */
export const MAX_MEDIA_BYTES = 8 * 1024 * 1024 // 8 MB

/** Prefijos MIME aceptados por cada tipo de recurso multimedia. */
const MIME_PREFIX: Record<MediaKind, string> = {
  image: 'image/',
  audio: 'audio/',
  video: 'video/',
}

/** Deduce el tipo de recurso (image/audio/video) a partir del MIME. */
export function mediaKindFromMime(mime: string): MediaKind | null {
  if (mime.startsWith('image/')) return 'image'
  if (mime.startsWith('audio/')) return 'audio'
  if (mime.startsWith('video/')) return 'video'
  return null
}

/** `accept` para el input de archivo (todas las categorías soportadas). */
export const MEDIA_ACCEPT = 'image/*,audio/*,video/*'

export { MIME_PREFIX }

/** Formatea un tamaño en bytes de forma legible (KB/MB). */
export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/** Lee un `File` como data URL base64. */
export function readAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(reader.error ?? new Error('read error'))
    reader.readAsDataURL(file)
  })
}
