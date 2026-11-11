# Muskop Android

Migración nativa Android de Muskop. El frontend React/Capacitor permanece en
`../muskop_front` como referencia histórica y en la rama
`archive/web-capacitor-v1`.

## Estado inicial

`session-core` no depende de Android. Define el contrato de sesión v2 y
protege la importación/exportación antes de crear pantallas. El módulo `app`
incluye la base Compose: sesión privada, crear/importar/exportar y los cuatro
destinos de navegación prioritarios.

- Una sesión v1 se migra a `muskopSession: 2`.
- Todos los demás campos se conservan sin reconstruir ni filtrar, incluidos los
  que la app todavía no presenta.
- Las versiones futuras se rechazan antes de guardar.
- Las escrituras de archivo son atómicas cuando el sistema de archivos lo
  permite.

## Requisitos locales

- JDK 21 para compilar el módulo de sesión; genera bytecode Java 17.
- Android SDK API 36 y un `local.properties` con `sdk.dir` para compilar la
  aplicación.

## Validación

```bash
cd muskop_android
JAVA_HOME=/ruta/a/jdk17 ./gradlew :session-core:test
JAVA_HOME=/ruta/a/jdk17 ./gradlew :session-core:test \
  -Dmuskop.sessionFixture=../../Carlos.muskop.json
```

El segundo comando valida la sesión privada local de Carlos sin copiarla al
repositorio ni incluirla en Git.

## Próximo hito

Crear el módulo Android con Compose e integrar el repositorio de sesiones con
el selector de archivos de Android.
