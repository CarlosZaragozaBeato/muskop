# 🎸 Muskop

**Your guitar practice — your data in your files.**

Muskop is a **local-first** guitar practice app (focused on fingerstyle and
related techniques). It runs entirely in the browser — and, packaged with
Capacitor, as a native **Android** app — with **no backend**. Everything you
create lives in a portable session file you own, can download, move between
devices, or open with any app that speaks the same format.

- **Tab editor** — tab, arpeggio, fingerstyle and chord sections, with PIMA
  right-hand fingering, techniques (hammer-on, pull-off, slide, bend, vibrato,
  palm mute, harmonic), per-section BPM, metronome playback, and export to
  text, PNG or PDF.
- **Practice routines** — ordered blocks with duration, target BPM, an
  associated library resource and cues. Practise them block by block with a
  timer, auto-advance and a visual metronome.
- **Progress & XP** — every practised minute earns XP per skill; levels,
  streaks, daily average and total time are computed from your practice log.
- **Explore** — a bundled catalog of guided fingerstyle exercises and theory
  articles, plus your **own exercises** (mark any tab/theory as an exercise
  with skill + level). Import/export exercise packs as JSON.
- **Library** — reusable tabs, chords, snippets, theory and collections.
- **Bilingual** — English (default) and Spanish, switchable at runtime.
- **Light & dark themes** — following the Muskop brand palette.

## Local-first: the session file

There is no login server. Writing a username **creates a new session** (or
opens an existing one). A *session* is your whole "database": library,
routines, practice log, experience and settings. It is stored on the device
(IndexedDB) and can be **downloaded at any time** as a `*.muskop.json` file —
that download is the official backup and the way to move everything (levels,
routines, blocks, tabs, statistics, own exercises) to another device.

- Resource and routine ids are **UUIDs**, so importing/merging sessions never
  collides. Old sessions with numeric ids migrate automatically on open.
- Multiple sessions per user are supported (an optional label distinguishes
  e.g. "carlos — classical" from "carlos — electric").
- The bundled Explore catalog ships with the app, not in your file.

The exact JSON schema and the tab/routine/pack formats accepted on import (also
handy to have an AI agent generate content) are documented in
[`AGENT_CONTEXT.md`](AGENT_CONTEXT.md).

## Tech stack

- **React 19 + TypeScript + Vite** (single-page app, `muskop_front/`)
- **Web Audio API** for the metronome and playback
- **SVG + Canvas** for tab rendering and PNG/PDF export
- **IndexedDB** for session storage; `localStorage` for language/theme/last
  session
- **Capacitor 8** to package the same frontend as a native Android app
- No state library, no CSS framework, no i18n library — small hand-rolled
  layers (`src/i18n/`, `src/theme/`, `src/storage/`) keep dependencies minimal

## Getting started (web)

```bash
cd muskop_front
npm install
npm run dev        # http://localhost:5173
npm run build      # type-check + production build to dist/
npm run lint       # oxlint
```

## Building the Android app (Capacitor)

The Android project lives in `muskop_front/android/`. It needs a JDK **17–21**
(the Android Gradle Plugin does not support newer JDKs) and the Android SDK
(platform 36, build-tools 36).

```bash
cd muskop_front
npm run build                 # build the web assets
npx cap sync android          # copy dist/ and plugins into the native project
# App icons & splash (from assets/*.svg):
npx @capacitor/assets generate --android \
  --iconBackgroundColor '#1F2430' --splashBackgroundColor '#1F2430'
# Build the APK:
cd android
JAVA_HOME=/path/to/jdk21 ANDROID_HOME=/path/to/android-sdk ./gradlew assembleDebug
# → android/app/build/outputs/apk/debug/app-debug.apk
```

Install on a device with `adb install app-debug.apk`, or transfer the APK and
open it (enable "install unknown apps"). App id: `com.muskop.app`.

Native adaptations live in `src/native/`: file export/import uses the system
**Share** sheet + Filesystem on Android (plain download on web), and the
hardware **back** button is handled there too.

## Project structure

```
muskop_front/
  src/
    pages/            Screens (routines, practice, library, explore, progress…)
    components/       UI + the tab editor (components/tab/)
    storage/          Session model + IndexedDB (the local "database")
    i18n/             en.ts / es.ts dictionaries, t(), language switcher
    theme/            Light/dark theme context + toggle
    native/           Capacitor helpers (share, back button)
    data/             Bundled Explore catalog (exercises, theory, skill levels)
    audio/            Metronome and tab player (Web Audio)
    utils/            Import/export (tab, routine, exercise packs), stats
  android/            Native Android project (Capacitor)
  assets/             Icon/splash sources for @capacitor/assets
files_images/         Brand assets (icon, logotypes)
AGENT_CONTEXT.md      Content formats for AI-generated tabs/routines/packs
ROADMAP.md            Vision and decisions log
```

## Roadmap

See [`ROADMAP.md`](ROADMAP.md) for the phased plan and the decisions taken
(local-first, routines & practice, content, i18n, Android).

## Brand palette

`#1F2430` ink · `#3A4152` slate · `#F2A33C` amber (accent) · `#E8875B` coral ·
`#F5F1E8` cream.
