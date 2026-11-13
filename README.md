# Muskop

**Your guitar practice — your data in your files.**

Muskop is a local-first Android app for guitar practice, especially fingerstyle.
The active implementation is **Kotlin + Jetpack Compose / Material 3** in
`muskop_android/`. It works offline without an account or backend. The older
React/Capacitor app in `muskop_front/` is a historical reference.

## What works today

- Create, reopen, import and export a local `*.muskop.json` session.
- Library search and filters, tablature reading, chord diagrams and theory notes.
- Active/Hidden/Completed resource views and High/Normal/Low priority. Library
  and free/routine resource pickers default to active resources, ordered by priority.
- Import multiple TAB/THEORY resources from selected JSON files, a pasted array
  or a `muskopResources: 1` package, with review and a single append operation.
- Import generated tablature v2 by pasting JSON or choosing a file; preserve
  musical information and unknown fields. Read by block or as a full resource.
- Initial manual tablature editor for six-string 4/4 material.
- PIMA beside each supplied note, a hide/show legend, high-contrast frets and
  spaced attacks across full reading, full screen and measure practice.
- Routine editor, preparation screen, timed blocks, free practice, pause/resume
  and an independent metronome. Routine preparation does not start the timer.
- Progress with practice time, recent activity, streak, goals and skill breakdown.
- A saved-practice summary for free, completed and partial routine practice:
  exact time, day totals, practiced resources and optional mastery assessment.
- Imported musical concepts and difficulty. Resource **Connections** show
  preparation, applications and theory through shared musical identities.
- Manual personal learning status per resource, editable from its detail and
  visible in Library and connection cards.
- Optional manual mastery percentage (0–100%) in the same assessment dialog,
  independent of learning status, difficulty and recorded time.
- Recorded time and last practice per resource in guided/free practice, including
  resource selection while playing from memory and resumable attribution.
- Study path toward the opened resource: explanations, preparation, application
  and explicit gaps in the library, derived from its applied musical concepts.
- Light and dark themes; English interface, user content kept in its own language.

A resource that **trains** `transition:a-minor_to_c-major` can prepare another
that **uses** it. Organizational tags are separate from those musical concepts.
Connections explain their shared concepts and sections; they do not measure
mastery. Existing resources without musical metadata remain usable.

The owner has installed the app and confirmed that connections work with new
resources containing musical metadata. This is favorable manual feedback on
that flow, not complete device coverage.

## Your session is your data

The app saves one active session in private Android storage. Export from Settings
for backup or transfer. Importing a whole session **replaces the active document**;
it does not merge it with the current one. Export current work before importing
an older or externally amended copy.

The current session format is `muskopSession: 2`; v1 is migrated and future
session versions are rejected. Existing fields, ids, history and unknown JSON
are preserved. New resources/routines use UUID strings. The app does not require
an external database or cloud synchronization.

## Generate content with an external agent

Give the agent **[CONTENT_AGENT_CONTEXT.md](CONTENT_AGENT_CONTEXT.md)** and your
latest exported session, plus your goal and available practice time. That file
is self-contained: session interpretation, musical formats, shared concept ids,
personalization, examples and delivery modes are included.

| Delivery | Current Android entry point |
| --- | --- |
| Individual tablature v2 | Library → Add resource → Import generated tablature → Save |
| Multiple TAB/THEORY resources | Library → Import resources → choose files or review pasted JSON → Import |
| Routine plan | Create/edit it in Routines; no standalone routine JSON importer yet |
| Resources and routines in a complete updated session | Settings → Import; preserves prior data only if the agent kept it in the returned document |
| Theory note | Native editor, Import resources, or inside a complete session |

Standalone routine and exercise-pack imports remain unavailable. Resource
packages version 1 now support TAB/THEORY through Import resources. Resource time is recorded explicitly from this version; older logs stay
unattributed. Study path provides a direct visual sequence; creating routine
drafts from that sequence remains future work. Manual
learning status is independent of difficulty and practice time.

## Documentation

| File | Purpose |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Engineering rules and architecture for development agents |
| [CONTENT_AGENT_CONTEXT.md](CONTENT_AGENT_CONTEXT.md) | Standalone context for a practice/content agent receiving a session |
| [AGENT_CONTEXT.md](AGENT_CONTEXT.md) | Individual tablature format and explicitly marked legacy formats |
| [RESOURCE_METADATA.md](RESOURCE_METADATA.md) | Musical metadata v1 and exact connection semantics |
| [RESOURCE_IMPORT.md](RESOURCE_IMPORT.md) | Multiple JSON files, resource packages, validation and append behavior |
| [RESOURCE_LEARNING.md](RESOURCE_LEARNING.md) | Personal learning status and persistence contract |
| [RESOURCE_ORGANIZATION.md](RESOURCE_ORGANIZATION.md) | Hide/complete resources, recover them and prioritize selection |
| [RESOURCE_PRACTICE.md](RESOURCE_PRACTICE.md) | Resource time, last practice, checkpoints and idempotent recording |
| [PRACTICE_REVIEW.md](PRACTICE_REVIEW.md) | End-of-practice summary, day totals, save retry and manual assessment |
| [RESOURCE_STUDY_PATH.md](RESOURCE_STUDY_PATH.md) | Direct study sequence toward a target resource and missing preparation |
| [DESIGN_DIRECTION.md](DESIGN_DIRECTION.md) | Agreed visual baseline and remaining design proposals |
| [TAB_READING.md](TAB_READING.md) | PIMA, contrast and dense-measure reading behavior |
| [ROADMAP.md](ROADMAP.md) | Current status followed by historical decisions |
| [muskop_android/README.md](muskop_android/README.md) | Native structure and build instructions |

## Build and test

Use JDK 21, Android SDK API 36 and the project's Gradle wrapper:

```bash
cd muskop_android
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :session-core:test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleDebug
```

Set `sdk.dir` in an untracked `local.properties` if needed. The debug APK is
`muskop_android/app/build/outputs/apk/debug/app-debug.apk`; app id `com.muskop.app`.
Gradle 8.14.3 / AGP 8.13.0 / Kotlin 2.2.21 generate JVM 17 bytecode. Avoid the
system Gradle and newer JDKs unless performing an explicit toolchain migration.

The connections delivery passed 31 core tests and direct compilation of all
Android Kotlin/Compose sources. Wrapper builds in the restricted agent environment
stopped before compilation because the Gradle lock service could not use local
networking. Owner installation feedback is tracked separately from those checks.

The manual learning-status delivery passed 39 core tests and direct compilation
of all Android Kotlin/Compose sources. Its wrapper attempt stopped on a read-only
Gradle lock; APK generation and device validation remain pending for this change.

The resource-time delivery passed 46 core tests and direct Android Kotlin/Compose
compilation. Its Gradle wrapper attempt stopped on the read-only cache lock.
APK generation and device checks remain pending for this change.

The Study path delivery passed 57 core tests and direct compilation of all
Android Kotlin/Compose sources. Its wrapper build stopped before compilation on
the read-only Gradle lock. APK generation and device checks remain pending.

The practice-summary delivery passed 94 core tests and direct Android
Kotlin/Compose source compilation. The wrapper stopped on the read-only cache
lock; APK packaging and device validation remain pending for this change.

## Source layout

```text
muskop_android/
  app/             Android UI, practice, Library, progress and session storage
  session-core/    Pure Kotlin JSON contract, immutable operations and tests
muskop_front/      Legacy React/Capacitor reference
files_images/      Brand assets
```

Brand: white, black, neutral grays and orange `#F2A33C`. See DESIGN_DIRECTION.md
for the native theme; the older web palette is not the active visual contract.
