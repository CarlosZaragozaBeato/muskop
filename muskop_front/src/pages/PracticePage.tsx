import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Metronome, playChime } from '../audio/player'
import ResourceView from '../components/ResourceView'
import * as sessions from '../storage/sessionManager'
import type { ResourceDetail } from '../types/tab'
import {
  COMPLETION_BONUS,
  GENERAL_SKILL,
  XP_PER_MINUTE,
  skillLabel,
  todayKey,
  type Routine,
} from '../types/routine'

function formatTime(totalSeconds: number): string {
  const m = Math.floor(totalSeconds / 60)
  const s = totalSeconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}

/**
 * Modo práctica: recorre los bloques de la rutina con temporizador,
 * metrónomo al BPM objetivo y el recurso asociado en pantalla.
 */
export default function PracticePage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [routine, setRoutine] = useState<Routine | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [blockIndex, setBlockIndex] = useState(0)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const [running, setRunning] = useState(true)
  const [finished, setFinished] = useState(false)
  const [metronomeOn, setMetronomeOn] = useState(false)
  const [beat, setBeat] = useState<number | null>(null)
  const [resource, setResource] = useState<ResourceDetail | null>(null)

  const metronome = useRef<Metronome | null>(null)
  useEffect(() => {
    metronome.current = new Metronome()
    return () => metronome.current?.stop()
  }, [])

  // seguimiento de la práctica real para el registro y la experiencia
  const blockSeconds = useRef(0)
  const skillSeconds = useRef<Record<string, number>>({})
  const saved = useRef(false)
  const [earnedXp, setEarnedXp] = useState<Record<string, number> | null>(null)

  const collectCurrentBlock = useCallback(() => {
    const current = routine?.blocks[blockIndex]
    if (!current || blockSeconds.current <= 0) return
    const skill = current.skill ?? GENERAL_SKILL
    skillSeconds.current[skill] = (skillSeconds.current[skill] ?? 0) + blockSeconds.current
    blockSeconds.current = 0
  }, [routine, blockIndex])

  const savePractice = useCallback(
    async (completed: boolean) => {
      if (!routine || saved.current) return
      const totalSeconds = Object.values(skillSeconds.current).reduce((a, b) => a + b, 0)
      if (totalSeconds < 30) return
      saved.current = true
      const bonus = completed ? COMPLETION_BONUS : 1
      const gains: Record<string, number> = {}
      for (const [skill, seconds] of Object.entries(skillSeconds.current)) {
        const xp = Math.round((seconds / 60) * XP_PER_MINUTE * bonus)
        if (xp > 0) gains[skill] = xp
      }
      setEarnedXp(gains)
      await sessions.recordPractice(
        {
          date: todayKey(),
          routineId: routine.id,
          minutes: Math.max(1, Math.round(totalSeconds / 60)),
          completed,
        },
        gains,
      )
    },
    [routine],
  )

  useEffect(() => {
    sessions
      .getRoutine(Number(id))
      .then(setRoutine)
      .catch((err) => setError(err instanceof Error ? err.message : 'Rutina no encontrada'))
  }, [id])

  const block = routine?.blocks[blockIndex] ?? null

  // al entrar en un bloque: reiniciar temporizador, metrónomo y recurso
  useEffect(() => {
    if (!block) return
    setSecondsLeft(block.minutes * 60)
    setRunning(true)
    metronome.current?.stop()
    setMetronomeOn(false)
    setBeat(null)
    setResource(null)
    if (block.resourceId !== null) {
      sessions
        .getResource(block.resourceId)
        .then(setResource)
        .catch(() => setResource(null))
    }
  }, [block])

  const goTo = useCallback(
    (index: number) => {
      if (!routine) return
      collectCurrentBlock()
      if (index >= routine.blocks.length) {
        metronome.current?.stop()
        setFinished(true)
        void savePractice(true)
        return
      }
      setFinished(false)
      setBlockIndex(Math.max(0, index))
    },
    [routine, collectCurrentBlock, savePractice],
  )

  // temporizador (cuenta atrás del bloque + acumulado de práctica real)
  useEffect(() => {
    if (!running || finished || !block) return
    const timer = setInterval(() => {
      blockSeconds.current += 1
      setSecondsLeft((prev) => {
        if (prev <= 1) {
          playChime()
          goTo(blockIndex + 1)
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => clearInterval(timer)
  }, [running, finished, block, blockIndex, goTo])

  const toggleMetronome = () => {
    if (!block?.bpm) return
    if (metronomeOn) {
      metronome.current?.stop()
      setMetronomeOn(false)
      setBeat(null)
    } else {
      metronome.current?.start(block.bpm, setBeat)
      setMetronomeOn(true)
    }
  }

  if (error) {
    return <p className="error">{error}</p>
  }
  if (!routine) {
    return <p className="muted">Cargando…</p>
  }

  if (finished) {
    return (
      <div className="practice-page practice-finished">
        <h2>🎉 ¡Rutina completada!</h2>
        <p className="muted">
          «{routine.name}» — {routine.blocks.length} bloques hechos.
        </p>
        {earnedXp && Object.keys(earnedXp).length > 0 && (
          <div className="xp-gains">
            {Object.entries(earnedXp).map(([skill, xp]) => (
              <span key={skill} className="xp-gain">
                +{xp} XP {skillLabel(skill)}
              </span>
            ))}
          </div>
        )}
        <div className="header-actions">
          <button
            type="button"
            className="primary"
            onClick={() => {
              saved.current = false
              skillSeconds.current = {}
              blockSeconds.current = 0
              setEarnedXp(null)
              setFinished(false)
              setBlockIndex(0)
            }}
          >
            Repetir rutina
          </button>
          <button type="button" onClick={() => navigate('/routines')}>← Rutinas</button>
          <button type="button" onClick={() => navigate('/progress')}>📈 Progreso</button>
        </div>
      </div>
    )
  }

  return (
    <div className="practice-page">
      <div className="page-header">
        <h2>
          {routine.name}
          <span className="muted practice-progress">
            {' '}· bloque {blockIndex + 1} de {routine.blocks.length}
          </span>
        </h2>
        <button
          type="button"
          onClick={async () => {
            metronome.current?.stop()
            collectCurrentBlock()
            await savePractice(false)
            navigate('/routines')
          }}
        >
          ✕ Salir
        </button>
      </div>

      <div className="practice-layout">
        <aside className="practice-blocks">
          {routine.blocks.map((b, i) => (
            <button
              key={b.id}
              type="button"
              className={
                'practice-block-item' +
                (i === blockIndex ? ' current' : i < blockIndex ? ' done' : '')
              }
              onClick={() => goTo(i)}
            >
              <span className="block-number">{i + 1}</span>
              <span className="practice-block-name">{b.name || 'Bloque'}</span>
              <span className="muted">{b.minutes}′</span>
            </button>
          ))}
        </aside>

        <main className="practice-main">
          <h3>{block?.name || 'Bloque'}</h3>
          {block?.notes && <p className="practice-notes">{block.notes}</p>}

          <div className={'practice-timer' + (secondsLeft <= 10 ? ' ending' : '')}>
            {formatTime(secondsLeft)}
          </div>

          <div className="practice-controls">
            <button type="button" onClick={() => setRunning((r) => !r)}>
              {running ? '⏸ Pausar' : '▶ Continuar'}
            </button>
            <button type="button" onClick={() => setSecondsLeft((block?.minutes ?? 5) * 60)}>
              ↺ Reiniciar
            </button>
            <button type="button" disabled={blockIndex === 0} onClick={() => goTo(blockIndex - 1)}>
              ← Anterior
            </button>
            <button type="button" onClick={() => goTo(blockIndex + 1)}>
              {blockIndex === routine.blocks.length - 1 ? 'Terminar ✓' : 'Siguiente →'}
            </button>
          </div>

          <div className="practice-bpm">
            {block?.bpm ? (
              <>
                <button
                  type="button"
                  className={metronomeOn ? 'active' : ''}
                  onClick={toggleMetronome}
                >
                  {metronomeOn ? '⏹ Metrónomo' : '▶ Metrónomo'} · {block.bpm} bpm
                </button>
                <span className="beat-dots">
                  {[0, 1, 2, 3].map((b) => (
                    <span key={b} className={'beat-dot' + (beat === b ? ' on' : '')} />
                  ))}
                </span>
              </>
            ) : (
              <span className="muted">Bloque a tempo libre</span>
            )}
          </div>

          {resource && (
            <div className="practice-resource">
              <ResourceView detail={resource} />
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
