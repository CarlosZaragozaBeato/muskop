# Muskop Android

Aplicación activa de Muskop: Kotlin, Jetpack Compose y Material 3. Funciona sin
backend, cuenta remota ni conexión. `../muskop_front/` conserva la referencia
React/Capacitor anterior; las nuevas features se implementan aquí.

## Arquitectura

- `session-core/`: Kotlin puro; contrato de sesión v2, importación, operaciones
  inmutables, metadatos musicales, conexiones entre recursos y pruebas.
- `app/`: navegación Compose, biblioteca/visores/editores, rutinas/práctica,
  metrónomo, progreso, selectores Android y persistencia privada.
- Cinco destinos: Home, Routines, Library, Progress y Settings. La propuesta
  de cuatro destinos sigue pendiente.

La sesión es la fuente de verdad. Android mantiene un documento activo en
`filesDir/sessions/active-session.muskop.json`. Exportar crea una copia portable;
importar una sesión reemplaza el documento activo, sin fusionar bibliotecas.
Sesiones v1 migran a v2; versiones futuras se rechazan antes de guardar. Las
escrituras son atómicas cuando el sistema de archivos lo permite. No filtrar
campos desconocidos ni regenerar contenido desde los modelos de lectura.

## Estado funcional

Biblioteca con búsqueda/filtros, importación de tablaturas v2 por texto/archivo,
lectura por bloques y completa, teoría y edición manual inicial de seis cuerdas
4/4. Rutinas con editor, preparación, práctica guiada y libre, reanudación y
metrónomo independiente. Progreso muestra tiempo, actividad y objetivos.

Al finalizar y guardar práctica libre/guiada/parcial se abre Practice summary:
tiempo exacto, recursos practicados, acumulado del día y valoración manual
opcional de estado/maestría. Permite reintentar un guardado fallido con el mismo
id o volver a practicar pausada. Ver [PRACTICE_REVIEW.md](../PRACTICE_REVIEW.md).

Los metadatos `content.musical` v1 permiten declarar conceptos, función y
exigencia del arreglo. Library → recurso → Connections muestra preparación,
aplicaciones y explicaciones, con dificultad y ubicaciones en secciones.
El estado manual de aprendizaje por recurso se guarda en `resources[].learning`
y se edita desde el detalle. El tiempo y última práctica se calculan desde
atribución explícita en el log, con reanudación y selección del recurso en
práctica libre incluso con la lectura oculta. Los registros antiguos no se
reparten retrospectivamente; ver [RESOURCE_PRACTICE.md](../RESOURCE_PRACTICE.md).

El propietario ha instalado la app y ha confirmado que las conexiones funcionan
con nuevos recursos relacionados. El nuevo estado manual de aprendizaje todavía
requiere prueba en dispositivo. No interpretar el feedback como cobertura exhaustiva.

Library → recurso → Study path organiza explicaciones, preparación y aplicación
para sus conceptos `uses`, señalando conceptos sin ejercicios disponibles.
No modifica la sesión ni crea una rutina automáticamente; contrato en
[RESOURCE_STUDY_PATH.md](../RESOURCE_STUDY_PATH.md).

Learning status incluye un porcentaje manual opcional 0–100 % con slider.
Se guarda en `resources[].learning.masteryPercent`, separado del estado, y puede
quitarse sin convertirlo a 0. El detalle y tarjetas muestran la valoración; ver
[RESOURCE_LEARNING.md](../RESOURCE_LEARNING.md).


Library → recurso → Organize permite marcar Active/Hidden/Completed y prioridad
High/Normal/Low. Library y los selectores de recurso abren en Active, con los
prioritarios primero; los apartados siguen accesibles desde sus filtros. Rutinas
e historial conservan vínculos y orden. Ver [RESOURCE_ORGANIZATION.md](../RESOURCE_ORGANIZATION.md).

## Importación de contenido

El lector muestra PIMA explícita debajo de cada traste, con leyenda y Show/Hide
PIMA, también en pantalla completa y Practice por compás. Los ataques próximos
se reparten en filas sin dividir las notas simultáneas. La digitación ausente no
se inventa. Alcance y límites: [TAB_READING.md](../TAB_READING.md).

- Tablatura individual: Library → Add resource → Import generated tablature.
- Lotes TAB/THEORY: Library → Import resources, con archivos múltiples, array
  pegado o paquete `muskopResources: 1`. Revisión previa y añadido conjunto
  con ids nuevos; ver [RESOURCE_IMPORT.md](../RESOURCE_IMPORT.md).
- Rutinas: editor nativo o incorporadas en una sesión completa. No hay importador
  individual de `muskopRoutine` ni de `muskopExercisePack`.
- Teoría: editor nativo, Import resources o contenido de una sesión completa.
- Los tags organizativos y los conceptos musicales son campos distintos.

Para generar contenido dar al agente [CONTENT_AGENT_CONTEXT.md](../CONTENT_AGENT_CONTEXT.md)
y la sesión exportada más reciente. Para desarrollar leer primero
[AGENTS.md](../AGENTS.md) y [RESOURCE_METADATA.md](../RESOURCE_METADATA.md).

## Compilación y validación

JDK 21, SDK Android API 36, Gradle wrapper 8.14.3, AGP 8.13.0, Kotlin 2.2.21 y
bytecode JVM 17. Usar el wrapper; el Gradle global 9.x y Java 27 no son la base
compatible del proyecto.

Desde este directorio:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :session-core:test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleDebug
```

Si el SDK no se detecta, indicar `sdk.dir` en `local.properties` (sin subirlo a
Git) o las variables de entorno Android correspondientes. APK:
`app/build/outputs/apk/debug/app-debug.apk`.

Validación opcional, de solo lectura, con la sesión privada local:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :session-core:test \
  -Dmuskop.sessionFixture=../../Muskop.muskop.json
```

No copiar esa sesión al repositorio ni registrar su contenido. La entrega de
conexiones pasó 31 tests y compilación directa Kotlin/Compose; el wrapper quedó
bloqueado antes de compilar por restricciones del entorno. Esos resultados no
equivalen a un APK construido aquí ni a pruebas completas en dispositivo.

El corte posterior de estado manual de aprendizaje pasó 39 tests y compilación
directa de todos los fuentes Kotlin/Compose. El intento con wrapper se detuvo
en el lock de Gradle de solo lectura. No se ha generado un APK desde este entorno
ni probado en dispositivo el nuevo control.

El corte de tiempo/última práctica por recurso pasó 46 tests y compilación
directa de todos los fuentes Kotlin/Compose. Gradle quedó bloqueado en el lock
de solo lectura; APK y validación en dispositivo siguen pendientes para este corte.

El corte Study path pasó 57 tests del núcleo y compilación directa de todos
los fuentes Kotlin/Compose. El wrapper se detuvo en el lock de solo lectura,
por lo que el APK y la prueba en dispositivo siguen pendientes para este corte.

## Recuperación después de mover el proyecto

Si D8/mergeLibDexDebug indica que una clase está fuera del directorio raíz y
mezcla la ruta antigua con `muskop_develop/muskop`, los resultados previos de
compilación conservan rutas anteriores. Desde este directorio, regenerarlos:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew clean :session-core:test :app:assembleDebug \
  --no-configuration-cache --no-build-cache --rerun-tasks -Pkotlin.incremental=false
```

`clean` elimina los resultados compilados de los módulos; no toca los fuentes
ni la sesión del dispositivo. Las opciones desactivan la reutilización de cachés
y la compilación incremental Kotlin solo para esa ejecución. Después de una
build correcta, se puede volver al comando habitual. El agente no pudo ejecutar
esta recuperación por el lock de `~/.gradle` de solo lectura; queda pendiente
confirmarla en el entorno del propietario.
