import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ImportRoutineDialog from '../components/ImportRoutineDialog'
import * as sessions from '../storage/sessionManager'
import type { ResourceSummary } from '../types/tab'
import { routineMinutes, type PracticeEntry, type Routine } from '../types/routine'
import { exportRoutineToJson } from '../utils/routineIO'
import { saveText } from '../native/share'
import { useI18n } from '../i18n/I18nContext'
import {
  dailyAverage,
  practiceStreak,
  routineCompletionStreak,
  timesCompleted,
  totalMinutes,
} from '../utils/stats'

export default function RoutinesPage() {
  const navigate = useNavigate()
  const { t } = useI18n()
  const [routines, setRoutines] = useState<Routine[] | null>(null)
  const [log, setLog] = useState<PracticeEntry[]>([])
  const [resources, setResources] = useState<ResourceSummary[]>([])
  const [categoryFilter, setCategoryFilter] = useState('')
  const [importing, setImporting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const reload = useCallback(() => {
    sessions
      .listRoutines()
      .then((list) => setRoutines([...list]))
      .catch((err) => setError(err instanceof Error ? err.message : t('routines.errorLoad')))
    try {
      setLog([...sessions.getPracticeLog()])
    } catch {
      setLog([])
    }
    sessions.listResources().then(setResources).catch(() => setResources([]))
  }, [t])

  useEffect(reload, [reload])

  const categories = useMemo(
    () => [...new Set((routines ?? []).map((r) => r.category).filter((c): c is string => !!c))],
    [routines],
  )

  const filtered = useMemo(
    () => (routines ?? []).filter((r) => !categoryFilter || r.category === categoryFilter),
    [routines, categoryFilter],
  )

  const streak = practiceStreak(log)
  const average = dailyAverage(log, 7)
  const total = totalMinutes(log)

  const remove = async (routine: Routine) => {
    if (!window.confirm(t('routines.deleteConfirm', { name: routine.name }))) return
    await sessions.deleteRoutine(routine.id)
    reload()
  }

  const exportRoutine = (routine: Routine) => {
    const json = exportRoutineToJson(routine, resources)
    const safe = routine.name.replace(/[^\p{L}\p{N}\-_ ]/gu, '').trim().replace(/\s+/g, '-')
    return saveText(`${safe || 'routine'}.rutina.json`, json)
  }

  return (
    <div className="routines-page">
      <div className="page-header">
        <h2>{t('routines.title')}</h2>
        <div className="header-actions">
          <button type="button" onClick={() => setImporting(true)}>{t('routines.import')}</button>
          <Link className="button primary" to="/routines/new">{t('routines.newRoutine')}</Link>
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {notice && <p className="success">{notice}</p>}

      {log.length > 0 && (
        <div className="stats-panel">
          <div className="stat">
            <span className="stat-value">🔥 {streak}</span>
            <span className="stat-label">{t('routines.streakLabel')}</span>
          </div>
          <div className="stat">
            <span className="stat-value">{average} min</span>
            <span className="stat-label">{t('routines.avgLabel')}</span>
          </div>
          <div className="stat">
            <span className="stat-value">{total} min</span>
            <span className="stat-label">{t('routines.totalLabel')}</span>
          </div>
          <Link className="button" to="/progress">{t('routines.viewProgress')}</Link>
        </div>
      )}

      {categories.length > 0 && (
        <div className="library-filters">
          <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
            <option value="">{t('routines.allCategories')}</option>
            {categories.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
      )}

      {routines !== null && routines.length === 0 && (
        <p className="muted">{t('routines.empty')}</p>
      )}

      <ul className="library-list">
        {filtered.map((routine) => {
          const completionStreak = routineCompletionStreak(log, routine.id)
          const completions = timesCompleted(log, routine.id)
          return (
            <li key={routine.id} className="library-row">
              <span className="badge badge-routine">{t('routines.badge')}</span>
              <div className="routine-info">
                <span className="library-item-title">
                  {routine.name}
                  {routine.category && <span className="chip">{routine.category}</span>}
                </span>
                <span className="muted">
                  {t(routine.blocks.length === 1 ? 'routines.blockOne' : 'routines.blockMany', { n: routine.blocks.length })}
                  {' · '}
                  {t('routines.minutes', { n: routineMinutes(routine) })}
                  {completions > 0 && ` · ${t(completions === 1 ? 'routines.completedTimesOne' : 'routines.completedTimesMany', { n: completions })}`}
                  {completionStreak > 0 && ` · ${t('routines.streakDays', { n: completionStreak })}`}
                </span>
              </div>
              <span className="row-actions">
                <button
                  type="button"
                  className="primary"
                  disabled={routine.blocks.length === 0}
                  onClick={() => navigate(`/routines/${routine.id}/practice`)}
                >
                  {t('routines.practice')}
                </button>
                <button type="button" onClick={() => navigate(`/routines/${routine.id}`)}>
                  {t('common.edit')}
                </button>
                <button type="button" title={t('routines.exportTitle')} onClick={() => exportRoutine(routine)}>
                  {t('routines.exportJson')}
                </button>
                <button type="button" onClick={() => remove(routine)}>{t('common.delete')}</button>
              </span>
            </li>
          )
        })}
      </ul>

      {importing && (
        <ImportRoutineDialog
          resources={resources}
          onClose={() => setImporting(false)}
          onImport={async (routine, warnings) => {
            const id = await sessions.createRoutine(routine)
            setImporting(false)
            setNotice(
              t('routines.imported', { name: routine.name, id }) +
                (warnings.length > 0 ? ` — ${warnings.join(' · ')}` : ''),
            )
            setTimeout(() => setNotice(null), 6000)
            reload()
          }}
        />
      )}
    </div>
  )
}
