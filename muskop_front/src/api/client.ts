import * as sessions from '../storage/sessionManager'
import type {
  ExerciseMeta,
  ResourceDetail,
  ResourceSummary,
  SaveResourceRequest,
} from '../types/tab'

// ==========================================================================
// Capa de acceso a datos. Antes hablaba con el backend por HTTP; desde la
// fase 1 local-first (ver ROADMAP.md) delega en la sesión local del
// dispositivo. Se mantienen las firmas originales para no tocar el resto de
// la app: el parámetro userId ya no significa nada.
// ==========================================================================

export function createResource(_userId: number, req: SaveResourceRequest): Promise<string> {
  return sessions.createResource(req)
}

export function listResources(
  _userId: number,
  filters?: { type?: string; category?: string },
): Promise<ResourceSummary[]> {
  return sessions.listResources(filters)
}

export function getResource(id: string): Promise<ResourceDetail> {
  return sessions.getResource(id)
}

export function updateResource(id: string, req: SaveResourceRequest): Promise<void> {
  return sessions.updateResource(id, req)
}

export function deleteResource(id: string): Promise<void> {
  return sessions.deleteResource(id)
}

export function setResourceExercise(id: string, meta: ExerciseMeta | null): Promise<void> {
  return sessions.setResourceExercise(id, meta)
}
