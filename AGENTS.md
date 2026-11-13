# Muskop — Contexto de ingeniería para agentes

Este documento es la referencia operativa para cualquier agente que modifique
Muskop. Léelo completo antes de implementar una feature. `AGENT_CONTEXT.md`
tiene otro propósito: documenta formatos JSON para agentes que generan
contenido musical, no las reglas de desarrollo del repositorio.

Para un agente externo que deba analizar una sesión y generar recursos/rutinas,
entregar **`CONTENT_AGENT_CONTEXT.md` y la exportación más reciente de la sesión**.
Ese archivo es autosuficiente y distingue planificación, tablaturas individuales
y actualización íntegra de sesión. `RESOURCE_METADATA.md` define el contrato de
conceptos musicales; no confundirlo con tags organizativos o aprendizaje personal.

## 1. Dirección actual del proyecto

Muskop es una aplicación local-first para organizar y realizar práctica de
guitarra, especialmente fingerstyle. No tiene backend: la sesión del usuario
es su base de datos y debe poder importarse y exportarse como
`*.muskop.json`.

La implementación activa es la aplicación Android nativa:

- Proyecto: `muskop_android/`.
- Lenguaje: Kotlin.
- UI: Jetpack Compose + Material 3.
- Rama de integración actual: `feat/android-native-migration`.
- Rama que conserva la versión anterior: `archive/web-capacitor-v1`.
- `muskop_front/` es una referencia funcional y visual de React/Capacitor. No
  es el destino principal de nuevas implementaciones salvo petición explícita.

El `README.md` describe Android; `ROADMAP.md` distingue el estado vigente del
histórico web. No asumir que React, Capacitor o la antigua regla de paridad web
siguen siendo la arquitectura activa.

## 2. Principios que no deben romperse

1. **Local-first y sin backend.** La app debe funcionar sin conexión ni cuenta
   remota.
2. **La sesión es la fuente de verdad.** No crear una base de datos paralela
   que pueda divergir del documento de sesión.
3. **Preservación antes que reconstrucción.** Al editar o migrar JSON, conservar
   campos desconocidos y datos que la pantalla todavía no comprende.
4. **Compatibilidad de sesiones.** La versión canónica actual es
   `muskopSession: 2`; v1 se migra sin filtrar el árbol JSON y versiones futuras
   se rechazan antes de guardar.
5. **La app debe ser útil con los archivos del usuario.** Una UI bonita que
   pierda digitación, técnicas, campos rítmicos, tags o historial no es válida.
6. **Android nativo real.** No introducir WebView, React Native o Capacitor en
   esta migración sin una decisión explícita del propietario.

## 3. Mapa del código activo

```text
muskop_android/
  app/
    src/main/java/com/muskop/android/
      MainActivity.kt
      ui/                         navegación, Home, Settings y tema
      practice/                   rutinas, práctica, editor y metrónomo
      library/                    biblioteca, recursos y editor de tablaturas
      progress/                   métricas y progreso
      session/                    repositorio Android y ViewModel
    src/main/res/                 logos, splash, iconos y temas Android
  session-core/
    src/main/kotlin/com/muskop/session/
                                  contrato JSON, migraciones y operaciones
    src/test/                     pruebas y fixtures de compatibilidad
```

Responsabilidades:

- `session-core` es Kotlin puro, sin dependencias Android. Aquí viven las
  mutaciones del documento, validaciones y migraciones.
- `app` presenta el estado con Compose, usa selectores nativos para
  importar/exportar y persiste la sesión en el almacenamiento privado.
- Las pantallas no deben reimplementar manualmente mutaciones JSON que
  pertenecen a `session-core`.
- `MuskopApp.kt` coordina los cinco destinos actuales: Home, Routines, Library,
  Progress y Settings.

## 4. Estado funcional actual

- Crear, reabrir, importar y exportar una sesión local.
- Home con resumen de hoy/semana y accesos rápidos a práctica libre y última
  rutina; prioriza una práctica pendiente cuando existe.
- Biblioteca con búsqueda, filtros, lectura de recursos, metadatos y teoría.
- Importación de tablaturas v2 generadas por agentes mediante JSON pegado o
  archivo; vista previa, práctica por bloques y vista completa. El usuario ha
  probado el flujo en su dispositivo. La validación de cada corte se registra
  en su worklog; el corte de conexiones incluye tests y compilación directa,
  con la limitación de wrapper descrita más abajo.
- Editor manual inicial con grid de seis cuerdas por 16 semicorcheas en 4/4.
- Rutinas con bloques, duración, BPM, habilidad, recurso asociado, edición,
  preparación previa, práctica libre y práctica guiada. Start/Continue en la
  preparación inicia el tiempo; abrir el detalle de rutina no lo inicia.
- Reanudación de práctica conservando bloque, tiempo restante y tiempo por
  habilidad.
- Metrónomo con presets y con el BPM específico de la rutina, aunque no sea un
  preset estándar.
- Progreso con totales, racha, objetivos y habilidades.
- Primera actualización de diseño cerrada el 2026-11-12, con prueba inicial
  favorable del usuario. El detalle de alcance y validación está en
  `DESIGN_DIRECTION.md`; no equivale a una revisión exhaustiva de todos los flujos.
- Settings con información de sesión e importar/exportar.
- Recursos relacionados: `content.musical` v1, dificultad y Connections con
  preparación, aplicaciones y explicaciones. El propietario comunica que ha
  instalado la app y ha confirmado después que las conexiones funcionan
  correctamente con nuevos recursos que incluyen estos metadatos. Es feedback
  manual favorable del flujo, no una matriz completa de pruebas.
- Estado personal de aprendizaje manual por recurso: Not started, Learning,
  Playable y Maintaining, con Not assessed para ausencia de valoración.
  Contrato en `RESOURCE_LEARNING.md`; esta nueva entrega aún requiere prueba
  en dispositivo.
- Porcentaje personal de dominio opcional, 0–100 %, con slider en Learning status.
  `resources[].learning.masteryPercent` es independiente del estado y del tiempo;
  ausencia/null no equivale a cero. `updateResourceLearning` cambia únicamente
  los campos editados y persiste una sola valoración por Save.

Importar una sesión reemplaza el documento activo; no fusiona bibliotecas.
La importación individual acepta tablaturas v2. Library → Import resources
admite archivos múltiples, arrays y paquetes `muskopResources: 1` de TAB/THEORY.
Se prepara/valida el lote y se añade con ids nuevos en una mutación/guardado;
ver `RESOURCE_IMPORT.md`. Las rutinas individuales y packs de ejercicios
siguen sin importador nativo; las rutinas se crean en el editor o en una sesión.
No convertir un lote en una importación de sesión ni guardar elemento por elemento.
El estado manual y el tiempo/última práctica por recurso están implementados.
La finalización libre/guiada/parcial abre Practice summary tras guardar: duración,
recursos, acumulado del día de guardado y valoración manual de estado/maestría.
Ver `PRACTICE_REVIEW.md`. Consultar el log por practiceId; no sumar de nuevo la
entrada ni deducir aprendizaje. Los fallos permiten reintentar con el mismo id
o volver a la práctica pausada con su checkpoint de UI. El resumen/valoración
usa resultados de persistencia explícitos, sin presentar éxito antes de guardar.
El tiempo se deriva de atribución explícita en `practiceLog`, sin reconstruir
registros antiguos. Ver `RESOURCE_PRACTICE.md`. Las ayudas
de creación de rutinas desde el recorrido siguen pendientes. Study path muestra
explicaciones, preparación, aplicación y conceptos sin ejercicios disponibles;
ver `RESOURCE_STUDY_PATH.md`. Los registros pueden incluir
`seconds` además de `minutes`; priorizar segundos positivos para duración total
y conservar la compatibilidad con registros antiguos.

La aplicación ya compila y genera APK con el entorno local adecuado, pero aún
es una migración en evolución. El motor/editor de tablaturas y el pulido visual
son áreas prioritarias.

Organización de recursos: `resources[].organization` guarda status
(active/hidden/completed) y priority (high/normal/low). Library y los selectores
usan vistas filtradas y prioridad estable; no filtrar la colección canónica ni
las referencias de rutinas/práctica. Completed es independiente de masteryPercent.
`updateResourceOrganization` persiste la edición parcial sin eliminar datos.
Ver `RESOURCE_ORGANIZATION.md`. Pendiente prueba en dispositivo.

## 5. Preferencias visuales y de experiencia

- Idioma visible por defecto: **inglés**. No añadir nuevos textos de interfaz
  en español. Los nombres, descripciones y notas escritos por el usuario nunca
  se traducen automáticamente.
- Colores de marca: blanco, negro y naranja `#F2A33C`. Se permiten grises
  neutros únicamente para superficies, separación y contraste.
- Mantener modo claro y oscuro coherentes.
- Reutilizar los logos, splash e iconos existentes en `app/src/main/res/` y
  `files_images/`; no reemplazar la identidad visual por placeholders.
- Diseño mobile-first, claro y llamativo, pero simple. Debe probarse mentalmente
  y, cuando sea posible, en anchos estrechos.
- Crear jerarquía visible entre título, metadatos, descripción y acciones.
  Evitar bloques de texto apretados y controles sin separación.
- Evitar botones enormes y filas de acciones textuales. Usar iconos para
  acciones conocidas como reproducir, editar, añadir, exportar o volver.
- Todo icono interactivo necesita `contentDescription`. Mantener un área táctil
  accesible aunque el icono sea visualmente compacto.
- Nunca usar letras como sustituto de iconos de navegación.
- Proteger el ancho de acciones en `Row`: el contenido textual flexible debe
  usar `Modifier.weight(1f)` para impedir textos con una letra por línea.
- Comparar con la versión web anterior para no perder riqueza visual,
  especialmente en tablaturas, pero adaptar el resultado a patrones nativos.
- Antes de añadir más opciones, buscar una organización mejor: tarjetas,
  secciones plegables, chips, acciones progresivas y buenos estados vacíos.

El tema global está en `ui/MuskopTheme.kt`. Una feature no debe introducir una
paleta propia o tamaños arbitrarios si puede expresar la intención mediante
`MaterialTheme`.

### Base visual tras la primera actualización (2026-11-12)

Antes de modificar diseño, leer `DESIGN_DIRECTION.md`. Es la referencia visual
acordada, con decisiones implementadas y propuestas todavía pendientes.

- Tema: superficies neutras en claro/oscuro; naranja para acciones y estados.
  Emparejar cada superficie con su color de contenido. Los números sobre
  círculos naranjas usan `onPrimary`; evitar textos pequeños naranjas sobre blanco.
- Inicio: acción de práctica antes de métricas. Rutinas: tarjetas que abren
  preparación; edición desde el detalle y tratamiento explícito de rutinas vacías.
- `practice/PracticeLayout.kt` comparte cabecera, zona de lectura y controles
  fijos entre práctica libre/guiada. Las barras globales se ocultan durante
  práctica activa y se restauran al salir. El metrónomo es independiente del
  temporizador y sus tempos se eligen desde un diálogo.
- Sin recurso, `PracticeFocusPanel.kt` da protagonismo al tiempo y estado.
  Free practice permite buscar y filtrar recursos por tipo desde
  `PracticeResourceChooser.kt`, cambiarlos o quitarlos. La selección también
  atribuye los ticks nuevos al recurso y
  permite ocultar la lectura para practicar de memoria. Ver RESOURCE_PRACTICE.md.
- Progreso: tiempo total, gráfica de siete días, objetivos y reparto por
  habilidad. El tiempo registrado no mide dominio musical. El usuario considera
  satisfactorio el resultado actual; futuras iteraciones parten de esta base.
- Library conserva búsqueda, filtros y posición al volver de un recurso. El
  acceso `Add resource` abre el editor existente para teoría/tablaturas/importación.
- Dentro del recurso: cabecera fija, zoom desplegable, afinación/notas/detalles
  plegables y contenido musical prioritario. Los detalles se abren si el dibujo
  no puede representar las notas. `ResourcePreview` es compartido con Práctica:
  cualquier cambio debe comprobarse también allí.
- Lectura continua: Full resource es la vista inicial y distribuye los compases
  en filas. Un compás denso puede continuar en partes numeradas sin modificar
  sus datos. El icono de pantalla completa abre `TabReadingSheet.kt`, con zoom,
  pantalla encendida y desplazamiento automático opcional. Este desplazamiento
  no reproduce audio ni se sincroniza con BPM; se pausa al arrastrar, llegar al
  final o pasar al fondo. La vista por compás sigue disponible en Practice.
- La navegación inferior sigue teniendo **cinco** destinos. Los cuatro destinos
  con Settings en cabecera son una propuesta pendiente, no una feature terminada.

- Lectura PIMA: `TabReadingLayout.kt` proyecta notas y distribuye ataques completos
  con espacio mínimo; `library/TabNotationView.kt` dibuja trastes de alto contraste
  y PIMA debajo de cada nota. Las opciones Show/Hide PIMA y la leyenda se comparten
  entre lectura completa, pantalla completa y Practice por compás. No inferir
  digitación ausente ni confundir PIMA con mano izquierda. Ver `TAB_READING.md`.

El usuario ha probado esta primera actualización y comunica que funciona bien
por ahora; quiere utilizar más la aplicación antes de otra revisión completa.
Las iteraciones de diseño se retomarán cuando las solicite. Mantener separados
ese feedback, la revisión estática y las pruebas automatizadas: en este chat no
se consiguió compilar porque el sandbox bloqueó la descarga de Gradle 8.14.3.
No inferir una build verificada ni cobertura completa de accesibilidad,
rotación, tamaños de pantalla, persistencia o ciclo de vida.

Validación posterior del corte de conexiones (worklog 045): 31 tests de
`session-core` correctos y compilación directa de todos los fuentes Android
Kotlin/Compose con dependencias cacheadas. El wrapper siguió bloqueado antes
de compilar por el servicio de bloqueo/red local del sandbox. El feedback de
instalación del propietario es independiente y no verifica por sí mismo
relaciones, restauración de estado, accesibilidad o toda la persistencia.

## 6. Reglas específicas del motor de tablaturas

- El formato documentado de contenido está en `AGENT_CONTEXT.md` y en el
  modelo del frontend legado.
- Afinación: el array `tuning` va de cuerda grave a aguda.
- En una nota, `string = 1` es la cuerda más aguda; en una guitarra de seis
  cuerdas, `string = 6` es la grave. No confundir el orden con `tuning`.
- El flujo prioritario es generar el recurso con un agente, importarlo y
  estudiarlo. La edición manual es secundaria. Mantener disponibles la lectura
  continua del recurso completo y el modo Practice por bloque con sus detalles.
- El visor adapta el número de cuerdas (hasta 24) y el ancho temporal a la
  afinación y al compás compatibles. Los datos fuera del dibujo deben seguir
  disponibles en los detalles; no afirmar que se visualizan todos los campos.
- La importación valida la estructura básica y conserva campos desconocidos;
  no equivale a una validación musical completa. Mantener la información al
  importar, guardar y reabrir, incluso cuando el editor no la comprende.
- El grid del editor manual representa 16 posiciones de semicorchea en 4/4; `beat` usa
  incrementos de `0.25` desde `1` hasta `4.75`.
- Conservar `duration`, PIMA, técnicas, notas simultáneas y cualquier campo
  desconocido al editar una celda.
- Un acorde puede tener varias notas en el mismo evento. Editar una nota no
  debe eliminar las demás.
- No degradar una tablatura avanzada a los campos que entiende la UI actual.
- Las mutaciones deben ser inmutables desde `session-core` y acompañarse de
  tests de preservación del JSON.
- La importación de JSON es parte del flujo de contenido generado. La lectura
  y la práctica deben ser visuales y específicas del dominio, sin depender de
  un editor genérico de JSON.
- La implementación anterior en `muskop_front/src/components/tab/` y
  `muskop_front/src/audio/` sirve como referencia para render, reproducción y
  exportación; no copiar dependencias web al módulo Android.

## 7. Seguridad del documento de sesión

El contrato opcional `content.musical` v1 está en `RESOURCE_METADATA.md`.
Es la fuente única de metadatos musicales (conceptos, función y dificultad),
independiente de los tags de organización y de `exercise.level` heredado.
`importTabResource` conecta la validación de tablaturas con el editor; no
duplicar ese mapeo en Compose. `resourceConnections` deriva relaciones musicales
sin mutar la sesión; Library las muestra desde Connections con dificultad,
conceptos y ubicación en secciones. No inferir preparación de tags o de dos
recursos que simplemente usan el mismo concepto. El aprendizaje personal se
guarda en `resources[].learning`, separado de
`content.musical`; usar `setResourceLearningStatus` para cambiarlo. El tiempo
por recurso vive en `practiceLog[].resourceSeconds` y `resourceLastPracticedAt`,
con identidad idempotente por práctica y checkpoints en `activePractice`.
No duplicar estos acumulados dentro de los recursos ni inferir actividad antigua.

`SessionDocument` encapsula el `JsonObject` canónico. Al añadir una operación:

1. Modificar solo las claves que pertenecen a la feature.
2. Copiar el resto del objeto sin filtrarlo.
3. Añadir una prueba que demuestre que los campos ajenos permanecen intactos.
4. Persistir a través de `SessionViewModel` y `AndroidSessionRepository`.
5. No sobrescribir una sesión si la importación o validación falla.

La exportación privada disponible como referencia local es una instantánea;
no asumir que está al día. Consultar primero `../AGENTS.md`. Su ubicación es:

```text
/home/carlos/Documents/develop/muskop_develop/Muskop.muskop.json
```

Es un archivo privado de trabajo y está fuera del repositorio. Puede usarse en
modo lectura con el test opcional, pero nunca debe copiarse al repo, incluirse
en un commit, reescribirse ni volcarse entero en logs o respuestas.

## 8. Forma de trabajo con varios agentes

Cada agente debe recibir una feature bien delimitada y trabajar en su propia
rama y, preferiblemente, en su propio worktree. Convención sugerida:

```text
feat/ui-<feature>
feat/tabs-<feature>
feat/core-<feature>
fix/<problema>
```

Todas parten de la revisión acordada de `feat/android-native-migration`, no de
`main` ni de `archive/web-capacitor-v1`.

Antes de modificar:

1. Leer este archivo y los archivos directamente relacionados con la feature.
2. Revisar `git status`; los cambios existentes pertenecen a otro trabajo y no
   deben descartarse, reformatearse ni incluirse accidentalmente.
3. Declarar qué archivos o área va a poseer el agente.
4. Si dos features necesitan el mismo archivo coordinador —por ejemplo
   `MuskopApp.kt`, `MuskopTheme.kt` o el contrato de sesión—, acordar primero
   quién lo modifica. El otro agente debe entregar un cambio aislable.

Durante la implementación:

- Mantener el alcance de la feature; evitar refactors generales no solicitados.
- Hacer cambios pequeños y revisables.
- No actualizar Gradle, Kotlin, Compose o `compileSdk` como efecto colateral.
- No borrar código o datos de otra feature para resolver un conflicto.
- No editar artefactos generados de `.gradle/`, `.kotlin/`, `build/` o `.idea/`.
- No añadir dependencias si Compose/Kotlin/Android ya ofrecen una solución
  razonable.

Al entregar, el agente debe indicar:

- Resultado funcional.
- Archivos modificados.
- Decisiones y riesgos.
- Tests y comandos ejecutados, diferenciando claramente revisión estática de
  compilación real.
- Pruebas manuales pendientes.
- Posibles conflictos con otros agentes.

La integración entre ramas debe hacerla un agente coordinador después de
revisar el diff y ejecutar las validaciones comunes.

## 9. Worklog de entregas

El histórico de la migración se guarda fuera del repositorio en:

```text
/home/carlos/Documents/develop/muskop_develop/worklog/
```

Cada implementación terminada debe añadir un Markdown numerado con el siguiente
ordinal disponible, por ejemplo `029-tab-renderer.md`. Debe contener objetivo,
implementación, verificación, riesgos y siguiente prueba manual. No reescribir
entradas anteriores.

## 10. Build y validación

Versiones actuales relevantes:

- Gradle Wrapper `8.14.3`.
- Android Gradle Plugin `8.13.0`.
- Kotlin `2.2.21`.
- Compose BOM `2025.09.00`.
- `compileSdk`/`targetSdk` 36, `minSdk` 24.
- Gradle se ejecuta con JDK 21; bytecode objetivo Java/Kotlin 17.

Usar siempre el wrapper, no el Gradle global 9.x:

```bash
cd /home/carlos/Documents/develop/muskop_develop/muskop/muskop_android
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :session-core:test
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleDebug
```

Con SDK no detectado:

```bash
ANDROID_HOME="$HOME/Android/Sdk" \
ANDROID_SDK_ROOT="$HOME/Android/Sdk" \
JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
./gradlew :app:assembleDebug
```

APK resultante:

```text
muskop_android/app/build/outputs/apk/debug/app-debug.apk
```

Validación mínima por feature:

1. `git diff --check`.
2. Tests de `session-core` si se modifica modelo, JSON o lógica de dominio.
3. `:app:assembleDebug` si se modifica la aplicación Android.
4. Prueba manual del flujo y de un estado vacío/error, cuando corresponda.
5. Comprobar modo claro/oscuro y pantalla estrecha para cambios de UI.

Si el sandbox impide escribir locks en `~/.gradle`, no afirmar que la build fue
correcta: registrar la limitación y proporcionar el comando exacto para que el
propietario la ejecute.

## 11. Problemas conocidos que no deben repetirse

- El Gradle global 9.8 no es compatible con el AGP actual. Usar `./gradlew`.
- Java 27 provoca `IllegalArgumentException: 27` en el compilador Kotlin usado
  por Gradle. Fijar `JAVA_HOME` a JDK 21.
- Mantener alineados los targets Java/Kotlin; actualmente ambos generan 17.
- No subir Compose a versiones que exijan API 37 o AGP 9 sin una migración
  explícita y completa.
- En Android API 24 no usar `java.nio.file.Files.writeString`; ya causó un
  `NoSuchMethodError`. Usar `File`, streams y las utilidades de
  `SessionFileStore`.
- Kotlin no siempre permite smart cast de propiedades públicas declaradas en
  otro módulo. Copiar primero a una variable local no nula.
- Los recursos Android requieren el namespace/import correcto
  `com.muskop.android.R`.
- Un `BUILD SUCCESSFUL` no demuestra por sí solo que importación, persistencia,
  temporizadores o layout funcionen en dispositivo: probar el flujo afectado.

## 12. Definition of Done

Una feature se considera lista cuando:

- Resuelve el comportamiento solicitado, no solo la apariencia superficial.
- Mantiene compatibilidad con sesiones existentes y campos desconocidos.
- Sigue la identidad visual y el inglés por defecto.
- Tiene estados vacío, error y contenido real razonables.
- No introduce regresiones evidentes en pantallas estrechas o modo oscuro.
- Incluye tests de dominio cuando modifica datos.
- Compila con el wrapper y JDK 21, o documenta de forma explícita por qué no se
  pudo ejecutar la build.
- Deja un worklog y una entrega clara para el agente coordinador.
