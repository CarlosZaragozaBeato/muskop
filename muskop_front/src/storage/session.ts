// ==========================================================================
// Formato de archivo de sesión de Muskop (ver ROADMAP.md).
// Una sesión es la "base de datos" completa de un usuario: su librería de
// recursos, sus rutinas y su configuración. Se guarda en el dispositivo
// (IndexedDB) y se puede descargar/importar como archivo .muskop.json.
// ==========================================================================

import {
  newBlockId,
  type PracticeBlock,
  type PracticeEntry,
  type Routine,
} from '../types/routine'

export const SESSION_VERSION = 1

export interface SessionUser {
  username: string
  createdAt: string
  updatedAt: string
}

export interface SessionResource {
  id: number
  title: string
  type: string
  category: string | null
  content: unknown
}

export interface MuskopSession {
  muskopSession: number
  user: SessionUser
  resources: SessionResource[]
  /** Rutinas de práctica con sus bloques */
  routines: Routine[]
  /** Registro de práctica: una entrada por sesión de práctica */
  practiceLog: PracticeEntry[]
  /** Experiencia acumulada por habilidad (XP) */
  experience: Record<string, number>
  settings: Record<string, unknown>
  /** Contadores internos para que los ids no colisionen al seguir creando */
  nextResourceId: number
  nextRoutineId: number
}

export function createSession(username: string): MuskopSession {
  const now = new Date().toISOString()
  return {
    muskopSession: SESSION_VERSION,
    user: { username, createdAt: now, updatedAt: now },
    resources: [],
    routines: [],
    practiceLog: [],
    experience: {},
    settings: {},
    nextResourceId: 1,
    nextRoutineId: 1,
  }
}

function normalizePracticeLog(raw: unknown): PracticeEntry[] {
  if (!Array.isArray(raw)) return []
  return raw.filter(
    (e): e is PracticeEntry =>
      e &&
      typeof e === 'object' &&
      typeof e.date === 'string' &&
      typeof e.routineId === 'number' &&
      typeof e.minutes === 'number' &&
      typeof e.completed === 'boolean',
  )
}

function normalizeExperience(raw: unknown): Record<string, number> {
  if (!raw || typeof raw !== 'object') return {}
  const result: Record<string, number> = {}
  for (const [key, value] of Object.entries(raw as Record<string, unknown>)) {
    if (typeof value === 'number' && value > 0) {
      result[key] = value
    }
  }
  return result
}

function normalizeRoutine(raw: unknown): Routine | null {
  const r = raw as Partial<Routine>
  if (!r || typeof r !== 'object' || typeof r.id !== 'number' || typeof r.name !== 'string') {
    return null
  }
  const blocks: PracticeBlock[] = (Array.isArray(r.blocks) ? r.blocks : []).map((b) => ({
    id: typeof b.id === 'string' ? b.id : newBlockId(),
    name: typeof b.name === 'string' ? b.name : '',
    minutes: typeof b.minutes === 'number' && b.minutes > 0 ? b.minutes : 5,
    bpm: typeof b.bpm === 'number' ? b.bpm : null,
    resourceId: typeof b.resourceId === 'number' ? b.resourceId : null,
    skill: typeof b.skill === 'string' ? b.skill : null,
    notes: typeof b.notes === 'string' ? b.notes : null,
  }))
  return {
    id: r.id,
    name: r.name,
    description: r.description ?? null,
    category: typeof r.category === 'string' ? r.category : null,
    blocks,
  }
}

/** Valida (y normaliza) una sesión leída de un archivo. Lanza si no es válida. */
export function parseSession(raw: unknown): MuskopSession {
  const obj = raw as Partial<MuskopSession>
  if (!obj || typeof obj !== 'object' || typeof obj.muskopSession !== 'number') {
    throw new Error('El archivo no es una sesión de Muskop (falta "muskopSession")')
  }
  if (obj.muskopSession > SESSION_VERSION) {
    throw new Error(
      `La sesión es de una versión más nueva (${obj.muskopSession}) que esta aplicación (${SESSION_VERSION})`,
    )
  }
  if (!obj.user || typeof obj.user.username !== 'string' || !obj.user.username.trim()) {
    throw new Error('La sesión no tiene usuario válido')
  }
  const resources = Array.isArray(obj.resources) ? obj.resources : []
  for (const r of resources) {
    if (typeof r.id !== 'number' || typeof r.title !== 'string' || typeof r.type !== 'string') {
      throw new Error(`Recurso inválido en la sesión (id=${String(r?.id)})`)
    }
  }
  const maxId = resources.reduce((max, r) => Math.max(max, r.id), 0)
  const routines = (Array.isArray(obj.routines) ? obj.routines : [])
    .map(normalizeRoutine)
    .filter((r): r is Routine => r !== null)
  const maxRoutineId = routines.reduce((max, r) => Math.max(max, r.id), 0)
  const now = new Date().toISOString()
  return {
    muskopSession: SESSION_VERSION,
    user: {
      username: obj.user.username,
      createdAt: obj.user.createdAt ?? now,
      updatedAt: obj.user.updatedAt ?? now,
    },
    resources: resources.map((r) => ({ ...r, category: r.category ?? null })),
    routines,
    practiceLog: normalizePracticeLog(obj.practiceLog),
    experience: normalizeExperience(obj.experience),
    settings: obj.settings && typeof obj.settings === 'object' ? obj.settings : {},
    nextResourceId:
      typeof obj.nextResourceId === 'number' && obj.nextResourceId > maxId
        ? obj.nextResourceId
        : maxId + 1,
    nextRoutineId:
      typeof obj.nextRoutineId === 'number' && obj.nextRoutineId > maxRoutineId
        ? obj.nextRoutineId
        : maxRoutineId + 1,
  }
}

export function sessionFilename(session: MuskopSession): string {
  const safe = session.user.username.replace(/[^\p{L}\p{N}\-_ ]/gu, '').trim().replace(/\s+/g, '-')
  return `${safe || 'sesion'}.muskop.json`
}
