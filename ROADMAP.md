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

### Fase 4 — Móvil (en curso)

- [x] Empaquetado con **Capacitor** (`muskop_front/capacitor.config.ts`,
      `appId: com.muskop.app`) y proyecto Android generado (`android/`).
      Build del APK con JDK21 (`~/jdk21`) + SDK (`~/android-sdk`) y `gradlew`.
- [x] Pulir la experiencia móvil ✅ 2026-07-20: la navegación, settings y
      visores ya son mobile-first (Fase 5). Añadido: **barra de estado nativa
      coordinada con el tema** (`@capacitor/status-bar` + `native/statusBar.ts`;
      iconos claros/oscuros según el tema y `theme-color` dinámico para web/PWA),
      y **feedback táctil** (sin flash gris, sin rebote de overscroll, escala al
      pulsar, áreas de toque de la barra inferior ≥3rem). Auditoría a 393px sin
      overflow horizontal ni errores de consola.

> **Regla de paridad:** todo lo que se implemente para Android debe
> implementarse también en la web. La base es la misma app (100% frontend);
> no se hacen funciones exclusivas de móvil salvo integraciones nativas
> inevitables (p. ej. compartir con el SO), y aun así con equivalente web.

### Fase 5 — Experiencia de usuario y gestión de recursos (planificada)

> Recogida (2026-07-20). Objetivo: interfaz más limpia y "mobile-first",
> recursos multimedia y gamificación general. Cada punto se implementa por
> igual en web y en Android (regla de paridad de la Fase 4). Ordenada por
> prioridad; dentro de cada nivel, de arriba abajo.

#### Prioridad alta — pulido de UX y base de navegación

Cambios pequeños y de alto impacto, y la reestructuración de navegación de la
que dependen puntos posteriores.

- [x] **No auto-iniciar** la rutina al entrar en el modo práctica ✅ 2026-07-20:
      pantalla previa con resumen de bloques (nombre, habilidad, minutos y BPM
      / tempo libre) y tiempo total; el temporizador solo arranca al pulsar
      «Empezar» (`PracticePage.tsx`, estado `started`).
- [x] **Botón de visualización** del recurso desde la Librería ✅ 2026-07-20:
      botón «👁 Ver» en cada fila (tablaturas, acordes, fragmentos y teoría)
      que abre un visor modal de solo lectura reutilizando `ResourceView`
      (`ResourceViewDialog.tsx`), sin entrar al editor.
- [x] **Zoom** en el visor de recursos ✅ 2026-07-20: controles −/%/+ en la
      barra del visor que escalan el render SVG de tablaturas/fragmentos
      (0,5×–3×, con scroll horizontal), en `ResourceView`. Reutilizable para
      las imágenes cuando existan recursos de imagen (prioridad media).
- [x] **Barra superior más limpia** + **menú de Settings** específico
      ✅ 2026-07-20: nueva página `/settings` (`SettingsPage.tsx`) que agrupa
      tema, idioma, descarga de sesión y cuenta (logout). La cabecera derecha
      queda solo con el usuario y un icono ⚙️ hacia Ajustes. *(Base para la
      sección de Recursos en Settings.)*
- [x] **Barra de navegación estilo mobile** ✅ 2026-07-20: navegación principal
      con iconos en una lista compartida (`NAV_ITEMS` en `App.tsx`). En móvil
      (≤640px) se convierte en una **barra inferior fija** (icono + etiqueta,
      con `safe-area-inset`) y se oculta la nav superior; en escritorio sigue
      arriba. 5 secciones (Inicio, Rutinas, Progreso, Explorar, Librería);
      «Nueva tablatura» se accede desde la Librería y Ajustes desde el ⚙️.

#### Prioridad media — recursos multimedia y su gestión

- [x] **Subir recursos de imagen, audio y vídeo** ✅ 2026-07-20: nuevo tipo de
      recurso `MEDIA` (`content: {kind:'media', mediaType, mime, name, data,
      size}`) con el binario en data URL base64 dentro de la sesión. Botón
      «+ Multimedia» en la Librería con diálogo de subida
      (`MediaUploadDialog.tsx`), validación de tipo y **límite de 8 MB/archivo**
      (`utils/media.ts`, `MAX_MEDIA_BYTES`). El visor (`ResourceView`) renderiza
      imagen (con zoom reutilizado), audio y vídeo. El zoom sobre imágenes
      queda cubierto por el mismo mecanismo. Import/export de sesión ya lo
      soporta (tipo string libre, contenido opaco).
- [x] Nueva sección **Resources** dentro de Settings ✅ 2026-07-20: página
      `/settings/resources` (`ResourcesPage.tsx`) enlazada desde Ajustes, con
      la lista de **todos** los recursos y acciones de **visualizar**, **editar**
      (donde hay editor: tablatura/teoría), **exportar individual**, **eliminar**,
      e **importar / exportar en bloque**. Formato portable `muskopResources: 1`
      (`utils/resourceIO.ts`); al importar se crean recursos nuevos (id nuevo)
      para no colisionar.
- [x] **Exportación opcional de los archivos multimedia** ✅ 2026-07-20:
      `downloadActiveSession(includeMedia=false)` excluye por defecto los
      binarios (vacía `data` de los recursos MEDIA conservando los metadatos);
      en Ajustes hay un **check «incluir multimedia» desactivado por defecto**
      que solo aparece si la sesión tiene multimedia. El visor avisa si un
      recurso multimedia se exportó sin su archivo (`resourceView.mediaMissing`).

#### Prioridad baja — gamificación y compartir

Funcionalidades más grandes y menos bloqueantes.

- [x] **Nivel general** del usuario ✅ 2026-07-20: además del nivel por
      habilidad, un nivel general = suma de XP de habilidades + `bonusXp`
      (+50 al completar una rutina, y +100/+300/+1000 al cumplir los objetivos
      **semanal/mensual/anual**). Objetivos de minutos por periodo editables en
      Progreso (`goals`/`goalsClaimed` en la sesión); tarjeta de nivel general
      y sección de objetivos con barra de progreso y estado «cumplido». XP se
      otorga en `recordPractice` (una vez por periodo). Normalización de sesión
      para los campos nuevos.
- [x] **Logros (achievements)** ✅ 2026-07-20: catálogo de 15 logros
      (`utils/achievements.ts`) derivados de la sesión (primera práctica,
      rutinas completadas, rachas 3/7/30, minutos totales, nivel general 5/10,
      nivel 5 en una habilidad, objetivo cumplido, 5 recursos, primer
      multimedia). Página `/achievements` enlazada desde Progreso, con
      insignias desbloqueadas/bloqueadas. Se **guardan** en la sesión
      (`achievements`) al cumplirse, así que son permanentes.
- [x] **Compartir sesiones por correo** ✅ 2026-07-20: botón «Compartir por
      correo» en Ajustes (`shareActiveSession`, respeta el check de multimedia).
      En Android usa la hoja de compartir nativa (Capacitor `Share`, archivo
      adjunto); en web usa la Web Share API con archivos si está disponible y,
      si no, descarga el archivo + abre el cliente de correo (`mailto:`) con
      asunto/cuerpo para adjuntarlo a mano (`native/share.ts` `shareFile`).

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
