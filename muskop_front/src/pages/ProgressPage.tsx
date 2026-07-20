import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { catalogFor } from '../data/skillCatalog'
import * as sessions from '../storage/sessionManager'
import type { OwnedExercise } from '../storage/sessionManager'
import {
  EMPTY_GOALS,
  GOAL_PERIODS,
  GOAL_XP,
  SKILLS,
  levelFromXp,
  periodClaimKey,
  type GoalPeriod,
  type PracticeEntry,
  type PracticeGoals,
} from '../types/routine'
import {
  dailyAverage,
  minutesInCurrentPeriod,
  practiceStreak,
  totalMinutes,
} from '../utils/stats'
import { useI18n } from '../i18n/I18nContext'

/**
 * Página de progreso: experiencia por habilidad con niveles, y ejercicios
 * recomendados del catálogo para el nivel actual de cada una.
 */
export default function ProgressPage() {
  const navigate = useNavigate()
  const { t, lang } = useI18n()
  const [experience, setExperience] = useState<Record<string, number>>({})
  const [log, setLog] = useState<PracticeEntry[]>([])
  const [exercises, setExercises] = useState<OwnedExercise[]>([])
  const [generalXp, setGeneralXp] = useState(0)
  const [goals, setGoalsState] = useState<PracticeGoals>(EMPTY_GOALS)
  const [claimed, setClaimed] = useState<string[]>([])

  useEffect(() => {
    try {
      setExperience({ ...sessions.getExperience() })
      setLog([...sessions.getPracticeLog()])
      setGeneralXp(sessions.getGeneralXp())
      setGoalsState(sessions.getGoals())
      setClaimed([...sessions.getGoalsClaimed()])
    } catch {
      // sin sesión activa no se llega aquí (ruta protegida)
    }
    sessions.listExercises().then(setExercises).catch(() => setExercises([]))
  }, [])

  const updateGoal = (period: GoalPeriod, value: number) => {
    const next = { ...goals, [period]: Math.max(0, value || 0) }
    setGoalsState(next)
    void sessions.setGoals(next)
  }

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
        <h2>{t('progress.title')}</h2>
        <button type="button" onClick={() => navigate('/achievements')}>
          {t('progress.achievementsLink')}
        </button>
      </div>

      <div className="stats-panel">
        <div className="stat">
          <span className="stat-value">🔥 {streak}</span>
          <span className="stat-label">{t('progress.streakLabel')}</span>
        </div>
        <div className="stat">
          <span className="stat-value">{average} min</span>
          <span className="stat-label">{t('progress.avgLabel')}</span>
        </div>
        <div className="stat">
          <span className="stat-value">{total} min</span>
          <span className="stat-label">{t('progress.totalLabel')}</span>
        </div>
      </div>

      {(() => {
        const info = levelFromXp(generalXp)
        const progress = Math.min(100, Math.round((info.xpInLevel / info.xpForNext) * 100))
        const levelTitle = t(`levels.${Math.min(info.level, 7)}`)
        return (
          <div className="general-level-card">
            <div className="skill-header">
              <span className="skill-icon">🏅</span>
              <div>
                <strong>{t('progress.generalTitle')}</strong>
                <div className="muted">
                  {t('progress.levelLine', { level: info.level, title: levelTitle, xp: generalXp })}
                </div>
              </div>
            </div>
            <div
              className="xp-bar"
              title={t('progress.xpForNext', { cur: info.xpInLevel, next: info.xpForNext, level: info.level + 1 })}
            >
              <div className="xp-bar-fill" style={{ width: `${progress}%` }} />
            </div>
            <div className="muted xp-bar-label">
              {t('progress.xpForNext', { cur: info.xpInLevel, next: info.xpForNext, level: info.level + 1 })}
            </div>
            <p className="muted">{t('progress.generalSub')}</p>
          </div>
        )
      })()}

      <section className="goals-section">
        <h3>{t('progress.goalsTitle')}</h3>
        <p className="muted">{t('progress.goalsIntro')}</p>
        <div className="goals-grid">
          {GOAL_PERIODS.map((period) => {
            const target = goals[period]
            const current = minutesInCurrentPeriod(log, period)
            const done =
              target > 0 && (current >= target || claimed.includes(periodClaimKey(period)))
            const pct = target > 0 ? Math.min(100, Math.round((current / target) * 100)) : 0
            const label = t(`progress.goal${period[0].toUpperCase()}${period.slice(1)}`)
            return (
              <div className={'goal-card' + (done ? ' done' : '')} key={period}>
                <div className="goal-head">
                  <strong>{label}</strong>
                  <span className="muted">{t('progress.goalReward', { xp: GOAL_XP[period] })}</span>
                </div>
                <label className="goal-target">
                  {t('progress.goalTargetLabel')}
                  <input
                    type="number"
                    min={0}
                    step={5}
                    value={target || ''}
                    onChange={(e) => updateGoal(period, Number(e.target.value))}
                  />
                </label>
                {target > 0 ? (
                  <>
                    <div className="xp-bar">
                      <div className="xp-bar-fill" style={{ width: `${pct}%` }} />
                    </div>
                    <div className="muted xp-bar-label">
                      {done
                        ? t('progress.goalDone')
                        : t('progress.goalProgress', { cur: current, target })}
                    </div>
                  </>
                ) : (
                  <div className="muted xp-bar-label">{t('progress.goalNoTarget')}</div>
                )}
              </div>
            )
          })}
        </div>
      </section>

      <p className="muted">{t('progress.intro')}</p>

      <div className="skills-grid">
        {ordered.map((skill) => {
          const xp = experience[skill.id] ?? 0
          const info = levelFromXp(xp)
          const catalog = catalogFor(skill.id, info.level, lang)
          const progress = Math.min(100, Math.round((info.xpInLevel / info.xpForNext) * 100))
          const skillExercises = bySkill[skill.id] ?? []
          // los pensados para tu nivel actual o por debajo; si aún no llegas a
          // ninguno, muestra los de nivel más bajo como "próximos"
          const forNow = skillExercises.filter((ex) => ex.level <= info.level)
          const recommended = forNow.length > 0 ? forNow : skillExercises.slice(0, 3)
          const skillName = t(`skills.${skill.id}`)
          const levelTitle = t(`levels.${Math.min(info.level, 7)}`)
          return (
            <div className="skill-card" key={skill.id}>
              <div className="skill-header">
                <span className="skill-icon">{skill.icon}</span>
                <div>
                  <strong>{skillName}</strong>
                  <div className="muted">
                    {t('progress.levelLine', { level: info.level, title: levelTitle, xp })}
                  </div>
                </div>
              </div>
              <div className="xp-bar" title={t('progress.xpForNext', { cur: info.xpInLevel, next: info.xpForNext, level: info.level + 1 })}>
                <div className="xp-bar-fill" style={{ width: `${progress}%` }} />
              </div>
              <div className="muted xp-bar-label">
                {t('progress.xpForNext', { cur: info.xpInLevel, next: info.xpForNext, level: info.level + 1 })}
              </div>
              {catalog && (
                <div className="skill-catalog">
                  <strong className="muted">{t('progress.forYourLevel', { level: info.level })}</strong>
                  <ul>
                    {catalog.items.map((item, i) => (
                      <li key={i}>{item}</li>
                    ))}
                  </ul>
                </div>
              )}
              {recommended.length > 0 && (
                <div className="skill-catalog skill-own">
                  <strong className="muted">{t('progress.yourExercises')}</strong>
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
                          {t('progress.exerciseLevel', { title: ex.title, level: ex.level })}
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              {skill.id !== 'general' && (
                <Link className="skill-explore-link" to={`/explore?skill=${skill.id}`}>
                  {t('progress.exploreLink', { skill: skillName.toLowerCase() })}
                </Link>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
