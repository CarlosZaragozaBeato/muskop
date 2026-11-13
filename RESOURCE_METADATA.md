# Musical resource metadata — version 1

Contrato de recursos relacionados. Android importa estos datos al pegar o
seleccionar una tablatura v2 y muestra sus relaciones desde **Connections** en
Library. El estado personal de aprendizaje está definido en
[RESOURCE_LEARNING.md](RESOURCE_LEARNING.md). El tiempo y última práctica
por recurso están definidos en [RESOURCE_PRACTICE.md](RESOURCE_PRACTICE.md).

## Almacenamiento y compatibilidad

- La sesión sigue siendo `muskopSession: 2` y la tablatura `version: 2`.
- `content.musical` es la única fuente de verdad de los metadatos musicales.
  En un JSON de tablatura independiente se escribe `musical` en su raíz.
- Es opcional: los recursos existentes no necesitan migración ni valores
  inventados. La dificultad ausente significa **sin evaluar**.
- La aplicación lee proyecciones del JSON; nunca lo reconstruye desde ellas.
  Los campos desconocidos se conservan al importar, editar, exportar y reabrir.
- Una importación individual rechaza metadatos inválidos o de una versión
  musical futura antes de reemplazar la vista previa. La importación de una
  sesión conserva su contenido opaco, incluidos metadatos futuros; el lector
  devuelve ausencia de metadatos interpretables cuando no puede leerlos.
- Los `tags` de organización siguen siendo independientes. Al importar una
  tablatura, sus `tags` opcionales se copian al campo del recurso que utiliza
  Library. Editar las etiquetas del recurso no reescribe el documento original;
  Library utiliza las etiquetas del recurso. No sirven como identidad musical.
- `exercise.skill/level` heredado se conserva; no se convierte automáticamente
  a esta dificultad. Tampoco se modifica el historial ni la experiencia.

## Campos

`musical.version` es obligatorio y vale `1`. El resto es opcional:

| Campo | Valores / significado |
| --- | --- |
| `role` | `exercise`, `song`, `fragment`, `theory`; propósito del recurso, independiente del tipo técnico TAB/THEORY |
| `difficulty` | Objeto con `level` entero 1–5 y `reasons` opcional como lista de textos |
| `concepts` | Lista de conceptos que el recurso entrena, utiliza o explica |

Escala orientativa: 1 introductorio, 2 básico, 3 intermedio, 4 avanzado,
5 muy avanzado. Se evalúa ese arreglo y su tempo objetivo, no toda la canción
en abstracto. `reasons` explica las exigencias concretas; no es una medida del
dominio del usuario. No declarar dificultad si no hay información suficiente.

Cada concepto requiere:

- `id`: identificador estable, independiente del idioma y del título del recurso.
- `label`: texto legible, en el idioma del contenido; nunca se traduce al guardar.
- `relation`: `trains` (ejercita), `uses` (aplica), `explains` (explica).
- `sectionIds`: lista opcional de ids de secciones donde aparece. Omitir para
  aplicarlo al recurso completo. Cada referencia debe resolver exactamente una
  sección con `id` explícito. No usar índices ni nombres como referencias.

Un recurso puede entrenar un cambio y utilizar otros conceptos. No repetir
la misma pareja `id`/`relation`: agrupar sus secciones en `sectionIds`.
Los campos adicionales se preservan sin interpretarlos. El contrato valida la
estructura, no la exactitud del análisis musical realizado por el generador.

## Identificadores y vocabulario inicial

Formato: `familia:identificador`, en ASCII minúsculas con letras, números y
separadores `.`, `_` o `-`. Familias admitidas: `chord`, `transition`,
`progression`, `scale`, `arpeggio`, `pattern`, `technique`.

Antes de crear un identificador, reutilizar los que ya existan en la sesión
facilitada al generador y los de esta tabla. La etiqueta puede cambiar de idioma;
el identificador no. No generar sinónimos para el mismo concepto.

| Concepto | ID canónico inicial |
| --- | --- |
| Am | `chord:a-minor` |
| C | `chord:c-major` |
| G | `chord:g-major` |
| F | `chord:f-major` |
| Cambio Am → C | `transition:a-minor_to_c-major` |
| Cambio C → G | `transition:c-major_to_g-major` |
| Progresión Am → F → C → G | `progression:a-minor_f-major_c-major_g-major` |
| Pentatónica menor de La | `scale:a-minor-pentatonic` |
| Patrón PIMA | `pattern:p-i-m-a` |
| Travis picking | `pattern:travis-picking` |
| Bajo alternado | `technique:alternating-bass` |
| Cejilla | `technique:barre` |
| Hammer-on | `technique:hammer-on` |

Los cambios son direccionales: C → Am es otro concepto. Una progresión conserva
orden y repeticiones. Un arpegio debe especificar su identidad musical; no
confundirlo con un patrón de dedos. No considerar equivalentes automáticamente
acordes con inversiones, posiciones o afinaciones distintas cuando la ejecución
importe: usar un id específico coherente entre los recursos que la comparten.

Se admiten nuevos identificadores de estas familias sin actualizar la app.
El validador comprueba su forma, pero no resuelve sinónimos ni inventa relaciones.
Compartir un concepto permite encontrar coincidencias; por sí solo no demuestra
que un ejercicio sea suficiente para aprender una canción.

## Ejemplo importable: ejercicio

```json
{
  "version": 2,
  "title": "Am to C preparation",
  "category": "fingerstyle",
  "tags": ["warmup"],
  "tuning": ["E", "A", "D", "G", "B", "E"],
  "timeSignature": "4/4",
  "baseBpm": 60,
  "musical": {
    "version": 1,
    "role": "exercise",
    "difficulty": {"level": 2, "reasons": ["Chord changes while keeping a steady pulse"]},
    "concepts": [
      {
        "id": "transition:a-minor_to_c-major",
        "label": "Am → C",
        "relation": "trains",
        "sectionIds": ["changes"]
      }
    ]
  },
  "sections": [
    {
      "id": "changes",
      "kind": "chords",
      "name": "Am → C",
      "notes": "Keep the pulse steady as you change chords.",
      "chords": [
        {"name": "Am", "frets": [null, 0, 2, 2, 1, 0], "beats": 4},
        {"name": "C", "frets": [null, 3, 2, 0, 1, 0], "beats": 4}
      ]
    }
  ]
}
```

Una canción que aplique ese cambio declara el mismo
`transition:a-minor_to_c-major`, con `relation: "uses"` y `role: "song"`.
Su `sectionIds`, si existe, referencia las secciones de la propia canción.
No se enlazan títulos ni UUID de recursos: ambos comparten la identidad del
concepto. Una explicación teórica usa `relation: "explains"`.

El modelo lector también admite `musical` dentro del contenido THEORY de una
sesión importada, y editar su cuerpo lo conserva. No se añade en este paso un
formulario de conceptos. La importación posterior de lotes TAB/THEORY desde
Library está definida en [RESOURCE_IMPORT.md](RESOURCE_IMPORT.md).

## Relaciones visuales — paso 2

Contexto para el agente que analiza sesiones y genera contenido relacionado:
[CONTENT_AGENT_CONTEXT.md](CONTENT_AGENT_CONTEXT.md). Se entrega junto a la sesión
exportada más reciente; incluye los contratos necesarios sin depender del repo.

El detalle del recurso mantiene **Read** como vista inicial. **Connections**
muestra función, dificultad, motivos y conceptos agrupados por su relación,
junto a los recursos relacionados de la biblioteca actual:

| Vista desde el recurso actual | Coincidencia por el mismo id de concepto |
| --- | --- |
| Prepare this piece / resource | El actual `uses`, el relacionado `trains` |
| Useful for | El actual `trains`, el relacionado `uses` |
| Useful for (explicación) | El actual `explains`, el relacionado `uses` o `trains` |
| Understand the concepts | El actual `uses` o `trains`, el relacionado `explains` |

No se deriva preparación de `uses` → `uses` ni de tags, títulos, categorías o
similitud de etiquetas. La función `role` no restringe las relaciones: un
ejercicio también puede usar conceptos y requerir preparación.

Cada tarjeta agrupa los conceptos compartidos con ese recurso y muestra sus
secciones en ambos lados. Sin referencias explícitas indica el recurso completo;
si una sección desapareció o su id quedó ambiguo, avisa de que la referencia ya
no está disponible. La relación musical se conserva sin inventar una ubicación.

Las conexiones se calculan al leer la biblioteca; no se guardan aristas ni
porcentajes de dominio en la sesión. Por grupo se muestran primero los recursos
con más conceptos compartidos, después por título e id. Este orden no constituye
una evaluación pedagógica ni una garantía de preparación suficiente.

Abrir una tarjeta permite leer el recurso relacionado. Volver regresa al
anterior con su vista y posición; volver de nuevo conduce a Library. Los ciclos
del recorrido se reducen al recurso ya visitado. La lectura compartida con
práctica libre/guiada no incorpora controles de conexiones.

### Recorrido hacia un objetivo

Library también muestra **Study path**, un recorrido directo de explicaciones,
ejercicios y aplicación hacia el recurso abierto. Reutiliza este contrato sin
nuevos campos. Ver [RESOURCE_STUDY_PATH.md](RESOURCE_STUDY_PATH.md) para reglas y
conceptos que todavía no tienen preparación en la biblioteca.

### Estado de comprobación

El propietario ha instalado la app y comunica funcionamiento general favorable.
Posteriormente confirmó que las conexiones funcionan con nuevos recursos que
utilizan los metadatos musicales. Los tests del núcleo comprueban
la coincidencia de ids y sus direcciones; no sustituyen esa prueba de uso.
