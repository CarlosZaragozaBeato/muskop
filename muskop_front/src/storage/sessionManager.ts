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
  uuid,
  type MuskopSession,
  type SessionResource,
} from './session'
import type {
  ExerciseMeta,
  MediaContent,
  ResourceDetail,
  ResourceSummary,
  SaveResourceRequest,
} from '../types/tab'
import {
  GOAL_PERIODS,
  GOAL_XP,
  ROUTINE_COMPLETION_XP,
  periodClaimKey,
  type PracticeEntry,
  type PracticeGoals,
  type Routine,
} from '../types/routine'
import { minutesInCurrentPeriod } from '../utils/stats'
import { translate as tr } from '../i18n/translate'
import { saveText } from '../native/share'

// ==========================================================================
// Gestor de la sesión activa. Es la "base de datos" en memoria: cada
// mutación actualiza la sesión y la autoguarda en IndexedDB. La descarga
// del archivo .muskop.json es el backup/portabilidad oficial.
// ==========================================================================

const LAST_SESSION_KEY = 'muskop.lastSession'

let active: { deviceId: string; session: MuskopSession } | null = null

function requireActive(): { deviceId: string; session: MuskopSession } {
  if (!active) {
    throw new Error(tr('errors.noActiveSession'))
  }
  return active
}

async function persist(): Promise<void> {
  const { deviceId, session } = requireActive()
  session.user.updatedAt = new Date().toISOString()
  await putStoredSession({
    id: deviceId,
    username: session.user.username,
    label: session.user.label ?? null,
    updatedAt: session.user.updatedAt,
    data: session,
  })
}

// ---- ciclo de vida ----------------------------------------------------------

export function getActiveSession(): MuskopSession | null {
  return active?.session ?? null
}

export async function startNewSession(
  username: string,
  label?: string,
): Promise<MuskopSession> {
  const session = createSession(username.trim(), label)
  active = { deviceId: newDeviceId(), session }
  localStorage.setItem(LAST_SESSION_KEY, active.deviceId)
  await persist()
  return session
}

export async function openStoredSession(deviceId: string): Promise<MuskopSession> {
  const stored = await getStoredSession(deviceId)
  if (!stored) {
    throw new Error(tr('errors.sessionGone'))
  }
  active = { deviceId, session: parseSession(stored.data) }
  localStorage.setItem(LAST_SESSION_KEY, deviceId)
  // Reescribe la sesión ya normalizada: consolida la migración de sesiones
  // antiguas (ids numéricos → UUID string) en el almacenamiento, sin esperar a
  // la primera mutación.
  await persist()
  return active.session
}

export async function importSessionFile(file: File): Promise<MuskopSession> {
  let raw: unknown
  try {
    raw = JSON.parse(await file.text())
  } catch {
    throw new Error(tr('errors.fileNotJson'))
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

/** ¿Tiene la sesión activa algún recurso multimedia con binario? */
export function activeSessionHasMedia(): boolean {
  const session = getActiveSession()
  return !!session?.resources.some(
    (r) => r.type.toUpperCase() === 'MEDIA' && !!(r.content as MediaContent)?.data,
  )
}

/**
 * Devuelve una copia de la sesión sin los binarios de los recursos multimedia:
 * conserva los metadatos (título, tipo, mediaType, mime, nombre, tamaño) pero
 * vacía `data`, para que la exportación no arrastre archivos pesados.
 */
function stripMediaBinaries(session: MuskopSession): MuskopSession {
  return {
    ...session,
    resources: session.resources.map((r) => {
      if (r.type.toUpperCase() === 'MEDIA' && r.content && typeof r.content === 'object') {
        return { ...r, content: { ...(r.content as MediaContent), data: '' } }
      }
      return r
    }),
  }
}

/**
 * Descarga (web) o comparte (Android) la sesión activa como .muskop.json.
 * Por defecto **excluye** los binarios multimedia (pueden ser grandes); pasa
 * `includeMedia: true` para incluirlos.
 */
export function downloadActiveSession(includeMedia = false): Promise<void> {
  const { session } = requireActive()
  const data = includeMedia ? session : stripMediaBinaries(session)
  return saveText(sessionFilename(session), JSON.stringify(data, null, 2))
}

// ---- CRUD de recursos sobre la sesión activa --------------------------------

function toSummary(resource: SessionResource): ResourceSummary {
  return {
    id: resource.id,
    title: resource.title,
    type: resource.type,
    category: resource.category,
    exercise: resource.exercise ?? null,
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

export async function getResource(id: string): Promise<ResourceDetail> {
  const { session } = requireActive()
  const resource = session.resources.find((r) => r.id === id)
  if (!resource) {
    throw new Error(tr('errors.resourceNotFound', { id }))
  }
  return { ...toSummary(resource), content: resource.content }
}

export async function createResource(req: SaveResourceRequest): Promise<string> {
  const { session } = requireActive()
  const id = uuid()
  session.resources.push({
    id,
    title: req.title,
    type: req.type,
    category: req.category,
    content: req.content,
    exercise: req.exercise ?? null,
  })
  await persist()
  return id
}

export async function updateResource(id: string, req: SaveResourceRequest): Promise<void> {
  const { session } = requireActive()
  const resource = session.resources.find((r) => r.id === id)
  if (!resource) {
    throw new Error(tr('errors.resourceNotFound', { id }))
  }
  resource.title = req.title
  resource.type = req.type
  resource.category = req.category
  resource.content = req.content
  // Solo se toca `exercise` si la petición lo trae; así editar la tablatura no
  // borra los metadatos de ejercicio puestos desde la librería.
  if ('exercise' in req) {
    resource.exercise = req.exercise ?? null
  }
  await persist()
}

export async function deleteResource(id: string): Promise<void> {
  const { session } = requireActive()
  session.resources = session.resources.filter((r) => r.id !== id)
  await persist()
}

// ---- Ejercicios propios -----------------------------------------------------

/** Un recurso de la librería marcado como ejercicio, con su contenido. */
export interface OwnedExercise {
  id: string
  title: string
  /** TAB | THEORY */
  type: string
  category: string | null
  skill: string
  level: number
  description: string
  content: unknown
}

export async function listExercises(): Promise<OwnedExercise[]> {
  const { session } = requireActive()
  return session.resources
    .filter((r) => r.exercise)
    .map((r) => ({
      id: r.id,
      title: r.title,
      type: r.type,
      category: r.category,
      skill: r.exercise!.skill,
      level: r.exercise!.level,
      description: r.exercise!.description,
      content: r.content,
    }))
}

/** Marca (o desmarca, con null) un recurso como ejercicio sin tocar su contenido. */
export async function setResourceExercise(
  id: string,
  meta: ExerciseMeta | null,
): Promise<void> {
  const { session } = requireActive()
  const resource = session.resources.find((r) => r.id === id)
  if (!resource) {
    throw new Error(tr('errors.resourceNotFound', { id }))
  }
  resource.exercise = meta
  await persist()
}

// ---- CRUD de rutinas sobre la sesión activa ---------------------------------

export async function listRoutines(): Promise<Routine[]> {
  const { session } = requireActive()
  return session.routines
}

export async function getRoutine(id: string): Promise<Routine> {
  const { session } = requireActive()
  const routine = session.routines.find((r) => r.id === id)
  if (!routine) {
    throw new Error(tr('errors.routineNotFound', { id }))
  }
  return routine
}

export async function createRoutine(input: Omit<Routine, 'id'>): Promise<string> {
  const { session } = requireActive()
  const id = uuid()
  session.routines.push({ ...input, id })
  await persist()
  return id
}

export async function updateRoutine(id: string, input: Omit<Routine, 'id'>): Promise<void> {
  const { session } = requireActive()
  const index = session.routines.findIndex((r) => r.id === id)
  if (index < 0) {
    throw new Error(tr('errors.routineNotFound', { id }))
  }
  session.routines[index] = { ...input, id }
  await persist()
}

export async function deleteRoutine(id: string): Promise<void> {
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
  // nivel general: bonus al completar la rutina + al cumplir objetivos
  if (entry.completed) {
    session.bonusXp += ROUTINE_COMPLETION_XP
  }
  for (const period of GOAL_PERIODS) {
    const target = session.goals[period]
    if (target <= 0) continue
    const key = periodClaimKey(period)
    if (session.goalsClaimed.includes(key)) continue
    if (minutesInCurrentPeriod(session.practiceLog, period) >= target) {
      session.bonusXp += GOAL_XP[period]
      session.goalsClaimed.push(key)
    }
  }
  await persist()
}

/** XP total del nivel general: suma de habilidades + bonus. */
export function getGeneralXp(): number {
  const { session } = requireActive()
  const skillsXp = Object.values(session.experience).reduce((a, b) => a + b, 0)
  return skillsXp + session.bonusXp
}

export function getGoals(): PracticeGoals {
  const { session } = requireActive()
  return { ...session.goals }
}

export function getGoalsClaimed(): string[] {
  const { session } = requireActive()
  return session.goalsClaimed
}

export function getAchievements(): string[] {
  const { session } = requireActive()
  return session.achievements
}

/**
 * Marca como desbloqueados los logros cuya condición se cumple ahora
 * (`metIds`), fusionándolos con los ya conseguidos. Devuelve la lista
 * completa de desbloqueados. Solo persiste si hay alguno nuevo.
 */
export async function unlockAchievements(metIds: string[]): Promise<string[]> {
  const { session } = requireActive()
  let changed = false
  for (const id of metIds) {
    if (!session.achievements.includes(id)) {
      session.achievements.push(id)
      changed = true
    }
  }
  if (changed) await persist()
  return [...session.achievements]
}

export async function setGoals(goals: PracticeGoals): Promise<void> {
  const { session } = requireActive()
  session.goals = {
    weekly: Math.max(0, Math.round(goals.weekly) || 0),
    monthly: Math.max(0, Math.round(goals.monthly) || 0),
    yearly: Math.max(0, Math.round(goals.yearly) || 0),
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
