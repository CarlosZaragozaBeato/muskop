# Muskop — Visión y hoja de ruta

> Documento de trabajo. Recoge la dirección acordada (2026-07-18) para ir
> implementándola poco a poco. Actualizar aquí cada decisión nueva.

## Visión

Muskop pasa a ser una **herramienta 100% frontend, local-first**: una app de
práctica de guitarra (fingerstyle y derivados) que no depende de ningún
servicio de backend para guardar datos. Los datos viven en **archivos que el
usuario posee**, puede descargar, mover a otro dispositivo o abrir con
cualquier aplicación que comparta el mismo formato.

## Principios

1. **Sin backend para datos.** Nada de base de datos remota; la app funciona
   entera en el navegador. El antiguo `muskop_core` (backend Java) se ha
   eliminado del repo; podría volver en el futuro como sincronización
   opcional, nunca como requisito.
2. **El archivo es la fuente de verdad.** La librería de tablaturas/prácticas
   se guarda en archivos exportables con estructura documentada y versionada
   (JSON). Portabilidad primero: otras apps que implementen el formato deben
   poder leerlos.
3. **Sesiones como "base de datos local".** Una *sesión* es un archivo que
   contiene todo lo de un usuario: su librería, sus rutinas, su configuración.

## Concepto de sesión

- Al hacer **login** ya no se consulta ningún servicio: escribir un nombre de
  usuario **crea una sesión nueva** (o abre una existente).
- En la pantalla de entrada se puede:
  - **Crear sesión nueva** (nombre de usuario → archivo de sesión nuevo).
  - **Importar una sesión** desde un archivo del dispositivo.
  - **Continuar una sesión anterior** usada en este dispositivo (la app
    mantiene una copia/registro local, p. ej. en IndexedDB/localStorage, de
    las sesiones abiertas anteriormente).
- La sesión se puede **descargar en cualquier momento** como archivo (nuestro
  "llevárnoslo"), y esa descarga es el backup/portabilidad oficial.

### Formato de archivo de sesión (propuesta inicial, a refinar al implementar)

```json
{
  "muskopSession": 1,
  "user": { "username": "carlos", "label": "clásica",
            "createdAt": "...", "updatedAt": "..." },
  "resources": [
    { "id": "uuid", "title": "...", "type": "TAB|CHORD|SNIPPET|COLLECTION|THEORY",
      "category": "...", "content": { /* TabDocument v2, chord, snippet, collection, theory */ },
      "exercise": { "skill": "arpegios", "level": 1, "description": "..." } }
  ],
  "routines": [ /* ver bloque de rutinas */ ],
  "practiceLog": [ /* una entrada por sesión de práctica */ ],
  "experience": { "arpegios": 120 },
  "settings": { /* preferencias del usuario */ }
}
```

- Extensión sugerida: `.muskop.json` (sigue siendo JSON legible).
- Ids de recurso y rutina por **UUID** (string). Al abrir sesiones antiguas
  con ids numéricos se migran a string automáticamente.
- `user.label` opcional: distingue varias sesiones del mismo usuario.
- `exercise` opcional en un recurso: lo marca como ejercicio (habilidad/nivel).
- El `content` de los recursos reutiliza los formatos ya existentes
  (TabDocument v2 — ver `AGENT_CONTEXT.md`), que no cambian.
- Versionado explícito (`muskopSession`) para poder migrar en el futuro.

## Hoja de ruta

### Fase 1 — Local-first (quitar el backend) ✅ 2026-07-18

- [x] Capa de almacenamiento que sustituye a `src/api/client.ts`: misma
      interfaz (list/get/create/update/delete de recursos) pero contra la
      sesión en memoria + persistencia local (`muskop_front/src/storage/`).
- [x] Pantalla de entrada nueva: crear sesión / importar archivo / continuar
      sesiones previas del dispositivo (con reapertura automática de la última).
- [x] Exportar sesión (botón «⬇ Sesión», archivo `usuario.muskop.json`) e
      importar sesión con validación de versión (`muskopSession: 1`).
- [x] Autoguardado en IndexedDB en cada cambio.
- [x] Retiradas las llamadas HTTP y el proxy de Vite; `muskop_core` eliminado
      del repo (2026-07-19, ver «Decisiones tomadas»).

Decisiones tomadas: IndexedDB (un store `sessions`, un registro por sesión);
ids de recurso y rutina por **UUID** (antes autoincremento local; migración
automática al abrir sesiones antiguas); `localStorage` solo guarda el id de la
última sesión abierta.

### Fase 2 — Rutinas y bloques de práctica ✅ 2026-07-18

- [x] Modelo de **rutina**: lista ordenada de **bloques de práctica**
      (`muskop_front/src/types/routine.ts`).
- [x] Bloque de práctica: duración (min), BPM objetivo, recurso asociado
      (tablatura/acorde/snippet de la librería) e indicaciones.
- [x] Editor de rutinas (`/routines`, `/routines/:id`) + modo práctica
      (`/routines/:id/practice`): bloque a bloque con temporizador,
      auto-avance con aviso sonoro, metrónomo al BPM objetivo con pulso
      visual, y el recurso renderizado en pantalla con botón de escucha.
- [x] Las rutinas viven dentro de la sesión (`routines` + `nextRoutineId`);
      los archivos de sesión antiguos se normalizan al abrirlos.

Ampliación (2026-07-18):

- [x] **Importar/exportar rutinas** como JSON (recursos referenciados por
      título para que sean portables); formato documentado en
      `AGENT_CONTEXT.md`.
- [x] **Categorías de rutina** (diaria, esporádica, canción, velocidad…) con
      filtro en el listado.
- [x] **Registro de práctica** (`practiceLog` en la sesión): al practicar se
      guarda día, minutos reales y si se completó. Estadísticas: racha de
      días practicando, racha completando cada rutina, media diaria (7 días)
      y tiempo total.
- [x] **Experiencia por habilidad** (`experience` en la sesión): cada bloque
      entrena una habilidad (arpegios, cambios de acordes, patrones,
      velocidad, técnica, repertorio); 10 XP/minuto con bonus del 20% al
      completar. Niveles con umbral triangular y página `/progress` con
      catálogo de ejercicios recomendados por nivel (p. ej. cambios de
      acordes típicos por nivel).

### Fase 3 — Contenido ✅ 2026-07-18 (catálogo inicial, ampliable)

- [x] **Página Explorar** (`/explore`): catálogo incluido con la app de
      ejercicios fingerstyle guiados — tablaturas reales con digitación PIMA,
      técnicas y notas de estudio, etiquetadas por habilidad y nivel
      (arpegios, patrones incl. Travis, cambios de acordes, técnica,
      velocidad). Filtros por habilidad/tipo, vista previa y «Añadir a mi
      librería». Enlazado desde las tarjetas de la página de Progreso.
      Contenido en `muskop_front/src/data/exploreContent.ts` — ampliar ahí.
- [x] **Teoría**: nuevo tipo de recurso `THEORY` (`content: {kind:'theory',
      body}` con markdown sencillo). Editor con vista previa
      (`/theory/new`, `/theory/:id`), visor integrado, 4 artículos de
      arranque en Explorar, y usable como bloque de estudio en las rutinas
      (se muestra en el modo práctica).
- [x] **Añadir ejercicios propios** ✅ 2026-07-19: el usuario marca cualquier
      tablatura o teoría de su librería como "ejercicio" con **habilidad +
      nivel + descripción** (metadatos en el propio recurso, campo `exercise`).
      Se marca desde la Librería (botón 🎯); los ejercicios propios aparecen
      junto al catálogo en Explorar (con badge «Tuyo» y filtro de origen) y en
      las recomendaciones por nivel de Progreso (enlazados a su editor).
      **Import/export de packs de ejercicios** (JSON, `muskopExercisePack: 1`)
      desde Explorar, reutilizando los formatos de contenido existentes
      (TabDocument v2 y teoría); formato documentado en `AGENT_CONTEXT.md`.

### Internacionalización (ES/EN) ✅ 2026-07-19

- [x] Soporte de **dos idiomas: inglés (por defecto) y español**, con solución
      propia sin dependencias (`muskop_front/src/i18n/`): contexto `useI18n()`
      con función `t()` e interpolación `{var}`, diccionarios `en.ts`/`es.ts`,
      selector de idioma en la cabecera y en el login. El idioma se guarda a
      nivel de dispositivo en `localStorage` (`muskop.lang`, por defecto `en`).
- [x] Traducida toda la interfaz y también el **contenido incluido**
      (ejercicios, notas de estudio, artículos de teoría y recomendaciones por
      nivel): la estructura musical es común y solo cambia el texto
      (`exploreContent.en.ts`, catálogo EN/ES en `skillCatalog.ts`).
- [x] El render/exportación de tablaturas (`TabSvg`, ASCII/PNG/PDF) recibe las
      etiquetas traducidas como datos (no vía hook) para funcionar también
      fuera del árbol de React.
- [x] Los mensajes de error de validación de la capa de datos
      (importadores, sesión, packs, rutinas) se traducen con una función
      `translate()` autónoma (`i18n/translate.ts`) que lee el idioma de
      `localStorage`, reutilizable fuera del árbol de React.

Al añadir texto de UI nuevo: crear la clave en **`en.ts` y `es.ts`** y usar
`t('clave')` (nunca cadenas literales en los componentes).

### Fase 4 — Móvil (futuro)

- [ ] Llevar la app a móvil. Al ser 100% frontend y basada en archivos, las
      opciones naturales son PWA (instalable, offline) o empaquetado
      (Capacitor). Decidir cuando llegue el momento; la arquitectura
      local-first ya lo facilita.

## Decisiones tomadas (2026-07-19)

- **Persistencia local: IndexedDB.** Un único store `sessions`, un registro
  por sesión (ya en uso desde la Fase 1). Se descarta localStorage por tamaño.
  (El uso de File System Access API para "guardar en el mismo archivo" queda
  como posible mejora futura, no bloqueante.)
- **Varias sesiones por usuario.** Cada sesión es un registro independiente en
  el dispositivo; un mismo usuario puede tener varias (p. ej. "carlos —
  clásica" y "carlos — eléctrica"). Se añade una **etiqueta** opcional
  (`user.label`) al crear la sesión para distinguirlas en el login y en el
  nombre del archivo descargado.
- **Ids por UUID.** Recursos y rutinas usan UUID (`crypto.randomUUID`) en vez
  de autoincremento local, para que importar/fusionar sesiones nunca colisione.
  Las sesiones antiguas se migran al abrirlas: los ids numéricos se convierten
  a string de forma determinista (`1` → `"1"`), conservando las referencias
  (bloques → recurso, colecciones, registro de práctica → rutina).
- **`muskop_core` eliminado.** El backend Java/Quarkus no se usa en la
  arquitectura local-first; se ha eliminado del repo (recuperable por el
  historial de git si algún día se retoma la sincronización opcional). También
  se retiró el `.dockerignore` raíz, que solo servía a ese backend.
