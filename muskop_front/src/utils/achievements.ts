// ==========================================================================
// Catálogo de logros del usuario de la sesión. Cada logro se evalúa contra
// unas estadísticas derivadas de la sesión (práctica, rachas, nivel, librería).
// Los logros conseguidos se guardan en la sesión (`achievements`) para que
// sean permanentes aunque luego cambie la estadística (p. ej. una racha).
// ==========================================================================

export interface AchievementStats {
  /** Nº de sesiones de práctica registradas */
  practices: number
  /** Nº de rutinas completadas */
  completed: number
  /** Racha de días consecutivos practicando */
  streak: number
  /** Minutos totales practicados */
  totalMinutes: number
  /** Nivel general actual */
  generalLevel: number
  /** Nivel más alto alcanzado en alguna habilidad */
  maxSkillLevel: number
  /** Nº de objetivos (semana/mes/año) cumplidos alguna vez */
  goalsMet: number
  /** Nº de recursos en la librería */
  resourceCount: number
  /** ¿Tiene algún recurso multimedia? */
  hasMedia: boolean
}

export interface Achievement {
  id: string
  icon: string
  met: (s: AchievementStats) => boolean
}

/** Orden = orden de aparición en la página. */
export const ACHIEVEMENTS: Achievement[] = [
  { id: 'firstSteps', icon: '🌱', met: (s) => s.practices >= 1 },
  { id: 'firstRoutine', icon: '✅', met: (s) => s.completed >= 1 },
  { id: 'routines10', icon: '🎯', met: (s) => s.completed >= 10 },
  { id: 'routines50', icon: '🏆', met: (s) => s.completed >= 50 },
  { id: 'streak3', icon: '🔥', met: (s) => s.streak >= 3 },
  { id: 'streak7', icon: '🔥', met: (s) => s.streak >= 7 },
  { id: 'streak30', icon: '🔥', met: (s) => s.streak >= 30 },
  { id: 'time60', icon: '⏱️', met: (s) => s.totalMinutes >= 60 },
  { id: 'time600', icon: '⏳', met: (s) => s.totalMinutes >= 600 },
  { id: 'level5', icon: '⭐', met: (s) => s.generalLevel >= 5 },
  { id: 'level10', icon: '🌟', met: (s) => s.generalLevel >= 10 },
  { id: 'skillMaster', icon: '🎖️', met: (s) => s.maxSkillLevel >= 5 },
  { id: 'goalGetter', icon: '📅', met: (s) => s.goalsMet >= 1 },
  { id: 'library5', icon: '📚', met: (s) => s.resourceCount >= 5 },
  { id: 'firstMedia', icon: '🎬', met: (s) => s.hasMedia },
]

/** Ids de los logros cuya condición se cumple ahora mismo. */
export function evaluateAchievements(stats: AchievementStats): string[] {
  return ACHIEVEMENTS.filter((a) => a.met(stats)).map((a) => a.id)
}
