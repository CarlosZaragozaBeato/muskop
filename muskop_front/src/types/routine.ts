// ==========================================================================
// Rutinas de práctica, registro de práctica y experiencia (fase 2+).
// Todo vive dentro del archivo de sesión.
// ==========================================================================

export interface PracticeBlock {
  id: string
  /** Qué se practica: "Calentamiento", "Pulgar alternado"… */
  name: string
  /** Duración del bloque en minutos */
  minutes: number
  /** BPM objetivo del bloque (null = libre) */
  bpm: number | null
  /** Recurso de la librería asociado (tablatura/acorde/fragmento), si lo hay */
  resourceId: string | null
  /** Habilidad que entrena el bloque (gana experiencia al practicarlo) */
  skill: string | null
  /** Indicaciones: en qué fijarse durante el bloque */
  notes: string | null
}

export interface Routine {
  id: string
  name: string
  description: string | null
  /** Tipo de rutina: diaria, esporádica, por canción, velocidad… */
  category: string | null
  blocks: PracticeBlock[]
}

export const ROUTINE_CATEGORIES = [
  'diaria',
  'esporádica',
  'canción',
  'velocidad',
  'técnica',
  'fingerstyle',
  'repertorio',
]

export function routineMinutes(routine: Routine | Omit<Routine, 'id'>): number {
  return routine.blocks.reduce((total, block) => total + (block.minutes || 0), 0)
}

/** Id único (UUID) para recursos, rutinas y bloques. Con UUIDs, importar o
 * fusionar sesiones nunca provoca colisiones de id. */
export function uuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  // respaldo para entornos sin crypto.randomUUID
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export function newBlockId(): string {
  return uuid()
}

export function newBlock(): PracticeBlock {
  return {
    id: newBlockId(),
    name: '',
    minutes: 5,
    bpm: null,
    resourceId: null,
    skill: null,
    notes: null,
  }
}

// ==========================================================================
// Registro de práctica: una entrada por sesión de práctica realizada.
// ==========================================================================

export interface PracticeEntry {
  /** Día local en formato YYYY-MM-DD */
  date: string
  routineId: string
  /** Minutos realmente practicados */
  minutes: number
  /** true si se llegó al final de la rutina */
  completed: boolean
}

export function todayKey(now = new Date()): string {
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// ==========================================================================
// Habilidades y niveles de experiencia. 10 XP por minuto practicado
// (con bonus del 20% al completar la rutina).
// ==========================================================================

export interface Skill {
  id: string
  label: string
  icon: string
}

export const SKILLS: Skill[] = [
  { id: 'arpegios', label: 'Arpegios', icon: '🎼' },
  { id: 'cambios-acordes', label: 'Cambios de acordes', icon: '🔀' },
  { id: 'patrones', label: 'Patrones', icon: '🖐' },
  { id: 'velocidad', label: 'Velocidad', icon: '⚡' },
  { id: 'tecnica', label: 'Técnica', icon: '🛠' },
  { id: 'repertorio', label: 'Repertorio', icon: '🎵' },
  { id: 'general', label: 'Práctica general', icon: '🎸' },
]

export const GENERAL_SKILL = 'general'

export function skillLabel(id: string): string {
  return SKILLS.find((s) => s.id === id)?.label ?? id
}

export const XP_PER_MINUTE = 10
export const COMPLETION_BONUS = 1.2

const LEVEL_TITLES = [
  'Principiante',
  'Iniciado',
  'Constante',
  'Sólido',
  'Avanzado',
  'Experto',
  'Maestro',
]

/**
 * Umbral triangular: subir al nivel L cuesta 100·(L−1) XP más que el
 * anterior. Nivel 2 a 100 XP (10 min), nivel 3 a 300, nivel 4 a 600…
 */
export interface LevelInfo {
  level: number
  title: string
  /** XP dentro del nivel actual */
  xpInLevel: number
  /** XP que hace falta acumular en este nivel para subir */
  xpForNext: number
}

export function levelFromXp(xp: number): LevelInfo {
  let level = 1
  let remaining = xp
  while (remaining >= level * 100) {
    remaining -= level * 100
    level += 1
  }
  return {
    level,
    title: LEVEL_TITLES[Math.min(level, LEVEL_TITLES.length) - 1],
    xpInLevel: remaining,
    xpForNext: level * 100,
  }
}
