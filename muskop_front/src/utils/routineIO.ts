import type { ResourceSummary } from '../types/tab'
import {
  SKILLS,
  newBlockId,
  type PracticeBlock,
  type Routine,
} from '../types/routine'
import { translate as tr } from '../i18n/translate'

// ==========================================================================
// Importación/exportación de rutinas como JSON. Los recursos se referencian
// por título (`resourceTitle`) para que las rutinas sean portables entre
// sesiones: al importar se resuelven contra la librería de la sesión.
// ==========================================================================

export const ROUTINE_FILE_VERSION = 1

const SKILL_IDS = new Set(SKILLS.map((s) => s.id))

export interface RoutineImportResult {
  routine: Omit<Routine, 'id'>
  /** Avisos no fatales (p. ej. recursos que no se encontraron) */
  warnings: string[]
}

export function importRoutineFromJson(
  raw: string,
  resources: ResourceSummary[],
): RoutineImportResult {
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    throw new Error(tr('errors.notJson'))
  }
  const obj = parsed as Record<string, unknown>
  if (!obj || typeof obj !== 'object') {
    throw new Error(tr('errors.notRoutine'))
  }
  if (typeof obj.name !== 'string' || !obj.name.trim()) {
    throw new Error(tr('errors.routineNoName'))
  }
  if (!Array.isArray(obj.blocks) || obj.blocks.length === 0) {
    throw new Error(tr('errors.routineNoBlocks'))
  }

  const byTitle = new Map(resources.map((r) => [r.title.trim().toLowerCase(), r]))
  const warnings: string[] = []

  const blocks: PracticeBlock[] = obj.blocks.map((rawBlock, i) => {
    const b = rawBlock as Record<string, unknown>
    if (!b || typeof b !== 'object' || typeof b.name !== 'string' || !b.name.trim()) {
      throw new Error(tr('errors.blockNoName', { n: i + 1 }))
    }
    if (typeof b.minutes !== 'number' || b.minutes < 1 || b.minutes > 240) {
      throw new Error(tr('errors.blockMinutes', { n: i + 1, name: b.name }))
    }
    if (b.bpm !== undefined && b.bpm !== null && (typeof b.bpm !== 'number' || b.bpm < 20 || b.bpm > 300)) {
      throw new Error(tr('errors.blockBpm', { n: i + 1, name: b.name }))
    }
    let skill: string | null = null
    if (typeof b.skill === 'string' && b.skill) {
      if (!SKILL_IDS.has(b.skill)) {
        throw new Error(
          tr('errors.blockSkill', { n: i + 1, name: b.name, skill: b.skill, list: [...SKILL_IDS].join(', ') }),
        )
      }
      skill = b.skill
    }
    let resourceId: string | null = null
    if (typeof b.resourceTitle === 'string' && b.resourceTitle.trim()) {
      const found = byTitle.get(b.resourceTitle.trim().toLowerCase())
      if (found) {
        resourceId = found.id
      } else {
        warnings.push(tr('errors.blockResourceMissing', { n: i + 1, name: b.name, title: b.resourceTitle }))
      }
    }
    return {
      id: newBlockId(),
      name: b.name,
      minutes: Math.round(b.minutes),
      bpm: typeof b.bpm === 'number' ? b.bpm : null,
      resourceId,
      skill,
      notes: typeof b.notes === 'string' && b.notes ? b.notes : null,
    }
  })

  return {
    routine: {
      name: obj.name,
      description: typeof obj.description === 'string' && obj.description ? obj.description : null,
      category: typeof obj.category === 'string' && obj.category ? obj.category : null,
      blocks,
    },
    warnings,
  }
}

/** Serializa una rutina a JSON portable (recursos referenciados por título). */
export function exportRoutineToJson(routine: Routine, resources: ResourceSummary[]): string {
  const titleById = new Map(resources.map((r) => [r.id, r.title]))
  return JSON.stringify(
    {
      muskopRoutine: ROUTINE_FILE_VERSION,
      name: routine.name,
      description: routine.description ?? undefined,
      category: routine.category ?? undefined,
      blocks: routine.blocks.map((b) => ({
        name: b.name,
        minutes: b.minutes,
        bpm: b.bpm ?? undefined,
        skill: b.skill ?? undefined,
        resourceTitle: b.resourceId !== null ? titleById.get(b.resourceId) : undefined,
        notes: b.notes ?? undefined,
      })),
    },
    null,
    2,
  )
}

/** Ejemplo de rutina para copiárselo a un agente. */
export const EXAMPLE_ROUTINE_JSON = JSON.stringify(
  {
    muskopRoutine: 1,
    name: 'Rutina fingerstyle diaria',
    description: 'Sesión corta de mantenimiento',
    category: 'diaria',
    blocks: [
      {
        name: 'Calentamiento p-i-m-a',
        minutes: 5,
        bpm: 60,
        skill: 'arpegios',
        notes: 'Cuerdas al aire, mano relajada',
      },
      {
        name: 'Cambios Am ↔ C ↔ G',
        minutes: 10,
        bpm: 70,
        skill: 'cambios-acordes',
        notes: 'Un rasgueo por acorde sin parar el pulso',
      },
      {
        name: 'Patrón con tablatura',
        minutes: 10,
        bpm: 80,
        skill: 'patrones',
        resourceTitle: 'Mi ejercicio',
        notes: 'El recurso se busca por título en tu librería',
      },
    ],
  },
  null,
  2,
)
