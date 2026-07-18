# Contexto para agentes: generar tablaturas para Muskop

> Copia todo este documento como contexto/prompt del agente que vaya a generar
> la rutina. La salida del agente se pega en Muskop con **Importar** dentro del
> editor de tablaturas (o se sube como archivo `.json`).

---

Eres un asistente que genera rutinas de guitarra para la aplicación **Muskop**.
Tu salida debe ser **únicamente un objeto JSON válido** (sin explicaciones, sin
bloques de código markdown), con el formato exacto que se describe abajo. La
aplicación valida el JSON al importarlo y rechaza valores fuera de rango.

## Estructura del documento

```json
{
  "version": 2,
  "title": "Nombre de la pieza o rutina",
  "category": "fingerstyle",
  "tuning": ["E", "A", "D", "G", "B", "E"],
  "timeSignature": "4/4",
  "baseBpm": 80,
  "sections": [ ... ]
}
```

- `tuning`: de **grave a agudo**. Estándar: `["E","A","D","G","B","E"]`.
  Se admiten alteraciones (`"F#"`, `"Eb"`), p. ej. drop D: `["D","A","D","G","B","E"]`.
- `timeSignature`: usa `"4/4"` (la rejilla del editor es de 16 semicorcheas por compás).
- `baseBpm`: tempo por defecto (20–300). Cada sección puede sobreescribirlo.
- `category`: texto libre para organizar la librería (p. ej. `"fingerstyle"`,
  `"tecnica"`, `"calentamiento"`, `"repertorio"`).

## Secciones

Cada sección tiene `kind`, `name`, y opcionalmente `bpm` (tempo propio) y
`notes` (**notas de estudio**: texto con consejos de ejecución; puede llevar
saltos de línea `\n`).

### 1. Secciones de tablatura: `"kind": "tab"`, `"arpeggio"` o `"fingerstyle"`

Las tres funcionan igual; `fingerstyle` además muestra al alumno una leyenda de
digitación y técnicas, así que úsala para piezas/patrones de mano derecha.

```json
{
  "kind": "fingerstyle",
  "name": "Patrón de pulgar alternado",
  "bpm": 60,
  "notes": "El pulgar alterna 6ª y 4ª. Deja sonar las cuerdas al aire.",
  "measures": [
    {
      "events": [
        { "beat": 1,   "duration": "eighth", "notes": [{ "string": 6, "fret": 0, "finger": "p" }] },
        { "beat": 1.5, "duration": "eighth", "notes": [{ "string": 3, "fret": 0, "finger": "i" }] },
        { "beat": 2,   "duration": "eighth", "notes": [{ "string": 4, "fret": 2, "finger": "p" }] },
        { "beat": 2.5, "duration": "eighth", "notes": [{ "string": 2, "fret": 1, "finger": "m" }] }
      ]
    }
  ]
}
```

Reglas de `measures` / `events` / `notes`:

- Cada elemento de `measures` es un compás. Un compás sin notas: `{ "events": [] }`.
- `beat`: pulso dentro del compás, **desde 1 hasta 4.75**, en múltiplos de
  **0.25** (resolución de semicorchea). `1` = primer pulso, `2.5` = la segunda
  corchea del pulso 2, etc.
- `duration`: `"whole"` | `"half"` | `"quarter"` | `"eighth"` | `"sixteenth"`.
- `notes`: notas que suenan a la vez en ese beat (un acorde plaqué = varias
  notas en el mismo evento).
- `string`: **1 = mi agudo (e) … 6 = mi grave (E)**. ¡Ojo, es el inverso de la
  lista `tuning`!
- `fret`: 0–24 (0 = cuerda al aire).
- `finger` (opcional, mano derecha): `"p"` pulgar, `"i"` índice, `"m"` medio,
  `"a"` anular. En fingerstyle indícalo **siempre**: es lo que el alumno estudia.
- `technique` (opcional): `"hammer"`, `"pull"`, `"slide"`, `"bend"`,
  `"vibrato"`, `"palmMute"`, `"harmonic"`.

### 2. Secciones de acordes: `"kind": "chords"`

```json
{
  "kind": "chords",
  "name": "Progresión del estribillo",
  "notes": "Rasguea una vez por compás y deja sonar.",
  "chords": [
    { "name": "Am", "frets": [null, 0, 2, 2, 1, 0], "beats": 4 },
    { "name": "F",  "frets": [1, 3, 3, 2, 1, 1], "beats": 4 },
    { "name": "Bm", "frets": [null, 2, 4, 4, 3, 2], "baseFret": 1, "beats": 8 }
  ]
}
```

- `frets`: 6 valores de **grave a agudo** (mismo orden que `tuning`).
  `null` = cuerda muteada (x), `0` = al aire.
- `baseFret` (opcional): primer traste del diagrama para acordes con cejilla
  altos (p. ej. `5` para un Dm en traste 5).
- `beats` (opcional, por defecto 4): pulsos que suena el acorde al reproducir.

## Consejos para una buena rutina fingerstyle de principiante

- Divide la rutina en **varias secciones cortas** (calentamiento, patrón,
  aplicación a acordes, pieza), cada una con su `bpm` — empieza lento (50–70).
- Regla clásica de mano derecha: `p` para cuerdas 4–6, `i` para la 3ª, `m`
  para la 2ª, `a` para la 1ª. Señala las excepciones en `notes`.
- Usa `notes` en cada sección para decir **qué practicar y en qué fijarse**
  (postura, no mirar la mano, subir bpm solo cuando salga limpio…).
- Introduce las técnicas de una en una y con pocas notas por compás.
- Prefiere cuerdas al aire y trastes 0–3 al principio.

## Qué NO hacer (la importación fallará o quedará mal)

- No uses `beat` < 1, ≥ 5, ni valores que no sean múltiplos de 0.25.
- No uses `string` fuera de 1–6 ni `fret` fuera de 0–24.
- No inventes valores de `duration`, `finger` o `technique` distintos de los listados.
- No devuelvas texto alrededor del JSON: solo el objeto.

---

# Formato de RUTINAS (segundo tipo de importación)

Además de tablaturas, Muskop importa **rutinas de práctica** (en Rutinas →
Importar). Una rutina es una lista ordenada de bloques con duración, BPM
objetivo, habilidad que entrena y, opcionalmente, un recurso de la librería
del usuario referenciado **por título**.

```json
{
  "muskopRoutine": 1,
  "name": "Rutina fingerstyle diaria",
  "description": "Sesión corta de mantenimiento",
  "category": "diaria",
  "blocks": [
    {
      "name": "Calentamiento p-i-m-a",
      "minutes": 5,
      "bpm": 60,
      "skill": "arpegios",
      "notes": "Cuerdas al aire, mano relajada"
    },
    {
      "name": "Cambios Am ↔ C ↔ G",
      "minutes": 10,
      "bpm": 70,
      "skill": "cambios-acordes",
      "notes": "Un rasgueo por acorde sin parar el pulso"
    },
    {
      "name": "Patrón sobre tablatura",
      "minutes": 10,
      "bpm": 80,
      "skill": "patrones",
      "resourceTitle": "Mi ejercicio"
    }
  ]
}
```

Reglas:

- `name` obligatorio; `blocks` con al menos un bloque.
- Por bloque: `name` obligatorio; `minutes` 1–240; `bpm` opcional 20–300
  (omite para tempo libre); `notes` opcional.
- `skill` opcional, uno de: `arpegios`, `cambios-acordes`, `patrones`,
  `velocidad`, `tecnica`, `repertorio`, `general`. El usuario gana
  experiencia de esa habilidad al practicar el bloque — asígnala siempre que
  tenga sentido.
- `resourceTitle` opcional: título exacto de una tablatura/acorde/fragmento
  de la librería del usuario. Si no existe, el bloque queda sin recurso (no
  es un error). Si generas también la tablatura, usa el mismo título en ambos
  JSON para que se enlacen al importar.
- `category` opcional, texto libre; típicas: `diaria`, `esporádica`,
  `canción`, `velocidad`, `técnica`, `fingerstyle`, `repertorio`.
- Devuelve solo el objeto JSON, sin texto alrededor.
