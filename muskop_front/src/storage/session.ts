// ==========================================================================
// Formato de archivo de sesión de Muskop (ver ROADMAP.md).
// Una sesión es la "base de datos" completa de un usuario: su librería de
// recursos, sus rutinas y su configuración. Se guarda en el dispositivo
// (IndexedDB) y se puede descargar/importar como archivo .muskop.json.
//
// Decisiones (2026-07-19):
//   - Ids de recursos y rutinas por **UUID** (antes autoincremento local), para
//     que importar/fusionar sesiones no provoque colisiones.
//   - **Varias sesiones por usuario**: un `label` opcional distingue, p. ej.,
//     "carlos — clásica" de "carlos — eléctrica".
// ==========================================================================

import {
  newBlockId,
  uuid,
  type PracticeBlock,
  type PracticeEntry,
  type Routine,
} from '../types/routine'
import type { CollectionContent, ExerciseMeta } from '../types/tab'

export const SESSION_VERSION = 1

export interface SessionUser {
  username: string
  /** Etiqueta opcional para distinguir varias sesiones del mismo usuario */
  label?: string | null
  createdAt: string
  updatedAt: string
}

export interface SessionResource {
  id: string
  title: string
  type: string
  category: string | null
  content: unknown
  /** Metadatos de ejercicio si el usuario lo marcó como tal */
  exercise?: ExerciseMeta | null
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
}

export function createSession(username: string, label?: string): MuskopSession {
  const now = new Date().toISOString()
  return {
    muskopSession: SESSION_VERSION,
    user: { username, label: label?.trim() || null, createdAt: now, updatedAt: now },
    resources: [],
    routines: [],
    practiceLog: [],
    experience: {},
    settings: {},
  }
}

/**
 * Convierte un id (numérico de sesiones antiguas o string) a string. Como la
 * conversión es determinista (`1` → `"1"`), las referencias entre recursos,
 * bloques y registro siguen enlazadas tras migrar de autoincremento a UUID.
 */
function toId(raw: unknown): string | null {
  if (typeof raw === 'string' && raw.trim()) return raw
  if (typeof raw === 'number' && Number.isFinite(raw)) return String(raw)
  return null
}

function normalizePracticeLog(raw: unknown): PracticeEntry[] {
  if (!Array.isArray(raw)) return []
  return raw.flatMap((e) => {
    const routineId = toId(e?.routineId)
    if (
      !e ||
      typeof e !== 'object' ||
      typeof e.date !== 'string' ||
      routineId === null ||
      typeof e.minutes !== 'number' ||
      typeof e.completed !== 'boolean'
    ) {
      return []
    }
    return [{ date: e.date, routineId, minutes: e.minutes, completed: e.completed }]
  })
}

export function normalizeExercise(raw: unknown): ExerciseMeta | null {
  if (!raw || typeof raw !== 'object') return null
  const e = raw as Record<string, unknown>
  if (typeof e.skill !== 'string' || !e.skill.trim()) return null
  const level = typeof e.level === 'number' && e.level >= 1 ? Math.round(e.level) : 1
  return {
    skill: e.skill,
    level,
    description: typeof e.description === 'string' ? e.description : '',
  }
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
  const r = raw as Partial<Routine> & { id?: unknown }
  const id = toId(r?.id)
  if (!r || typeof r !== 'object' || id === null || typeof r.name !== 'string') {
    return null
  }
  const blocks: PracticeBlock[] = (Array.isArray(r.blocks) ? r.blocks : []).map((b) => ({
    id: typeof b.id === 'string' && b.id ? b.id : newBlockId(),
    name: typeof b.name === 'string' ? b.name : '',
    minutes: typeof b.minutes === 'number' && b.minutes > 0 ? b.minutes : 5,
    bpm: typeof b.bpm === 'number' ? b.bpm : null,
    resourceId: toId(b.resourceId),
    skill: typeof b.skill === 'string' ? b.skill : null,
    notes: typeof b.notes === 'string' ? b.notes : null,
  }))
  return {
    id,
    name: r.name,
    description: r.description ?? null,
    category: typeof r.category === 'string' ? r.category : null,
    blocks,
  }
}

/** Reescribe los ids de una colección a string (para migrar sesiones antiguas). */
function normalizeContent(content: unknown): unknown {
  if (content && typeof content === 'object' && (content as CollectionContent).kind === 'collection') {
    const c = content as CollectionContent & { resourceIds?: unknown }
    const ids = Array.isArray(c.resourceIds)
      ? c.resourceIds.map(toId).filter((id): id is string => id !== null)
      : []
    return { ...c, resourceIds: ids }
  }
  return content
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
  const rawResources = Array.isArray(obj.resources) ? obj.resources : []
  const resources: SessionResource[] = rawResources.map((r) => {
    const id = toId((r as { id?: unknown })?.id)
    if (id === null || typeof r.title !== 'string' || typeof r.type !== 'string') {
      throw new Error(`Recurso inválido en la sesión (id=${String((r as { id?: unknown })?.id)})`)
    }
    return {
      id,
      title: r.title,
      type: r.type,
      category: r.category ?? null,
      content: normalizeContent(r.content),
      exercise: normalizeExercise(r.exercise),
    }
  })
  const routines = (Array.isArray(obj.routines) ? obj.routines : [])
    .map(normalizeRoutine)
    .filter((r): r is Routine => r !== null)
  const now = new Date().toISOString()
  return {
    muskopSession: SESSION_VERSION,
    user: {
      username: obj.user.username,
      label: typeof obj.user.label === 'string' && obj.user.label.trim() ? obj.user.label : null,
      createdAt: obj.user.createdAt ?? now,
      updatedAt: obj.user.updatedAt ?? now,
    },
    resources,
    routines,
    practiceLog: normalizePracticeLog(obj.practiceLog),
    experience: normalizeExperience(obj.experience),
    settings: obj.settings && typeof obj.settings === 'object' ? obj.settings : {},
  }
}

/** Nombre descriptivo de la sesión (usuario + etiqueta, si la tiene). */
export function sessionLabel(session: MuskopSession): string {
  const { username, label } = session.user
  return label ? `${username} — ${label}` : username
}

export function sessionFilename(session: MuskopSession): string {
  const base = sessionLabel(session)
  const safe = base.replace(/[^\p{L}\p{N}\-_ ]/gu, '').trim().replace(/\s+/g, '-')
  return `${safe || 'sesion'}.muskop.json`
}

// Reexport para quien cree ids de recurso/rutina desde el gestor de sesión.
export { uuid }
