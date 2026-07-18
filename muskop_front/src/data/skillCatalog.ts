// ==========================================================================
// Catálogo de ejercicios recomendados por habilidad y nivel. Es contenido
// orientativo para la página de progreso: qué practicar en cada nivel.
// ==========================================================================

export interface SkillLevelContent {
  level: number
  items: string[]
}

export const SKILL_CATALOG: Record<string, SkillLevelContent[]> = {
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

export function catalogFor(skillId: string, level: number): SkillLevelContent | null {
  const entries = SKILL_CATALOG[skillId]
  if (!entries) return null
  // si el nivel supera el catálogo, se queda con el último disponible
  return entries.find((e) => e.level === level) ?? entries[entries.length - 1] ?? null
}
