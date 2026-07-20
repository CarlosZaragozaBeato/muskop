# Muskop Front

Muskop (rutinas de guitarra) — React + TypeScript + Vite. **Local-first**: no
hay backend; los datos viven en **sesiones**, archivos `.muskop.json` que se
guardan en el dispositivo (IndexedDB) y se pueden descargar/importar (ver
`ROADMAP.md` en la raíz del repo).

## Desarrollo

```bash
npm install
npm run dev
```

## Rutinas

En `/routines`: rutinas compuestas de **bloques de práctica** (nombre,
minutos, BPM objetivo, recurso de la librería e indicaciones). El modo
práctica recorre los bloques con temporizador (auto-avanza con aviso sonoro),
metrónomo al BPM objetivo con pulso visual, y el recurso en pantalla con
botón de escucha. Se guardan en la sesión (`routines`).

## Explorar y teoría

`/explore` trae un catálogo incluido de ejercicios fingerstyle (tablaturas con
digitación PIMA y notas de estudio, por habilidad y nivel) y artículos de
teoría, con vista previa y «Añadir a mi librería». La teoría es un tipo de
recurso más (`THEORY`, markdown sencillo) con editor propio (`/theory/new`) y
puede asociarse a bloques de rutina como material de estudio. El contenido del
catálogo vive en `src/data/exploreContent.ts`.

## Sesiones

- Al entrar se crea una sesión nueva (nombre de usuario), se importa un
  archivo `.muskop.json` o se continúa una sesión previa del dispositivo.
- Todo se autoguarda en IndexedDB; el botón «⬇ Sesión» de la cabecera descarga
  el archivo (backup/portabilidad oficial).
- Formato y capa de datos: `src/storage/` (`session.ts` formato, `db.ts`
  IndexedDB, `sessionManager.ts` sesión activa + CRUD). `src/api/client.ts` es
  ahora un shim sobre la sesión con las firmas del antiguo cliente HTTP.

## Editor de tablaturas

Un documento (formato v2, ver `src/types/tab.ts`) tiene metadatos (título,
categoría, afinación, compás, BPM base) y una lista de **secciones**:

- **Tablatura / Arpegio / Fingerstyle** — rejilla de 6 cuerdas × 16 semicorchea
  por compás (4/4). Clic + teclado (trastes 0–24, flechas, Retroceso), regla de
  pulsos arriba y fila de duraciones debajo. BPM propio por sección.
- **Acordes** — diagramas editables con clic, librería de acordes comunes,
  traste base y duración en pulsos por acorde.

Cada nota puede llevar **digitación de mano derecha** (PIMA, con colores) y
**técnica** (hammer-on, pull-off, slide, bend, vibrato, palm mute, armónico):
selecciona la nota y pulsa `p/i/m/a` o `h/o/s/b/v/x/n`, o usa la barra de
anotación. Las secciones *Fingerstyle* muestran además una leyenda para
principiantes, y todas las secciones admiten **notas de estudio** (📝) que se
incluyen en los exports. En la reproducción el pulgar acentúa, el palm mute
apaga y los armónicos suenan puros.

Funciones principales:

- **Reproducción** con Web Audio: metrónomo, plucks sintetizados y cursor
  resaltando la columna/acorde activo al BPM de cada sección
  (`src/audio/player.ts`).
- **Importar** (`src/utils/importers.ts`): pegar JSON v2/v1 o ASCII, subir
  archivo, y botón «Copiar formato de ejemplo» para pasárselo a un agente.
- **Exportar** (`src/utils/exporters.tsx`): `.txt` (ASCII), `.png` (SVG →
  canvas) y PDF (vista de impresión), tanto tablaturas sueltas como
  colecciones. El render es `src/components/tab/TabSvg.tsx`.
- **Incrustar**: guarda un compás como *snippet* o un acorde en la librería
  (recursos `SNIPPET` / `CHORD`) y reutilízalos en cualquier documento.
- **Librería** (`/library`): recursos con filtros por tipo y categoría, y
  **colecciones** (recurso `COLLECTION`) exportables a PDF/texto.

## Estructura

- `src/api/client.ts` — CRUD de recursos y login
- `src/auth/AuthContext.tsx` — sesión (usuario en localStorage)
- `src/components/tab/tabModel.ts` — modelo del editor y conversiones v1/v2
- `src/components/tab/TabEditor.tsx` — orquestador del editor
- `src/pages/` — Login, Home, Librería y página del editor (`/tabs/new`, `/tabs/:id`)
