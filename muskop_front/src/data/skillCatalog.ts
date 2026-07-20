import type { Lang } from '../i18n/types'

// ==========================================================================
// Catálogo de ejercicios recomendados por habilidad y nivel. Es contenido
// orientativo para la página de progreso: qué practicar en cada nivel.
// Disponible en español (base) e inglés.
// ==========================================================================

export interface SkillLevelContent {
  level: number
  items: string[]
}

const SKILL_CATALOG_ES: Record<string, SkillLevelContent[]> = {
  'cambios-acordes': [
    { level: 1, items: ['Em ↔ Am', 'Em ↔ D', 'Am ↔ Dm', 'Cambia sin mirar la mano izquierda'] },
    { level: 2, items: ['C ↔ G', 'G ↔ D', 'Am ↔ C', 'Un rasgueo por acorde, sin parar el pulso'] },
    { level: 3, items: ['C ↔ Fmaj7 (mini cejilla)', 'G ↔ B7', 'D ↔ A', 'Cambios a 60 bpm en negras'] },
    { level: 4, items: ['F (cejilla) ↔ C', 'Bm ↔ G', 'Progresión Am–F–C–G completa', 'Cambios en corcheas'] },
    { level: 5, items: ['Bm ↔ F#m (cejillas)', 'I–V–vi–IV en 4 tonalidades', 'Cambios con bajo alternado'] },
  ],
  arpegios: [
    { level: 1, items: ['p-i-m-a sobre Am y E al aire', 'Cada dedo su cuerda: p=6ª, i=3ª, m=2ª, a=1ª'] },
    { level: 2, items: ['p-i-m-a-m-i continuo', 'Arpegio sobre C ↔ G sin cortar', 'A 60 bpm en corcheas'] },
    { level: 3, items: ['Patrón Travis básico (bajo alternado)', 'Arpegio con melodía en la 1ª cuerda'] },
    { level: 4, items: ['Travis con bajos alternos + melodía simultánea', 'Arpegios sobre progresión completa'] },
    { level: 5, items: ['Arpegios con desplazamientos de posición', 'Armónicos dentro del arpegio', 'Rubato y dinámicas'] },
  ],
  patrones: [
    { level: 1, items: ['Pulgar alternado 6ª–4ª', 'Metrónomo a 50 bpm, sin fallos 4 compases'] },
    { level: 2, items: ['Pulgar 6-4-5-4 con i-m intercalados', 'Mismo patrón sobre dos acordes'] },
    { level: 3, items: ['Patrón de vals en 3/4', 'Patrón balada (p-i-m-a-m-i) sobre progresión'] },
    { level: 4, items: ['Patrón balada completo con cambios en corchea', 'Acentos desplazados'] },
    { level: 5, items: ['Patrones con síncopa', 'Golpe percusivo integrado', 'Combinar dos patrones en una pieza'] },
  ],
  velocidad: [
    { level: 1, items: ['Cromático 1-2-3-4 a 60 bpm en negras', 'Púa/dedos alternados estrictos'] },
    { level: 2, items: ['Cromático a 80 bpm en corcheas', 'Digitación 1-3-2-4'] },
    { level: 3, items: ['Cromático a 100 bpm', 'Escala mayor en una octava a 80 bpm en corcheas'] },
    { level: 4, items: ['Escala mayor 2 octavas a 100 bpm', 'Semicorcheas a 70 bpm limpias'] },
    { level: 5, items: ['Semicorcheas a 90+ bpm', 'Ráfagas con dinámica y acentos'] },
  ],
  tecnica: [
    { level: 1, items: ['Hammer-on y pull-off sobre cuerdas al aire', 'Notas limpias sin trastear'] },
    { level: 2, items: ['Ligados en el patrón 5-7 de todas las cuerdas', 'Slides afinados entre posiciones'] },
    { level: 3, items: ['Palm mute constante en bajos', 'Armónicos naturales en 12/7/5'] },
    { level: 4, items: ['Bends afinados de tono y semitono', 'Vibrato controlado'] },
    { level: 5, items: ['Combinar ligados+slides en frases', 'Armónicos artificiales'] },
  ],
}

const SKILL_CATALOG_EN: Record<string, SkillLevelContent[]> = {
  'cambios-acordes': [
    { level: 1, items: ['Em ↔ Am', 'Em ↔ D', 'Am ↔ Dm', 'Change without looking at your left hand'] },
    { level: 2, items: ['C ↔ G', 'G ↔ D', 'Am ↔ C', 'One strum per chord, without stopping the pulse'] },
    { level: 3, items: ['C ↔ Fmaj7 (mini barre)', 'G ↔ B7', 'D ↔ A', 'Changes at 60 bpm in quarter notes'] },
    { level: 4, items: ['F (barre) ↔ C', 'Bm ↔ G', 'Full Am–F–C–G progression', 'Changes in eighth notes'] },
    { level: 5, items: ['Bm ↔ F#m (barre chords)', 'I–V–vi–IV in 4 keys', 'Changes with alternating bass'] },
  ],
  arpegios: [
    { level: 1, items: ['p-i-m-a over open Am and E', 'Each finger its string: p=6th, i=3rd, m=2nd, a=1st'] },
    { level: 2, items: ['Continuous p-i-m-a-m-i', 'Arpeggio over C ↔ G without cutting', 'At 60 bpm in eighth notes'] },
    { level: 3, items: ['Basic Travis pattern (alternating bass)', 'Arpeggio with melody on the 1st string'] },
    { level: 4, items: ['Travis with alternating basses + simultaneous melody', 'Arpeggios over a full progression'] },
    { level: 5, items: ['Arpeggios with position shifts', 'Harmonics within the arpeggio', 'Rubato and dynamics'] },
  ],
  patrones: [
    { level: 1, items: ['Alternating thumb 6th–4th', 'Metronome at 50 bpm, 4 measures with no mistakes'] },
    { level: 2, items: ['Thumb 6-4-5-4 with i-m interleaved', 'Same pattern over two chords'] },
    { level: 3, items: ['Waltz pattern in 3/4', 'Ballad pattern (p-i-m-a-m-i) over a progression'] },
    { level: 4, items: ['Full ballad pattern with eighth-note changes', 'Displaced accents'] },
    { level: 5, items: ['Patterns with syncopation', 'Integrated percussive tap', 'Combine two patterns in one piece'] },
  ],
  velocidad: [
    { level: 1, items: ['Chromatic 1-2-3-4 at 60 bpm in quarter notes', 'Strict alternate pick/finger'] },
    { level: 2, items: ['Chromatic at 80 bpm in eighth notes', 'Fingering 1-3-2-4'] },
    { level: 3, items: ['Chromatic at 100 bpm', 'One-octave major scale at 80 bpm in eighth notes'] },
    { level: 4, items: ['Two-octave major scale at 100 bpm', 'Clean sixteenth notes at 70 bpm'] },
    { level: 5, items: ['Sixteenth notes at 90+ bpm', 'Bursts with dynamics and accents'] },
  ],
  tecnica: [
    { level: 1, items: ['Hammer-on and pull-off on open strings', 'Clean notes without fret buzz'] },
    { level: 2, items: ['Slurs in the 5-7 pattern on every string', 'In-tune slides between positions'] },
    { level: 3, items: ['Constant palm mute on the basses', 'Natural harmonics at 12/7/5'] },
    { level: 4, items: ['In-tune whole- and half-step bends', 'Controlled vibrato'] },
    { level: 5, items: ['Combine slurs+slides in phrases', 'Artificial harmonics'] },
  ],
}

const CATALOGS: Record<Lang, Record<string, SkillLevelContent[]>> = {
  es: SKILL_CATALOG_ES,
  en: SKILL_CATALOG_EN,
}

export function catalogFor(skillId: string, level: number, lang: Lang): SkillLevelContent | null {
  const entries = CATALOGS[lang][skillId] ?? CATALOGS.en[skillId]
  if (!entries) return null
  // si el nivel supera el catálogo, se queda con el último disponible
  return entries.find((e) => e.level === level) ?? entries[entries.length - 1] ?? null
}
