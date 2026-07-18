import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import ImportRoutineDialog from '../components/ImportRoutineDialog'
import * as sessions from '../storage/sessionManager'
import type { ResourceSummary } from '../types/tab'
import { routineMinutes, type PracticeEntry, type Routine } from '../types/routine'
import { exportRoutineToJson } from '../utils/routineIO'
import {
  dailyAverage,
  practiceStreak,
  routineCompletionStreak,
  timesCompleted,
  totalMinutes,
} from '../utils/stats'

export default function RoutinesPage() {
  const navigate = useNavigate()
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
      .catch((err) => setError(err instanceof Error ? err.message : 'Error cargando rutinas'))
    try {
      setLog([...sessions.getPracticeLog()])
    } catch {
      setLog([])
    }
    sessions.listResources().then(setResources).catch(() => setResources([]))
  }, [])

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
    if (!window.confirm(`¿Eliminar la rutina «${routine.name}»?`)) return
    await sessions.deleteRoutine(routine.id)
    reload()
  }

  const exportRoutine = (routine: Routine) => {
    const json = exportRoutineToJson(routine, resources)
    const blob = new Blob([json], { type: 'application/json;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${routine.name.replace(/[^\p{L}\p{N}\-_ ]/gu, '').trim().replace(/\s+/g, '-')}.rutina.json`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="routines-page">
      <div className="page-header">
        <h2>Mis rutinas</h2>
        <div className="header-actions">
          <button type="button" onClick={() => setImporting(true)}>⇪ Importar</button>
          <Link className="button primary" to="/routines/new">✚ Nueva rutina</Link>
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {notice && <p className="success">{notice}</p>}

      {log.length > 0 && (
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
          <Link className="button" to="/progress">📈 Ver progreso</Link>
        </div>
      )}

      {categories.length > 0 && (
        <div className="library-filters">
          <select value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)}>
            <option value="">Todas las categorías</option>
            {categories.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>
      )}

      {routines !== null && routines.length === 0 && (
        <p className="muted">
          Aún no tienes rutinas. Una rutina es una lista de bloques de práctica
          (duración, BPM objetivo y un recurso de tu librería) que puedes
          ejecutar con temporizador y metrónomo — o impórtala desde un JSON.
        </p>
      )}

      <ul className="library-list">
        {filtered.map((routine) => {
          const completionStreak = routineCompletionStreak(log, routine.id)
          const completions = timesCompleted(log, routine.id)
          return (
            <li key={routine.id} className="library-row">
              <span className="badge badge-routine">Rutina</span>
              <div className="routine-info">
                <span className="library-item-title">
                  {routine.name}
                  {routine.category && <span className="chip">{routine.category}</span>}
                </span>
                <span className="muted">
                  {routine.blocks.length} bloque{routine.blocks.length === 1 ? '' : 's'} ·{' '}
                  {routineMinutes(routine)} min
                  {completions > 0 && ` · completada ${completions} ${completions === 1 ? 'vez' : 'veces'}`}
                  {completionStreak > 0 && ` · 🔥 ${completionStreak} días seguidos`}
                </span>
              </div>
              <span className="row-actions">
                <button
                  type="button"
                  className="primary"
                  disabled={routine.blocks.length === 0}
                  onClick={() => navigate(`/routines/${routine.id}/practice`)}
                >
                  ▶ Practicar
                </button>
                <button type="button" onClick={() => navigate(`/routines/${routine.id}`)}>
                  Editar
                </button>
                <button type="button" title="Exportar como JSON" onClick={() => exportRoutine(routine)}>
                  ⤓ JSON
                </button>
                <button type="button" onClick={() => remove(routine)}>Eliminar</button>
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
              `Rutina «${routine.name}» importada (#${id})` +
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
