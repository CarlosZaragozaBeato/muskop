import type { ResourceDetail, SaveResourceRequest } from '../types/tab'
import { normalizeExercise } from '../storage/session'
import { translate as tr } from '../i18n/translate'

// ==========================================================================
// Importación/exportación de recursos de la librería como JSON, de forma
// individual o masiva. Formato portable entre sesiones: al importar se crean
// recursos nuevos (con id nuevo) para no colisionar con los existentes.
//   { "muskopResources": 1, "resources": [ { title, type, category, content,
//     exercise? } ] }
// ==========================================================================

export const RESOURCE_BUNDLE_VERSION = 1

/** Serializa uno o varios recursos a un JSON portable (sin ids). */
export function exportResourceBundle(resources: ResourceDetail[]): string {
  return JSON.stringify(
    {
      muskopResources: RESOURCE_BUNDLE_VERSION,
      resources: resources.map((r) => ({
        title: r.title,
        type: r.type,
        category: r.category ?? null,
        content: r.content,
        exercise: r.exercise ?? undefined,
      })),
    },
    null,
    2,
  )
}

function toRequest(raw: unknown): SaveResourceRequest {
  const r = raw as Record<string, unknown>
  if (!r || typeof r !== 'object' || typeof r.title !== 'string' || typeof r.type !== 'string') {
    throw new Error(tr('errors.resourceBadItem'))
  }
  return {
    title: r.title,
    type: r.type,
    category: typeof r.category === 'string' && r.category ? r.category : null,
    content: r.content,
    exercise: normalizeExercise(r.exercise),
  }
}

/**
 * Valida un JSON de recursos y devuelve peticiones de creación. Acepta tanto
 * un paquete (`{ muskopResources, resources: [...] }`) como un único recurso
 * suelto (`{ title, type, content, ... }`).
 */
export function parseResourceBundle(raw: string): SaveResourceRequest[] {
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    throw new Error(tr('errors.notJson'))
  }
  const obj = parsed as Record<string, unknown>
  if (!obj || typeof obj !== 'object') {
    throw new Error(tr('errors.notResources'))
  }
  if (Array.isArray(obj.resources)) {
    if (typeof obj.muskopResources === 'number' && obj.muskopResources > RESOURCE_BUNDLE_VERSION) {
      throw new Error(tr('errors.resourcesNewer', { v: obj.muskopResources }))
    }
    return obj.resources.map(toRequest)
  }
  // un único recurso suelto
  if (typeof obj.title === 'string' && typeof obj.type === 'string') {
    return [toRequest(obj)]
  }
  throw new Error(tr('errors.notResources'))
}
