import {
  BEATS_PER_MEASURE,
  SLOTS_PER_BEAT,
  SLOTS_PER_MEASURE,
  STRING_COUNT,
  type EditorDocument,
  type Technique,
} from '../components/tab/tabModel'

// ==========================================================================
// Reproducción sencilla con Web Audio: metrónomo + "plucks" sintetizados.
// Cada sección usa su BPM propio (o el base del documento), de modo que se
// oye exactamente qué notas caen en cada pulso a ese tempo. La digitación
// fingerstyle influye en el sonido: el pulgar acentúa, el palm mute apaga y
// los armónicos suenan más puros.
// ==========================================================================

export interface PlaybackPosition {
  sectionIndex: number
  measureIndex?: number
  slot?: number
  chordIndex?: number
}

interface TimelinePluck {
  midi: number
  accent: boolean
  technique: Technique | null
}

interface TimelineNote {
  time: number
  plucks: TimelinePluck[]
  strum: boolean
  position: PlaybackPosition
}

interface TimelineClick {
  time: number
  accent: boolean
}

const NOTE_SEMITONES: Record<string, number> = {
  C: 0, 'C#': 1, Db: 1, D: 2, 'D#': 3, Eb: 3, E: 4, F: 5, 'F#': 6,
  Gb: 6, G: 7, 'G#': 8, Ab: 8, A: 9, 'A#': 10, Bb: 10, B: 11,
}

/** Octavas por defecto de cada cuerda (de grave a agudo) en afinación estándar. */
const DEFAULT_OCTAVES = [2, 2, 3, 3, 3, 4]

function tuningToMidi(tuning: string[]): number[] {
  return tuning.map((note, i) => {
    const semitone = NOTE_SEMITONES[note.trim()] ?? 4
    return 12 * (DEFAULT_OCTAVES[i] + 1) + semitone
  })
}

function midiToFreq(midi: number): number {
  return 440 * Math.pow(2, (midi - 69) / 12)
}

function buildTimeline(doc: EditorDocument) {
  // midi por índice de cuerda del editor (0 = mi agudo)
  const tuningMidi = tuningToMidi(doc.tuning)
  const midiForEditorString = (s: number) => tuningMidi[STRING_COUNT - 1 - s]

  const notes: TimelineNote[] = []
  const clicks: TimelineClick[] = []
  let t = 0

  doc.sections.forEach((section, sectionIndex) => {
    const bpm = section.bpm ?? doc.baseBpm
    const beatDur = 60 / bpm

    if (section.kind === 'chords') {
      section.chords.forEach((chord, chordIndex) => {
        const beats = chord.beats ?? BEATS_PER_MEASURE
        const plucks: TimelinePluck[] = []
        chord.frets.forEach((fret, i) => {
          if (fret !== null) {
            plucks.push({ midi: tuningMidi[i] + fret, accent: false, technique: null })
          }
        })
        if (plucks.length > 0) {
          notes.push({ time: t, plucks, strum: true, position: { sectionIndex, chordIndex } })
        }
        for (let b = 0; b < beats; b++) {
          clicks.push({ time: t + b * beatDur, accent: b === 0 })
        }
        t += beats * beatDur
      })
      return
    }

    const slotDur = beatDur / SLOTS_PER_BEAT
    section.measures.forEach((measure, measureIndex) => {
      for (let b = 0; b < BEATS_PER_MEASURE; b++) {
        clicks.push({ time: t + b * beatDur, accent: b === 0 })
      }
      for (let slot = 0; slot < SLOTS_PER_MEASURE; slot++) {
        const plucks: TimelinePluck[] = []
        for (let s = 0; s < STRING_COUNT; s++) {
          const cell = measure.cells[s][slot]
          if (cell !== null) {
            plucks.push({
              midi: midiForEditorString(s) + cell.fret,
              accent: cell.finger === 'p',
              technique: cell.technique,
            })
          }
        }
        if (plucks.length > 0) {
          notes.push({
            time: t + slot * slotDur,
            plucks,
            strum: plucks.length > 2,
            position: { sectionIndex, measureIndex, slot },
          })
        }
      }
      t += BEATS_PER_MEASURE * beatDur
    })
  })

  return { notes, clicks, total: t }
}

export class TabPlayer {
  private ctx: AudioContext | null = null
  private raf = 0
  private stopped = true

  get playing(): boolean {
    return !this.stopped
  }

  play(
    doc: EditorDocument,
    opts: {
      metronome?: boolean
      onTick?: (pos: PlaybackPosition | null) => void
      onEnd?: () => void
    } = {},
  ) {
    this.stop()
    const { notes, clicks, total } = buildTimeline(doc)
    if (total <= 0) {
      opts.onEnd?.()
      return
    }

    const ctx = new AudioContext()
    this.ctx = ctx
    this.stopped = false
    const start = ctx.currentTime + 0.15

    const master = ctx.createGain()
    master.gain.value = 0.5
    master.connect(ctx.destination)

    for (const note of notes) {
      note.plucks.forEach((pluck, i) => {
        const when = start + note.time + (note.strum ? i * 0.015 : 0)
        this.pluck(ctx, master, midiToFreq(pluck.midi), when, pluck)
      })
    }

    if (opts.metronome) {
      for (const click of clicks) {
        this.click(ctx, master, start + click.time, click.accent)
      }
    }

    let nextIdx = 0
    const tick = () => {
      if (this.stopped) return
      const elapsed = ctx.currentTime - start
      while (nextIdx < notes.length && notes[nextIdx].time <= elapsed) {
        opts.onTick?.(notes[nextIdx].position)
        nextIdx++
      }
      if (elapsed >= total + 0.4) {
        this.stop()
        opts.onTick?.(null)
        opts.onEnd?.()
        return
      }
      this.raf = requestAnimationFrame(tick)
    }
    this.raf = requestAnimationFrame(tick)
  }

  stop() {
    this.stopped = true
    cancelAnimationFrame(this.raf)
    if (this.ctx) {
      this.ctx.close().catch(() => {})
      this.ctx = null
    }
  }

  private pluck(
    ctx: AudioContext,
    out: AudioNode,
    freq: number,
    when: number,
    opts: { accent?: boolean; technique?: Technique | null } = {},
  ) {
    const muted = opts.technique === 'palmMute'
    const harmonic = opts.technique === 'harmonic'
    const level = opts.accent ? 0.5 : 0.35
    const release = muted ? 0.22 : harmonic ? 1.6 : 1.1

    const osc = ctx.createOscillator()
    osc.type = harmonic ? 'sine' : 'triangle'
    osc.frequency.value = freq
    if (opts.technique === 'vibrato') {
      const lfo = ctx.createOscillator()
      lfo.frequency.value = 5.5
      const lfoGain = ctx.createGain()
      lfoGain.gain.value = freq * 0.012
      lfo.connect(lfoGain)
      lfoGain.connect(osc.frequency)
      lfo.start(when)
      lfo.stop(when + release + 0.1)
    }

    const gain = ctx.createGain()
    gain.gain.setValueAtTime(0, when)
    gain.gain.linearRampToValueAtTime(level, when + 0.008)
    gain.gain.exponentialRampToValueAtTime(0.001, when + release)

    const filter = ctx.createBiquadFilter()
    filter.type = 'lowpass'
    filter.frequency.value = muted ? Math.min(freq * 2.5, 1800) : Math.min(freq * 6, 5000)

    osc.connect(filter)
    filter.connect(gain)
    gain.connect(out)
    osc.start(when)
    osc.stop(when + release + 0.1)
  }

  private click(ctx: AudioContext, out: AudioNode, when: number, accent: boolean) {
    const osc = ctx.createOscillator()
    osc.type = 'square'
    osc.frequency.value = accent ? 1800 : 1200

    const gain = ctx.createGain()
    gain.gain.setValueAtTime(accent ? 0.25 : 0.15, when)
    gain.gain.exponentialRampToValueAtTime(0.001, when + 0.05)

    osc.connect(gain)
    gain.connect(out)
    osc.start(when)
    osc.stop(when + 0.06)
  }
}

// ==========================================================================
// Metrónomo independiente para los bloques de práctica: marca el BPM
// objetivo en compás de 4, con acento en el primer pulso.
// ==========================================================================

export class Metronome {
  private ctx: AudioContext | null = null
  private timer: ReturnType<typeof setInterval> | null = null
  private nextBeat = 0
  private beatCount = 0

  get running(): boolean {
    return this.ctx !== null
  }

  start(bpm: number, onBeat?: (beat: number) => void) {
    this.stop()
    const ctx = new AudioContext()
    this.ctx = ctx
    const beatDur = 60 / bpm
    this.nextBeat = ctx.currentTime + 0.1
    this.beatCount = 0

    // planificación con antelación para que el tempo sea estable
    const schedule = () => {
      if (!this.ctx) return
      while (this.nextBeat < ctx.currentTime + 0.2) {
        const accent = this.beatCount % 4 === 0
        this.clickAt(ctx, this.nextBeat, accent)
        const beat = this.beatCount % 4
        if (onBeat) {
          const delay = Math.max(0, (this.nextBeat - ctx.currentTime) * 1000)
          setTimeout(() => onBeat(beat), delay)
        }
        this.nextBeat += beatDur
        this.beatCount += 1
      }
    }
    schedule()
    this.timer = setInterval(schedule, 100)
  }

  stop() {
    if (this.timer !== null) {
      clearInterval(this.timer)
      this.timer = null
    }
    if (this.ctx) {
      this.ctx.close().catch(() => {})
      this.ctx = null
    }
  }

  private clickAt(ctx: AudioContext, when: number, accent: boolean) {
    const osc = ctx.createOscillator()
    osc.type = 'square'
    osc.frequency.value = accent ? 1800 : 1200
    const gain = ctx.createGain()
    gain.gain.setValueAtTime(accent ? 0.3 : 0.18, when)
    gain.gain.exponentialRampToValueAtTime(0.001, when + 0.05)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start(when)
    osc.stop(when + 0.06)
  }
}

/** Aviso sonoro al terminar un bloque de práctica. */
export function playChime() {
  const ctx = new AudioContext()
  const notes = [523.25, 659.25, 783.99] // C5 E5 G5
  notes.forEach((freq, i) => {
    const osc = ctx.createOscillator()
    osc.type = 'sine'
    osc.frequency.value = freq
    const gain = ctx.createGain()
    const when = ctx.currentTime + i * 0.12
    gain.gain.setValueAtTime(0.001, when)
    gain.gain.linearRampToValueAtTime(0.25, when + 0.02)
    gain.gain.exponentialRampToValueAtTime(0.001, when + 0.8)
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.start(when)
    osc.stop(when + 0.9)
  })
  setTimeout(() => ctx.close().catch(() => {}), 2000)
}
