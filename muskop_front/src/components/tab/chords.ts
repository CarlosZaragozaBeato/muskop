import type { ChordShape } from '../../types/tab'

// Librería de acordes comunes. frets: de grave a agudo, null = muteada.
function chord(name: string, frets: (number | null)[], baseFret = 1): ChordShape {
  return { name, frets, baseFret }
}

export const COMMON_CHORDS: ChordShape[] = [
  chord('C', [null, 3, 2, 0, 1, 0]),
  chord('Cmaj7', [null, 3, 2, 0, 0, 0]),
  chord('D', [null, null, 0, 2, 3, 2]),
  chord('Dm', [null, null, 0, 2, 3, 1]),
  chord('D7', [null, null, 0, 2, 1, 2]),
  chord('E', [0, 2, 2, 1, 0, 0]),
  chord('Em', [0, 2, 2, 0, 0, 0]),
  chord('E7', [0, 2, 0, 1, 0, 0]),
  chord('F', [1, 3, 3, 2, 1, 1]),
  chord('Fmaj7', [null, null, 3, 2, 1, 0]),
  chord('G', [3, 2, 0, 0, 0, 3]),
  chord('G7', [3, 2, 0, 0, 0, 1]),
  chord('A', [null, 0, 2, 2, 2, 0]),
  chord('Am', [null, 0, 2, 2, 1, 0]),
  chord('A7', [null, 0, 2, 0, 2, 0]),
  chord('Am7', [null, 0, 2, 0, 1, 0]),
  chord('B7', [null, 2, 1, 2, 0, 2]),
  chord('Bm', [null, 2, 4, 4, 3, 2], 1),
  chord('B', [null, 2, 4, 4, 4, 2], 1),
  chord('C#m', [null, 4, 6, 6, 5, 4], 1),
  chord('F#m', [2, 4, 4, 2, 2, 2], 1),
  chord('Asus2', [null, 0, 2, 2, 0, 0]),
  chord('Dsus4', [null, null, 0, 2, 3, 3]),
  chord('Cadd9', [null, 3, 2, 0, 3, 0]),
]

export function emptyChord(): ChordShape {
  return { name: '', frets: [null, null, null, null, null, null], baseFret: 1 }
}
