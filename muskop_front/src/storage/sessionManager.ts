import {
  deleteStoredSession,
  getStoredSession,
  listStoredSessions,
  newDeviceId,
  putStoredSession,
  type StoredSession,
} from './db'
import {
  createSession,
  parseSession,
  sessionFilename,
  type MuskopSession,
  type SessionResource,
} from './session'
import type { ResourceDetail, ResourceSummary, SaveResourceRequest } from '../types/tab'
import type { PracticeEntry, Routine } from '../types/routine'

// ==========================================================================
// Gestor de la sesión activa. Es la "base de datos" en memoria: cada
// mutación actualiza la sesión y la autoguarda en IndexedDB. La descarga
// del archivo .muskop.json es el backup/portabilidad oficial.
// ==========================================================================

const LAST_SESSION_KEY = 'muskop.lastSession'

let active: { deviceId: string; session: MuskopSession } | null = null

function requireActive(): { deviceId: string; session: MuskopSession } {
  if (!active) {
    throw new Error('No hay ninguna sesión abierta')
  }
  return active
}

async function persist(): Promise<void> {
  const { deviceId, session } = requireActive()
  session.user.updatedAt = new Date().toISOString()
  await putStoredSession({
    id: deviceId,
    username: session.user.username,
    updatedAt: session.user.updatedAt,
    data: session,
  })
}

// ---- ciclo de vida ----------------------------------------------------------

export function getActiveSession(): MuskopSession | null {
  return active?.session ?? null
}

export async function startNewSession(username: string): Promise<MuskopSession> {
  const session = createSession(username.trim())
  active = { deviceId: newDeviceId(), session }
  localStorage.setItem(LAST_SESSION_KEY, active.deviceId)
  await persist()
  return session
}

export async function openStoredSession(deviceId: string): Promise<MuskopSession> {
  const stored = await getStoredSession(deviceId)
  if (!stored) {
    throw new Error('La sesión ya no está en este dispositivo')
  }
  active = { deviceId, session: parseSession(stored.data) }
  localStorage.setItem(LAST_SESSION_KEY, deviceId)
  return active.session
}

export async function importSessionFile(file: File): Promise<MuskopSession> {
  let raw: unknown
  try {
    raw = JSON.parse(await file.text())
  } catch {
    throw new Error('El archivo no contiene JSON válido')
  }
  const session = parseSession(raw)
  active = { deviceId: newDeviceId(), session }
  localStorage.setItem(LAST_SESSION_KEY, active.deviceId)
  await persist()
  return session
}

/** Reabre la última sesión usada en este dispositivo, si sigue existiendo. */
export async function resumeLastSession(): Promise<MuskopSession | null> {
  const deviceId = localStorage.getItem(LAST_SESSION_KEY)
  if (!deviceId) return null
  try {
    return await openStoredSession(deviceId)
  } catch {
    localStorage.removeItem(LAST_SESSION_KEY)
    return null
  }
}

export function closeSession(): void {
  active = null
  localStorage.removeItem(LAST_SESSION_KEY)
}

export function listDeviceSessions(): Promise<StoredSession[]> {
  return listStoredSessions()
}

export function removeDeviceSession(deviceId: string): Promise<unknown> {
  return deleteStoredSession(deviceId)
}

/** Descarga la sesión activa como archivo .muskop.json */
export function downloadActiveSession(): void {
  const { session } = requireActive()
  const blob = new Blob([JSON.stringify(session, null, 2)], {
    type: 'application/json;charset=utf-8',
  })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = sessionFilename(session)
  a.click()
  URL.revokeObjectURL(url)
}

// ---- CRUD de recursos sobre la sesión activa --------------------------------

function toSummary(resource: SessionResource): ResourceSummary {
  return {
    id: resource.id,
    title: resource.title,
    type: resource.type,
    category: resource.category,
  }
}

export async function listResources(filters?: {
  type?: string
  category?: string
}): Promise<ResourceSummary[]> {
  const { session } = requireActive()
  return session.resources
    .filter(
      (r) =>
        (!filters?.type || r.type.toUpperCase() === filters.type.toUpperCase()) &&
        (!filters?.category || r.category?.toLowerCase() === filters.category.toLowerCase()),
    )
    .map(toSummary)
}

export async function getResource(id: number): Promise<ResourceDetail> {
  const { session } = requireActive()
  const resource = session.resources.find((r) => r.id === id)
  if (!resource) {
    throw new Error(`No existe el recurso #${id} en esta sesión`)
  }
  return { ...toSummary(resource), content: resource.content }
}

export async function createResource(req: SaveResourceRequest): Promise<number> {
  const { session } = requireActive()
  const id = session.nextResourceId
  session.nextResourceId += 1
  session.resources.push({
    id,
    title: req.title,
    type: req.type,
    category: req.category,
    content: req.content,
  })
  await persist()
  return id
}

export async function updateResource(id: number, req: SaveResourceRequest): Promise<void> {
  const { session } = requireActive()
  const resource = session.resources.find((r) => r.id === id)
  if (!resource) {
    throw new Error(`No existe el recurso #${id} en esta sesión`)
  }
  resource.title = req.title
  resource.type = req.type
  resource.category = req.category
  resource.content = req.content
  await persist()
}

export async function deleteResource(id: number): Promise<void> {
  const { session } = requireActive()
  session.resources = session.resources.filter((r) => r.id !== id)
  await persist()
}

// ---- CRUD de rutinas sobre la sesión activa ---------------------------------

export async function listRoutines(): Promise<Routine[]> {
  const { session } = requireActive()
  return session.routines
}

export async function getRoutine(id: number): Promise<Routine> {
  const { session } = requireActive()
  const routine = session.routines.find((r) => r.id === id)
  if (!routine) {
    throw new Error(`No existe la rutina #${id} en esta sesión`)
  }
  return routine
}

export async function createRoutine(input: Omit<Routine, 'id'>): Promise<number> {
  const { session } = requireActive()
  const id = session.nextRoutineId
  session.nextRoutineId += 1
  session.routines.push({ ...input, id })
  await persist()
  return id
}

export async function updateRoutine(id: number, input: Omit<Routine, 'id'>): Promise<void> {
  const { session } = requireActive()
  const index = session.routines.findIndex((r) => r.id === id)
  if (index < 0) {
    throw new Error(`No existe la rutina #${id} en esta sesión`)
  }
  session.routines[index] = { ...input, id }
  await persist()
}

export async function deleteRoutine(id: number): Promise<void> {
  const { session } = requireActive()
  session.routines = session.routines.filter((r) => r.id !== id)
  await persist()
}

// ---- Registro de práctica y experiencia -------------------------------------

export async function recordPractice(
  entry: PracticeEntry,
  xpGains: Record<string, number>,
): Promise<void> {
  const { session } = requireActive()
  session.practiceLog.push(entry)
  for (const [skill, xp] of Object.entries(xpGains)) {
    if (xp > 0) {
      session.experience[skill] = (session.experience[skill] ?? 0) + xp
    }
  }
  await persist()
}

export function getPracticeLog(): PracticeEntry[] {
  const { session } = requireActive()
  return session.practiceLog
}

export function getExperience(): Record<string, number> {
  const { session } = requireActive()
  return session.experience
}
