import { SKILLS } from '../types/routine'
import type { ExerciseMeta, TheoryContent } from '../types/tab'
import { parseTabContent } from '../components/tab/tabModel'
import type { OwnedExercise } from '../storage/sessionManager'
import { translate as tr } from '../i18n/translate'

// ==========================================================================
// Import/export de "packs de ejercicios" (JSON). Un pack agrupa varios
// ejercicios propios (tablaturas o teoría) con sus metadatos de habilidad y
// nivel, para compartirlos, hacer copia de seguridad o pedírselos a un agente.
// Reutiliza los formatos de contenido ya existentes (TabDocument v2 y teoría).
// Ver AGENT_CONTEXT.md.
// ==========================================================================

export const EXERCISE_PACK_VERSION = 1

const SKILL_IDS = new Set(SKILLS.map((s) => s.id))

/** Un ejercicio listo para crear como recurso en la sesión. */
export interface PackExerciseInput {
  title: string
  type: 'TAB' | 'THEORY'
  category: string | null
  content: unknown
  exercise: ExerciseMeta
}

export interface PackImportResult {
  exercises: PackExerciseInput[]
  /** Avisos no fatales (habilidades corregidas, títulos duplicados…) */
  warnings: string[]
}

/** Serializa una lista de ejercicios propios a un pack JSON portable. */
export function exportExercisePack(exercises: OwnedExercise[]): string {
  return JSON.stringify(
    {
      muskopExercisePack: EXERCISE_PACK_VERSION,
      exercises: exercises.map((ex) => ({
        title: ex.title,
        type: ex.type,
        skill: ex.skill,
        level: ex.level,
        description: ex.description || undefined,
        content: ex.content,
      })),
    },
    null,
    2,
  )
}

/** Valida y normaliza un pack de ejercicios pegado o subido. */
export function importExercisePack(raw: string, existingTitles: string[]): PackImportResult {
  let parsed: unknown
  try {
    parsed = JSON.parse(raw)
  } catch {
    throw new Error(tr('errors.notJson'))
  }
  const obj = parsed as Record<string, unknown>
  if (!obj || typeof obj !== 'object') {
    throw new Error(tr('errors.notPack'))
  }
  if (typeof obj.muskopExercisePack !== 'number') {
    throw new Error(tr('errors.packMissingVersion'))
  }
  if (obj.muskopExercisePack > EXERCISE_PACK_VERSION) {
    throw new Error(tr('errors.packNewer', { v: obj.muskopExercisePack }))
  }
  if (!Array.isArray(obj.exercises) || obj.exercises.length === 0) {
    throw new Error(tr('errors.packNoExercises'))
  }

  const owned = new Set(existingTitles.map((t) => t.trim().toLowerCase()))
  const warnings: string[] = []
  const exercises: PackExerciseInput[] = obj.exercises.map((rawEx, i) => {
    const e = rawEx as Record<string, unknown>
    if (!e || typeof e !== 'object' || typeof e.title !== 'string' || !e.title.trim()) {
      throw new Error(tr('errors.exerciseNoTitle', { n: i + 1 }))
    }
    const title = e.title.trim()

    const rawType = typeof e.type === 'string' ? e.type.toUpperCase() : 'TAB'
    const type: 'TAB' | 'THEORY' = rawType === 'THEORY' ? 'THEORY' : 'TAB'

    let skill = 'general'
    if (typeof e.skill === 'string' && SKILL_IDS.has(e.skill)) {
      skill = e.skill
    } else if (e.skill) {
      warnings.push(tr('errors.exerciseSkillInvalid', { title, skill: String(e.skill) }))
    }

    const level =
      typeof e.level === 'number' && e.level >= 1 ? Math.round(e.level) : 1

    let content: unknown
    if (type === 'THEORY') {
      const body =
        typeof e.content === 'object' && e.content
          ? (e.content as Record<string, unknown>).body
          : e.body
      if (typeof body !== 'string' || !body.trim()) {
        throw new Error(tr('errors.exerciseTheoryBody', { title }))
      }
      content = { kind: 'theory', body } satisfies TheoryContent
    } else {
      try {
        content = parseTabContent(e.content)
      } catch {
        throw new Error(tr('errors.exerciseContentInvalid', { title }))
      }
    }

    if (owned.has(title.toLowerCase())) {
      warnings.push(tr('errors.exerciseDuplicate', { title }))
    }

    return {
      title,
      type,
      category: skill,
      content,
      exercise: {
        skill,
        level,
        description: typeof e.description === 'string' ? e.description : '',
      },
    }
  })

  return { exercises, warnings }
}

/** Pack de ejemplo para copiárselo a un agente. */
export const EXAMPLE_PACK_JSON = JSON.stringify(
  {
    muskopExercisePack: 1,
    exercises: [
      {
        title: 'Mi arpegio de calentamiento',
        type: 'TAB',
        skill: 'arpegios',
        level: 1,
        description: 'p-i-m-a sobre Am, mano relajada y sin mirar.',
        content: {
          version: 2,
          title: 'Mi arpegio de calentamiento',
          tuning: ['E', 'A', 'D', 'G', 'B', 'E'],
          timeSignature: '4/4',
          baseBpm: 60,
          sections: [
            {
              kind: 'fingerstyle',
              name: 'Am en negras',
              notes: 'p=5ª, i=3ª, m=2ª, a=1ª',
              measures: [
                {
                  events: [
                    { beat: 1, duration: 'quarter', notes: [{ string: 5, fret: 0, finger: 'p' }] },
                    { beat: 2, duration: 'quarter', notes: [{ string: 3, fret: 2, finger: 'i' }] },
                    { beat: 3, duration: 'quarter', notes: [{ string: 2, fret: 1, finger: 'm' }] },
                    { beat: 4, duration: 'quarter', notes: [{ string: 1, fret: 0, finger: 'a' }] },
                  ],
                },
              ],
            },
          ],
        },
      },
    ],
  },
  null,
  2,
)
