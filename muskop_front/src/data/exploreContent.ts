import type {
  EventDTO,
  Finger,
  MeasureDTO,
  SectionDTO,
  TabDocument,
  Technique,
} from '../types/tab'

// ==========================================================================
// Contenido incluido con la app: ejercicios fingerstyle guiados (tablaturas
// reales, etiquetadas por habilidad y nivel) y artículos de teoría. Desde la
// página Explorar se añaden a la librería del usuario.
// ==========================================================================

type NoteSpec = [string: number, fret: number, finger?: Finger, technique?: Technique]

function ev(beat: number, duration: string, ...notes: NoteSpec[]): EventDTO {
  return {
    beat,
    duration,
    notes: notes.map(([string, fret, finger, technique]) => ({
      string,
      fret,
      ...(finger ? { finger } : {}),
      ...(technique ? { technique } : {}),
    })),
  }
}

function measure(...events: EventDTO[]): MeasureDTO {
  return { events }
}

function doc(title: string, baseBpm: number, sections: SectionDTO[]): TabDocument {
  return {
    version: 2,
    title,
    category: null,
    tuning: ['E', 'A', 'D', 'G', 'B', 'E'],
    timeSignature: '4/4',
    baseBpm,
    sections,
  }
}

export interface ExerciseItem {
  id: string
  kind: 'exercise'
  title: string
  skill: string
  level: number
  description: string
  doc: TabDocument
}

export interface TheoryItem {
  id: string
  kind: 'theory'
  title: string
  skill: string | null
  level: number | null
  description: string
  body: string
}

export type ExploreItem = ExerciseItem | TheoryItem

// --------------------------------------------------------------------------
// Ejercicios
// --------------------------------------------------------------------------

export const EXERCISES: ExerciseItem[] = [
  {
    id: 'pima-am-e',
    kind: 'exercise',
    title: 'p-i-m-a sobre Am y E',
    skill: 'arpegios',
    level: 1,
    description: 'El arpegio base: cada dedo pulsa siempre su cuerda. Primer ejercicio de mano derecha.',
    doc: doc('p-i-m-a sobre Am y E', 60, [
      {
        kind: 'fingerstyle',
        name: 'Am y E en negras',
        notes: 'p en el bajo, i=3ª, m=2ª, a=1ª. Mano quieta, solo se mueven los dedos.\nNo mires la mano derecha: memoriza la distancia.',
        measures: [
          measure(
            ev(1, 'quarter', [5, 0, 'p']),
            ev(2, 'quarter', [3, 2, 'i']),
            ev(3, 'quarter', [2, 1, 'm']),
            ev(4, 'quarter', [1, 0, 'a']),
          ),
          measure(
            ev(1, 'quarter', [6, 0, 'p']),
            ev(2, 'quarter', [3, 1, 'i']),
            ev(3, 'quarter', [2, 0, 'm']),
            ev(4, 'quarter', [1, 0, 'a']),
          ),
        ],
      },
    ]),
  },
  {
    id: 'pima-continuo-c-g',
    kind: 'exercise',
    title: 'p-i-m-a-m-i continuo sobre C y G',
    skill: 'arpegios',
    level: 2,
    description: 'Arpegio de ida y vuelta en corcheas, sin cortar el sonido al cambiar de acorde.',
    doc: doc('p-i-m-a-m-i continuo', 70, [
      {
        kind: 'fingerstyle',
        name: 'C y G en corcheas',
        notes: 'El pulso no se para en el cambio de acorde: prepara el acorde durante las dos últimas corcheas.',
        measures: [
          measure(
            ev(1, 'eighth', [5, 3, 'p']),
            ev(1.5, 'eighth', [3, 0, 'i']),
            ev(2, 'eighth', [2, 1, 'm']),
            ev(2.5, 'eighth', [1, 0, 'a']),
            ev(3, 'eighth', [2, 1, 'm']),
            ev(3.5, 'eighth', [3, 0, 'i']),
            ev(4, 'eighth', [2, 1, 'm']),
            ev(4.5, 'eighth', [1, 0, 'a']),
          ),
          measure(
            ev(1, 'eighth', [6, 3, 'p']),
            ev(1.5, 'eighth', [3, 0, 'i']),
            ev(2, 'eighth', [2, 0, 'm']),
            ev(2.5, 'eighth', [1, 3, 'a']),
            ev(3, 'eighth', [2, 0, 'm']),
            ev(3.5, 'eighth', [3, 0, 'i']),
            ev(4, 'eighth', [2, 0, 'm']),
            ev(4.5, 'eighth', [1, 3, 'a']),
          ),
        ],
      },
    ]),
  },
  {
    id: 'pulgar-alternado',
    kind: 'exercise',
    title: 'Pulgar alternado 6ª–4ª',
    skill: 'patrones',
    level: 1,
    description: 'La base del fingerstyle: el pulgar marca el pulso alternando dos cuerdas graves.',
    doc: doc('Pulgar alternado 6ª–4ª', 50, [
      {
        kind: 'fingerstyle',
        name: 'Solo pulgar',
        notes: 'El pulgar cae con el metrónomo, siempre igual de fuerte.\nApoya (apoyando en la siguiente cuerda) para un bajo sólido.',
        measures: [
          measure(
            ev(1, 'quarter', [6, 0, 'p']),
            ev(2, 'quarter', [4, 0, 'p']),
            ev(3, 'quarter', [6, 0, 'p']),
            ev(4, 'quarter', [4, 0, 'p']),
          ),
          measure(
            ev(1, 'quarter', [6, 0, 'p']),
            ev(2, 'quarter', [4, 0, 'p']),
            ev(3, 'quarter', [6, 0, 'p']),
            ev(4, 'quarter', [4, 0, 'p']),
          ),
        ],
      },
    ]),
  },
  {
    id: 'pulgar-im',
    kind: 'exercise',
    title: 'Pulgar 6-4-5-4 con i-m',
    skill: 'patrones',
    level: 2,
    description: 'El pulgar recorre tres bajos y los dedos i-m contestan en los contratiempos.',
    doc: doc('Pulgar 6-4-5-4 con i-m', 60, [
      {
        kind: 'fingerstyle',
        name: 'Bajos + contratiempos',
        notes: 'Primero solo el pulgar hasta que salga sin pensar; después añade i y m en el "y" de cada pulso.',
        measures: [
          measure(
            ev(1, 'eighth', [6, 0, 'p']),
            ev(1.5, 'eighth', [3, 0, 'i']),
            ev(2, 'eighth', [4, 0, 'p']),
            ev(2.5, 'eighth', [2, 0, 'm']),
            ev(3, 'eighth', [5, 0, 'p']),
            ev(3.5, 'eighth', [3, 0, 'i']),
            ev(4, 'eighth', [4, 0, 'p']),
            ev(4.5, 'eighth', [2, 0, 'm']),
          ),
        ],
      },
    ]),
  },
  {
    id: 'travis-c',
    kind: 'exercise',
    title: 'Patrón Travis básico sobre C',
    skill: 'patrones',
    level: 3,
    description: 'Bajo alternado del pulgar con i-m rellenando: el patrón de fingerpicking más usado.',
    doc: doc('Patrón Travis básico', 60, [
      {
        kind: 'fingerstyle',
        name: 'Travis sobre C',
        notes: 'El pulgar (5ª y 4ª) no puede fallar: es el metrónomo interno.\nAcento suave en los pulsos 1 y 3.',
        measures: [
          measure(
            ev(1, 'eighth', [5, 3, 'p']),
            ev(1.5, 'eighth', [3, 0, 'i']),
            ev(2, 'eighth', [4, 2, 'p']),
            ev(2.5, 'eighth', [2, 1, 'm']),
            ev(3, 'eighth', [5, 3, 'p']),
            ev(3.5, 'eighth', [3, 0, 'i']),
            ev(4, 'eighth', [4, 2, 'p']),
            ev(4.5, 'eighth', [2, 1, 'm']),
          ),
          measure(
            ev(1, 'eighth', [5, 3, 'p']),
            ev(1.5, 'eighth', [3, 0, 'i']),
            ev(2, 'eighth', [4, 2, 'p']),
            ev(2.5, 'eighth', [2, 1, 'm']),
            ev(3, 'eighth', [5, 3, 'p']),
            ev(3.5, 'eighth', [3, 0, 'i']),
            ev(4, 'half', [4, 2, 'p'], [2, 1, 'm'], [1, 0, 'a']),
          ),
        ],
      },
    ]),
  },
  {
    id: 'balada-am-c',
    kind: 'exercise',
    title: 'Patrón balada sobre Am y C',
    skill: 'patrones',
    level: 3,
    description: 'Patrón p-i-m-a-m-i-m-i en corcheas, el acompañamiento clásico de balada.',
    doc: doc('Patrón balada', 65, [
      {
        kind: 'fingerstyle',
        name: 'Am → C',
        notes: 'Deja sonar todas las cuerdas: el patrón debe sonar como un colchón continuo.',
        measures: [
          measure(
            ev(1, 'eighth', [5, 0, 'p']),
            ev(1.5, 'eighth', [3, 2, 'i']),
            ev(2, 'eighth', [2, 1, 'm']),
            ev(2.5, 'eighth', [1, 0, 'a']),
            ev(3, 'eighth', [2, 1, 'm']),
            ev(3.5, 'eighth', [3, 2, 'i']),
            ev(4, 'eighth', [2, 1, 'm']),
            ev(4.5, 'eighth', [3, 2, 'i']),
          ),
          measure(
            ev(1, 'eighth', [5, 3, 'p']),
            ev(1.5, 'eighth', [3, 0, 'i']),
            ev(2, 'eighth', [2, 1, 'm']),
            ev(2.5, 'eighth', [1, 0, 'a']),
            ev(3, 'eighth', [2, 1, 'm']),
            ev(3.5, 'eighth', [3, 0, 'i']),
            ev(4, 'eighth', [2, 1, 'm']),
            ev(4.5, 'eighth', [3, 0, 'i']),
          ),
        ],
      },
    ]),
  },
  {
    id: 'cambios-em-am',
    kind: 'exercise',
    title: 'Cambios Em ↔ Am',
    skill: 'cambios-acordes',
    level: 1,
    description: 'El primer cambio de acordes: dos formas vecinas, los dedos casi no se mueven.',
    doc: doc('Cambios Em ↔ Am', 60, [
      {
        kind: 'chords',
        name: 'Un compás por acorde',
        notes: 'Los dedos 2 y 3 mantienen su forma y solo saltan de cuerda.\nCambia en silencio primero; después con un rasgueo por compás.',
        chords: [
          { name: 'Em', frets: [0, 2, 2, 0, 0, 0], beats: 4 },
          { name: 'Am', frets: [null, 0, 2, 2, 1, 0], beats: 4 },
          { name: 'Em', frets: [0, 2, 2, 0, 0, 0], beats: 4 },
          { name: 'Am', frets: [null, 0, 2, 2, 1, 0], beats: 4 },
        ],
      },
    ]),
  },
  {
    id: 'cambios-c-g-d',
    kind: 'exercise',
    title: 'Cambios C ↔ G ↔ D',
    skill: 'cambios-acordes',
    level: 2,
    description: 'La progresión de acordes abiertos más común. Objetivo: cambiar sin parar el pulso.',
    doc: doc('Cambios C ↔ G ↔ D', 70, [
      {
        kind: 'chords',
        name: 'C – G – D – G',
        notes: 'Mueve primero el dedo que va más lejos.\nSi un cambio se atasca, practícalo aislado 10 veces.',
        chords: [
          { name: 'C', frets: [null, 3, 2, 0, 1, 0], beats: 4 },
          { name: 'G', frets: [3, 2, 0, 0, 0, 3], beats: 4 },
          { name: 'D', frets: [null, null, 0, 2, 3, 2], beats: 4 },
          { name: 'G', frets: [3, 2, 0, 0, 0, 3], beats: 4 },
        ],
      },
    ]),
  },
  {
    id: 'ligados-basicos',
    kind: 'exercise',
    title: 'Ligados: hammer-on y pull-off',
    skill: 'tecnica',
    level: 1,
    description: 'Primeros ligados sobre cuerdas al aire: el sonido lo produce la mano izquierda.',
    doc: doc('Ligados básicos', 60, [
      {
        kind: 'fingerstyle',
        name: 'h y p en 3ª y 2ª',
        notes: 'Hammer: golpea el traste con decisión, cerca del alambre.\nPull: tira ligeramente de la cuerda hacia abajo al soltar.',
        measures: [
          measure(
            ev(1, 'eighth', [3, 0, 'i']),
            ev(1.5, 'eighth', [3, 2, undefined, 'hammer']),
            ev(2, 'eighth', [3, 2, 'i']),
            ev(2.5, 'eighth', [3, 0, undefined, 'pull']),
            ev(3, 'eighth', [2, 0, 'm']),
            ev(3.5, 'eighth', [2, 2, undefined, 'hammer']),
            ev(4, 'eighth', [2, 2, 'm']),
            ev(4.5, 'eighth', [2, 0, undefined, 'pull']),
          ),
        ],
      },
    ]),
  },
  {
    id: 'cromatico-1234',
    kind: 'exercise',
    title: 'Cromático 1-2-3-4',
    skill: 'velocidad',
    level: 1,
    description: 'El ejercicio de digitación universal: un dedo por traste, subiendo de cuerda.',
    doc: doc('Cromático 1-2-3-4', 60, [
      {
        kind: 'tab',
        name: '6ª y 5ª cuerda',
        notes: 'Un dedo por traste (1=índice … 4=meñique).\nSube el BPM solo cuando todas las notas suenen limpias e iguales.',
        measures: [
          measure(
            ev(1, 'quarter', [6, 1]),
            ev(2, 'quarter', [6, 2]),
            ev(3, 'quarter', [6, 3]),
            ev(4, 'quarter', [6, 4]),
          ),
          measure(
            ev(1, 'quarter', [5, 1]),
            ev(2, 'quarter', [5, 2]),
            ev(3, 'quarter', [5, 3]),
            ev(4, 'quarter', [5, 4]),
          ),
        ],
      },
    ]),
  },
]

// --------------------------------------------------------------------------
// Teoría
// --------------------------------------------------------------------------

export const THEORY_ARTICLES: TheoryItem[] = [
  {
    id: 'teoria-leer-tab',
    kind: 'theory',
    title: 'Cómo leer una tablatura',
    skill: null,
    level: null,
    description: 'Qué significan las líneas, los números y los símbolos de las tablaturas de Muskop.',
    body: `# Cómo leer una tablatura

Cada línea horizontal es una **cuerda**: la de arriba es la 1ª (mi agudo, e) y la de abajo la 6ª (mi grave, E). El número indica el **traste** que pisas; el 0 es la cuerda al aire.

## El tiempo

- La regla de arriba marca los **pulsos** del compás (1 2 3 4).
- Debajo de cada compás verás la **figura** de cada nota: ♩ negra, ♪ corchea, 𝅘𝅥𝅯 semicorchea.
- El BPM dice cuántos pulsos caben en un minuto.

## Símbolos de técnica

- **h** hammer-on (ligado ascendente)
- **p** pull-off (ligado descendente)
- **/** slide (arrastre)
- **b** bend y **~** vibrato
- **x** palm mute (apagado) y **◇** armónico

## La mano derecha

Las letras de colores junto a las notas son los dedos de la mano derecha: **p** pulgar, **i** índice, **m** medio, **a** anular.`,
  },
  {
    id: 'teoria-pima',
    kind: 'theory',
    title: 'La mano derecha: PIMA',
    skill: 'arpegios',
    level: 1,
    description: 'La regla de oro de la digitación de mano derecha en fingerstyle.',
    body: `# La mano derecha: PIMA

En notación clásica cada dedo tiene una letra: **p** pulgar, **i** índice, **m** medio, **a** anular. El meñique no se usa (o se apoya).

## La regla de oro

- **p** toca las cuerdas graves: 6ª, 5ª y 4ª.
- **i** toca la 3ª cuerda.
- **m** toca la 2ª cuerda.
- **a** toca la 1ª cuerda.

Al principio, respétala siempre: tu mano aprenderá la distancia a cada cuerda y podrás tocar sin mirar.

## Cómo colocar la mano

- Muñeca relajada y ligeramente arqueada, como sujetando una naranja.
- El pulgar por delante de los dedos, nunca chocando con el índice.
- Los dedos pulsan hacia la palma; el pulgar hacia abajo/afuera.

## Errores típicos

- Mirar la mano derecha todo el rato: esconde la mano, confía en la regla.
- Tensar la muñeca al subir la velocidad: si notas tensión, baja el BPM.
- Que el pulgar suene igual que los dedos: el bajo debe llevar más peso.`,
  },
  {
    id: 'teoria-figuras',
    kind: 'theory',
    title: 'Figuras, pulso y BPM',
    skill: null,
    level: null,
    description: 'Redondas, negras y corcheas: cómo se reparte el tiempo dentro de un compás.',
    body: `# Figuras, pulso y BPM

Un compás de 4/4 tiene **4 pulsos**. El BPM (beats per minute) dice cuántos pulsos hay en un minuto: a 60 BPM cada pulso dura un segundo.

## Las figuras

- 𝅝 **Redonda**: dura los 4 pulsos.
- 𝅗𝅥 **Blanca**: 2 pulsos.
- ♩ **Negra**: 1 pulso.
- ♪ **Corchea**: medio pulso (dos por pulso: "1 y 2 y…").
- 𝅘𝅥𝅯 **Semicorchea**: un cuarto de pulso (cuatro por pulso).

## Cómo practicar el ritmo

- Cuenta en voz alta: "1 y 2 y 3 y 4 y" para corcheas.
- Usa el metrónomo de Muskop: el acento marca el pulso 1.
- Al reproducir una tablatura, el cursor te enseña qué nota cae en cada pulso: míralo antes de tocar.`,
  },
  {
    id: 'teoria-travis',
    kind: 'theory',
    title: 'El patrón Travis, explicado',
    skill: 'patrones',
    level: 3,
    description: 'Qué es el bajo alternado y por qué es la base de casi todo el fingerpicking.',
    body: `# El patrón Travis

El patrón Travis (por Merle Travis) es la base del fingerpicking: el **pulgar alterna dos bajos** en los pulsos, mientras i y m rellenan los contratiempos.

## Las dos capas

- **Capa 1 — el pulgar**: pulsos 1, 2, 3, 4. Alterna dos cuerdas graves del acorde. Es tu metrónomo interno: no puede fallar nunca.
- **Capa 2 — los dedos**: i y m tocan en los "y" (contratiempos), creando el vaivén característico.

## Cómo montarlo

- Practica el pulgar solo hasta que salga sin pensar.
- Añade **i** solo en el "y" del 1 y del 3.
- Añade **m** en el "y" del 2 y del 4.
- Junta todo muy despacio (50–60 BPM) antes de subir.

## Después

Cuando el patrón salga sobre un acorde, practica cambiarlo de acorde sin romper el bajo. Más adelante la melodía se cuela entre los huecos: eso ya es fingerstyle completo.`,
  },
]

export const EXPLORE_ITEMS: ExploreItem[] = [...EXERCISES, ...THEORY_ARTICLES]

// --------------------------------------------------------------------------
// Selección por idioma. El español es la base (estructura + texto); para
// inglés se sustituye solo el texto visible por los overrides de
// exploreContent.en.ts, sin tocar los datos musicales.
// --------------------------------------------------------------------------

import type { Lang } from '../i18n/types'
import { EN_EXERCISE_TEXT, EN_THEORY_TEXT } from './exploreContent.en'

function translateExercise(item: ExerciseItem): ExerciseItem {
  const en = EN_EXERCISE_TEXT[item.id]
  if (!en) return item
  const section = item.doc.sections[0]
  return {
    ...item,
    title: en.title,
    description: en.description,
    doc: {
      ...item.doc,
      title: en.title,
      sections: [{ ...section, name: en.sectionName, notes: en.sectionNotes }, ...item.doc.sections.slice(1)],
    },
  }
}

function translateTheory(item: TheoryItem): TheoryItem {
  const en = EN_THEORY_TEXT[item.id]
  if (!en) return item
  return { ...item, title: en.title, description: en.description, body: en.body }
}

export function getExploreItems(lang: Lang): ExploreItem[] {
  if (lang === 'es') return EXPLORE_ITEMS
  return EXPLORE_ITEMS.map((item) =>
    item.kind === 'exercise' ? translateExercise(item) : translateTheory(item),
  )
}
