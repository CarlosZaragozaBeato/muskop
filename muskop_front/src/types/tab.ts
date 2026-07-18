// ==========================================================================
// Formato de documento de tablatura de Muskop (v2)
//
// Este es el formato JSON que se guarda en `content` del recurso y el que
// acepta la importación (por ejemplo, cuando un agente genera una tablatura).
// Convenciones:
//   - tuning: de grave a agudo, p. ej. ["E","A","D","G","B","E"]
//   - string: 1 = mi agudo (e) ... 6 = mi grave (E)
//   - beat: pulso dentro del compás empezando en 1 (1, 1.5, 2.25...)
//   - frets de un acorde: 6 valores de grave a agudo; null = cuerda muteada (x)
// ==========================================================================

export type Duration = 'whole' | 'half' | 'quarter' | 'eighth' | 'sixteenth'

/** Dedo de la mano derecha (notación clásica): pulgar, índice, medio, anular */
export type Finger = 'p' | 'i' | 'm' | 'a'

export type Technique =
  | 'hammer'    // ligado ascendente (h)
  | 'pull'      // ligado descendente (p)
  | 'slide'     // arrastre (/)
  | 'bend'      // bend (b)
  | 'vibrato'   // vibrato (~)
  | 'palmMute'  // apagado con la palma (x)
  | 'harmonic'  // armónico natural (◇)

export interface NoteDTO {
  string: number
  fret: number
  /** Dedo de la mano derecha que pulsa la nota (fingerstyle) */
  finger?: Finger | null
  /** Técnica aplicada a la nota */
  technique?: Technique | null
}

export interface EventDTO {
  beat: number
  duration: Duration | string
  notes: NoteDTO[]
}

export interface MeasureDTO {
  index?: number
  events: EventDTO[]
}

export interface ChordShape {
  name: string
  /** 6 valores de grave a agudo; null = muteada, 0 = al aire */
  frets: (number | null)[]
  /** Traste base del diagrama (1 = cejilla en el primer traste del dibujo) */
  baseFret?: number
  /** Pulsos que dura el acorde al reproducir la sección (por defecto 4) */
  beats?: number
}

export type SectionKind = 'tab' | 'arpeggio' | 'fingerstyle' | 'chords'

export interface TabSectionDTO {
  id?: string
  /** 'fingerstyle' resalta la digitación PIMA y las técnicas de la pieza */
  kind: 'tab' | 'arpeggio' | 'fingerstyle'
  name: string
  /** BPM propio de la sección; si falta se usa el baseBpm del documento */
  bpm?: number
  /** Notas de estudio: consejos sobre cómo tocar la sección */
  notes?: string | null
  measures: MeasureDTO[]
}

export interface ChordSectionDTO {
  id?: string
  kind: 'chords'
  name: string
  bpm?: number
  notes?: string | null
  chords: ChordShape[]
}

export type SectionDTO = TabSectionDTO | ChordSectionDTO

export interface TabDocument {
  version: 2
  title: string
  category?: string | null
  tuning: string[]
  timeSignature: string
  baseBpm: number
  sections: SectionDTO[]
}

// ---- Formato v1 (compatibilidad con lo ya guardado) ----------------------

export interface GuitarTabV1 {
  metadata: {
    title: string
    tuning: string[]
    timeSignature: string
    baseBpm: number
  }
  measures: MeasureDTO[]
}

// ---- Otros contenidos de recurso -----------------------------------------

/** Fragmento reutilizable de tablatura (tipo de recurso SNIPPET) */
export interface SnippetContent {
  kind: 'fragment'
  tuning: string[]
  measures: MeasureDTO[]
}

/** Acorde suelto reutilizable (tipo de recurso CHORD) */
export interface ChordContent {
  kind: 'chord'
  chord: ChordShape
}

/** Colección de recursos (tipo de recurso COLLECTION) */
export interface CollectionContent {
  kind: 'collection'
  resourceIds: number[]
}

/** Artículo de teoría (tipo de recurso THEORY). El cuerpo admite un
 * markdown sencillo: `#`/`##` para títulos, `-` para listas y `**negrita**`. */
export interface TheoryContent {
  kind: 'theory'
  body: string
}

// ---- API -----------------------------------------------------------------

export type ResourceType = 'TAB' | 'CHORD' | 'SNIPPET' | 'COLLECTION'

export interface MuskopUser {
  id: number
  username: string
}

export interface ResourceSummary {
  id: number
  title: string
  type: string
  category: string | null
}

export interface ResourceDetail extends ResourceSummary {
  content: unknown
}

export interface SaveResourceRequest {
  title: string
  type: string
  category: string | null
  content: unknown
}
