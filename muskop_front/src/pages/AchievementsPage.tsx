import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import * as sessions from '../storage/sessionManager'
import { levelFromXp } from '../types/routine'
import { practiceStreak, totalMinutes } from '../utils/stats'
import {
  ACHIEVEMENTS,
  evaluateAchievements,
  type AchievementStats,
} from '../utils/achievements'
import { useI18n } from '../i18n/I18nContext'

/**
 * Página de logros: insignias que el usuario de la sesión desbloquea por su
 * práctica, rachas, nivel, objetivos y librería. Una vez conseguidos se
 * guardan en la sesión, así que son permanentes.
 */
export default function AchievementsPage() {
  const { t } = useI18n()
  const navigate = useNavigate()
  const [unlocked, setUnlocked] = useState<string[]>([])

  useEffect(() => {
    let active = true
    async function load() {
      try {
        const log = [...sessions.getPracticeLog()]
        const experience = sessions.getExperience()
        const resources = await sessions.listResources()
        const maxSkillLevel = Object.values(experience).reduce(
          (max, xp) => Math.max(max, levelFromXp(xp).level),
          0,
        )
        const stats: AchievementStats = {
          practices: log.length,
          completed: log.filter((e) => e.completed).length,
          streak: practiceStreak(log),
          totalMinutes: totalMinutes(log),
          generalLevel: levelFromXp(sessions.getGeneralXp()).level,
          maxSkillLevel,
          goalsMet: sessions.getGoalsClaimed().length,
          resourceCount: resources.length,
          hasMedia: resources.some((r) => r.type.toUpperCase() === 'MEDIA'),
        }
        const merged = await sessions.unlockAchievements(evaluateAchievements(stats))
        if (active) setUnlocked(merged)
      } catch {
        // sin sesión activa no se llega aquí (ruta protegida)
      }
    }
    void load()
    return () => {
      active = false
    }
  }, [])

  const count = unlocked.length

  return (
    <div className="achievements-page">
      <div className="page-header">
        <h2>{t('achievements.title')}</h2>
        <button type="button" onClick={() => navigate('/progress')}>
          {t('achievements.back')}
        </button>
      </div>

      <p className="muted">
        {t('achievements.count', { n: count, total: ACHIEVEMENTS.length })}
      </p>

      <div className="achievements-grid">
        {ACHIEVEMENTS.map((a) => {
          const isUnlocked = unlocked.includes(a.id)
          return (
            <div
              key={a.id}
              className={'achievement-card' + (isUnlocked ? ' unlocked' : ' locked')}
            >
              <span className="achievement-icon">{isUnlocked ? a.icon : '🔒'}</span>
              <div className="achievement-body">
                <strong>{t(`achievements.items.${a.id}.title`)}</strong>
                <div className="muted">{t(`achievements.items.${a.id}.desc`)}</div>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
