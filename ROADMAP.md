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
   entera en el navegador. `muskop_core` deja de ser una dependencia (queda
   aparcado; podría volver en el futuro como sincronización opcional, nunca
   como requisito).
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
  "user": { "username": "carlos", "createdAt": "...", "updatedAt": "..." },
  "resources": [
    { "id": 1, "title": "...", "type": "TAB|CHORD|SNIPPET|COLLECTION",
      "category": "...", "content": { /* TabDocument v2, chord, snippet, collection */ } }
  ],
  "routines": [ /* ver bloque de rutinas */ ],
  "settings": { /* preferencias del usuario */ }
}
```

- Extensión sugerida: `.muskop.json` (sigue siendo JSON legible).
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
- [x] Retiradas las llamadas HTTP y el proxy de Vite; `muskop_core` queda
      aparcado (sin tocar, en el repo).

Decisiones tomadas: IndexedDB (un store `sessions`, un registro por sesión);
ids de recurso por autoincremento local (`nextResourceId` en el archivo);
`localStorage` solo guarda el id de la última sesión abierta.

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
- [ ] **Añadir ejercicios propios**: que el usuario pueda crear sus propios
      ejercicios guiados, no solo consumir el catálogo incluido. Idea:
      marcar cualquier tablatura/teoría de la librería como "ejercicio" con
      **habilidad + nivel + descripción** (metadatos en el propio recurso),
      de modo que aparezca junto al catálogo en Explorar y en las
      recomendaciones por nivel de la página de Progreso. Incluir también
      importación/exportación de packs de ejercicios (JSON) para
      compartirlos o pedírselos a un agente, reutilizando el formato de
      `AGENT_CONTEXT.md` con los metadatos de habilidad/nivel.

### Fase 4 — Móvil (futuro)

- [ ] Llevar la app a móvil. Al ser 100% frontend y basada en archivos, las
      opciones naturales son PWA (instalable, offline) o empaquetado
      (Capacitor). Decidir cuando llegue el momento; la arquitectura
      local-first ya lo facilita.

## Decisiones pendientes (apuntar aquí al decidirlas)

- Persistencia local exacta: IndexedDB vs localStorage (tamaño de sesiones),
  y si se usa File System Access API para "guardar en el mismo archivo".
- ¿Una sesión por usuario o varias sesiones por usuario? (p. ej. "carlos —
  clásica" y "carlos — eléctrica").
- Estrategia de ids dentro de la sesión (autoincremento local vs uuid) para
  que importar/fusionar sesiones no colisione.
- Qué hacer exactamente con `muskop_core` en el repo (mantener aparcado,
  moverlo a una rama, o eliminarlo).
