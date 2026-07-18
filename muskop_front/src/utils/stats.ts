import { todayKey, type PracticeEntry } from '../types/routine'

// ==========================================================================
// Estadísticas de constancia calculadas sobre el registro de práctica.
// ==========================================================================

function previousDay(dateKey: string): string {
  const date = new Date(dateKey + 'T12:00:00')
  date.setDate(date.getDate() - 1)
  return todayKey(date)
}

/**
 * Racha de días consecutivos con alguna práctica. La racha sigue viva si se
 * practicó hoy o ayer (todavía se está a tiempo hoy).
 */
export function practiceStreak(log: PracticeEntry[], filter?: (e: PracticeEntry) => boolean): number {
  const days = new Set(log.filter((e) => (filter ? filter(e) : true)).map((e) => e.date))
  if (days.size === 0) return 0
  let cursor = todayKey()
  if (!days.has(cursor)) {
    cursor = previousDay(cursor)
    if (!days.has(cursor)) return 0
  }
  let streak = 0
  while (days.has(cursor)) {
    streak += 1
    cursor = previousDay(cursor)
  }
  return streak
}

/** Racha de días consecutivos completando una rutina concreta. */
export function routineCompletionStreak(log: PracticeEntry[], routineId: number): number {
  return practiceStreak(log, (e) => e.routineId === routineId && e.completed)
}

/** Minutos practicados en los últimos `days` días (incluyendo hoy). */
export function minutesInLastDays(log: PracticeEntry[], days: number): number {
  let cursor = todayKey()
  const keys = new Set<string>()
  for (let i = 0; i < days; i++) {
    keys.add(cursor)
    cursor = previousDay(cursor)
  }
  return log.filter((e) => keys.has(e.date)).reduce((total, e) => total + e.minutes, 0)
}

/** Media de minutos al día en los últimos `days` días. */
export function dailyAverage(log: PracticeEntry[], days = 7): number {
  return Math.round((minutesInLastDays(log, days) / days) * 10) / 10
}

export function totalMinutes(log: PracticeEntry[]): number {
  return log.reduce((total, e) => total + e.minutes, 0)
}

export function timesCompleted(log: PracticeEntry[], routineId: number): number {
  return log.filter((e) => e.routineId === routineId && e.completed).length
}
