import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { catalogFor } from '../data/skillCatalog'
import * as sessions from '../storage/sessionManager'
import type { OwnedExercise } from '../storage/sessionManager'
import {
  SKILLS,
  levelFromXp,
  type PracticeEntry,
} from '../types/routine'
import { dailyAverage, practiceStreak, totalMinutes } from '../utils/stats'

/**
 * Página de progreso: experiencia por habilidad con niveles, y ejercicios
 * recomendados del catálogo para el nivel actual de cada una.
 */
export default function ProgressPage() {
  const navigate = useNavigate()
  const [experience, setExperience] = useState<Record<string, number>>({})
  const [log, setLog] = useState<PracticeEntry[]>([])
  const [exercises, setExercises] = useState<OwnedExercise[]>([])

  useEffect(() => {
    try {
      setExperience({ ...sessions.getExperience() })
      setLog([...sessions.getPracticeLog()])
    } catch {
      // sin sesión activa no se llega aquí (ruta protegida)
    }
    sessions.listExercises().then(setExercises).catch(() => setExercises([]))
  }, [])

  // ejercicios propios agrupados por habilidad, ordenados por nivel
  const bySkill = useMemo(() => {
    const map: Record<string, OwnedExercise[]> = {}
    for (const ex of exercises) {
      ;(map[ex.skill] ??= []).push(ex)
    }
    for (const list of Object.values(map)) {
      list.sort((a, b) => a.level - b.level)
    }
    return map
  }, [exercises])

  const streak = practiceStreak(log)
  const average = dailyAverage(log, 7)
  const total = totalMinutes(log)

  // habilidades con experiencia primero, el resto después
  const ordered = [...SKILLS].sort(
    (a, b) => (experience[b.id] ?? 0) - (experience[a.id] ?? 0),
  )

  return (
    <div className="progress-page">
      <div className="page-header">
        <h2>Progreso</h2>
      </div>

      <div className="stats-panel">
        <div className="stat">
          <span className="stat-value">🔥 {streak}</span>
          <span className="stat-label">racha de días practicando</span>
        </div>
        <div className="stat">
          <span className="stat-value">{average} min</span>
          <span className="stat-label">media diaria (últimos 7 días)</span>
        </div>
        <div className="stat">
          <span className="stat-value">{total} min</span>
          <span className="stat-label">tiempo total practicado</span>
        </div>
      </div>

      <p className="muted">
        Cada minuto practicado en un bloque suma 10 XP a su habilidad (con un
        20% extra al completar la rutina). Asigna la habilidad de cada bloque
        en el editor de rutinas.
      </p>

      <div className="skills-grid">
        {ordered.map((skill) => {
          const xp = experience[skill.id] ?? 0
          const info = levelFromXp(xp)
          const catalog = catalogFor(skill.id, info.level)
          const progress = Math.min(100, Math.round((info.xpInLevel / info.xpForNext) * 100))
          const skillExercises = bySkill[skill.id] ?? []
          // los pensados para tu nivel actual o por debajo; si aún no llegas a
          // ninguno, muestra los de nivel más bajo como "próximos"
          const forNow = skillExercises.filter((ex) => ex.level <= info.level)
          const recommended = forNow.length > 0 ? forNow : skillExercises.slice(0, 3)
          return (
            <div className="skill-card" key={skill.id}>
              <div className="skill-header">
                <span className="skill-icon">{skill.icon}</span>
                <div>
                  <strong>{skill.label}</strong>
                  <div className="muted">
                    Nivel {info.level} · {info.title} · {xp} XP
                  </div>
                </div>
              </div>
              <div className="xp-bar" title={`${info.xpInLevel}/${info.xpForNext} XP para el nivel ${info.level + 1}`}>
                <div className="xp-bar-fill" style={{ width: `${progress}%` }} />
              </div>
              <div className="muted xp-bar-label">
                {info.xpInLevel}/{info.xpForNext} XP para nivel {info.level + 1}
              </div>
              {catalog && (
                <div className="skill-catalog">
                  <strong className="muted">Para tu nivel {info.level}:</strong>
                  <ul>
                    {catalog.items.map((item, i) => (
                      <li key={i}>{item}</li>
                    ))}
                  </ul>
                </div>
              )}
              {recommended.length > 0 && (
                <div className="skill-catalog skill-own">
                  <strong className="muted">Tus ejercicios:</strong>
                  <ul>
                    {recommended.map((ex) => (
                      <li key={ex.id}>
                        <button
                          type="button"
                          className="linklike"
                          onClick={() =>
                            navigate(
                              ex.type.toUpperCase() === 'THEORY'
                                ? `/theory/${ex.id}`
                                : `/tabs/${ex.id}`,
                            )
                          }
                        >
                          {ex.title} · nivel {ex.level}
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {skill.id !== 'general' && (
                <Link className="skill-explore-link" to={`/explore?skill=${skill.id}`}>
                  Ejercicios de {skill.label.toLowerCase()} en Explorar →
                </Link>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
