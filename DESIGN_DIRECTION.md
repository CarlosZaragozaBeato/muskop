# Muskop — Dirección visual Android

Fecha: 2026-11-12. Primera actualización de diseño cerrada por el propietario.
Alcance implementado: tema, Inicio, Rutinas/preparación, Práctica, Progreso,
Library e interior de recursos en Compose / Material 3.
Este documento conserva la dirección acordada y su evolución. Las propuestas
pendientes se distinguen de lo implementado en el cierre de la sección 13.

## 1. Intención

Una herramienta de práctica tranquila, precisa y cercana. Abrir Muskop debe
permitir entender qué hacer y empezar con pocos pasos. Durante la práctica,
la música ocupa la mayor parte de la pantalla y los controles permanecen a mano.

La personalidad viene de la tipografía, el espacio, los recursos musicales y
el naranja de marca. Superficies planas, separaciones claras y redondeos moderados.
Las estadísticas acompañan a la práctica; el detalle del progreso vive en Progress.

Mantener inglés en la interfaz y los textos del usuario en su idioma original.

## 2. Diagnóstico de la implementación

Diagnóstico inicial, anterior a los cambios de esta actualización.

Revisión estática de HomeScreen.kt, RoutinesScreen.kt, MetronomePanel.kt,
MuskopApp.kt y MuskopTheme.kt. No se han evaluado capturas ni ejecutado la app.

| Hallazgo | Consecuencia | Dirección propuesta |
| --- | --- | --- |
| Inicio coloca dos tarjetas naranjas y el objetivo antes de las acciones | Las cifras reciben la primera atención | Acción de práctica primero; resumen compacto después |
| `primary` y `primaryContainer` son el mismo naranja; algunos iconos usan ambos | Iconos sin contraste en SummaryCard y PendingPracticeCard | Superficies neutras; usar siempre el color de contenido correspondiente |
| El tema deja roles de contenedor y otros roles con valores predeterminados | No queda garantizada una paleta neutra en todos los componentes | Definir explícitamente los roles utilizados en claro y oscuro |
| Free practice y el metrónomo usan superficies de alto contraste | Compiten con el contenido y con la acción principal | Superficies neutras y acentos puntuales |
| El logo ocupa la barra global y cada pantalla añade otra cabecera | Se consume altura antes del contenido | Cabecera contextual; logo conservado en Inicio y bienvenida |
| Settings ocupa un destino inferior | Misma prioridad que tareas diarias | Cuatro destinos inferiores; Settings desde la cabecera |
| En práctica todo comparte una lista desplazable | Start/Pause puede salir de la vista | Controles persistentes y contenido musical desplazable |
| El recurso de la rutina comienza oculto | Un paso adicional para estudiar el bloque | Mostrar automáticamente el recurso asociado cuando exista |
| Play y Edit comparten peso en cada rutina | Lectura y acciones compiten por el ancho | Abrir detalle desde la tarjeta, editar desde el detalle |

## 3. Fundamentos visuales

### Color

Valores de partida; comprobar contraste y estados al implementar.
El naranja de marca se conserva. Los tonos suaves se obtienen mezclándolo con
la superficie y se reservan para selección o estados activos.

| Función | Claro | Oscuro |
| --- | --- | --- |
| Fondo | `#FFFFFF` | `#111111` |
| Superficie secundaria | `#F5F5F5` | `#1C1C1C` |
| Superficie elevada / panel | `#EEEEEE` | `#252525` |
| Texto principal | `#111111` | `#F5F5F5` |
| Texto secundario | `#595959` | `#B8B8B8` |
| Separador | `#DEDEDE` | `#383838` |
| Acento de marca | `#F2A33C` | `#F2A33C` |
| Contenido sobre acento | `#111111` | `#111111` |

Naranja para el botón principal, selección, progreso y pulso. Las métricas y
las tarjetas generales usan superficies neutras. En claro, no usar naranja
de marca para textos pequeños sobre blanco; preferir texto oscuro e indicador
naranja. Los errores incluyen icono, explicación y acción de recuperación,
sin depender del color para distinguirse.

Definir las parejas de roles de contenido y superficie, incluyendo
`surfaceContainer*`, contenedores de selección, outlines y estados. Revisar
también los colores por defecto de chips y navegación. Cada componente consume
roles semánticos del tema; no declara su propia paleta.

### Tipografía, espacio y forma

- Mantener la familia sans del sistema y la escala existente como base.
- Título de pantalla: 26/32 sp; título de sección: 20/26 sp; título de tarjeta:
  16/22 sp. Cuerpo: 14/21 o 16/24 sp. Metadatos: 12/18 sp.
- Temporizador: 38/44 sp como punto de partida, cifras de ancho estable si la
  fuente lo permite. En práctica guiada ocupa una fila compacta.
- Margen lateral: 20 dp; separación entre secciones: 24 dp; entre elementos:
  12–16 dp; separación interna corta: 4–8 dp.
- Tarjetas: radio 14 dp, padding 16 dp; usar 22 dp solo en paneles destacados.
  Mantener 8 dp para elementos pequeños. Evitar tarjetas dentro de tarjetas.
- Iconos visuales de 24 dp con objetivo táctil mínimo de 48 × 48 dp.
- Una acción principal destacada por estado. Las demás usan tratamiento tonal,
  iconos o texto según su significado; conservar etiqueta en acciones ambiguas.
- Títulos largos pueden ocupar dos líneas en listados y leerse completos en
  detalle. Con texto ampliado, permitir más altura o apilar controles.

## 4. Navegación

Barra inferior: Home · Routines · Library · Progress, con iconos y etiquetas.
Settings se abre desde un icono en la cabecera de los destinos principales.
Esta es una propuesta de navegación, pendiente de validar antes de implementarla.

Los detalles y editores tienen título contextual y volver. En práctica se
sustituye la navegación global por los controles de práctica: volver pausa y
conserva la sesión para reanudar. Verificar tanto el gesto Android como la flecha.
No modificar la persistencia como efecto colateral de un cambio de layout.

## 5. Inicio: decidir y empezar

Orden propuesto:

1. Cabecera breve con identidad y acceso a Settings.
2. Saludo y fecha discretos.
3. Si existe práctica pendiente, tarjeta principal con nombre, estado y Continue.
   Si no existe, última rutina disponible; sin historial, primera rutina
   disponible, identificada como tal, sin sugerir una recomendación inteligente.
4. Free practice como acceso compacto. Si no existen rutinas ni práctica
   pendiente, pasa a ser la acción principal.
5. Today y This week como dos cifras sobre una superficie neutra.
6. Objetivo semanal integrado debajo; detalle ampliado en Progress.

La tarjeta principal presenta el nombre, una línea de contexto y un botón
etiquetado. Su botón se coloca debajo del texto para conservar ancho de lectura.
La acción abre la preparación; el tiempo solo empieza con Start.

## 6. Rutinas: biblioteca de planes

Cabecera Routines con añadir; práctica pendiente si existe; acceso compacto a
Free practice; lista de rutinas. No añadir filtros hasta que el volumen de
contenido justifique su presencia.

Cada tarjeta: nombre, número de bloques, duración y categoría discreta si existe.
Descripción como máximo dos líneas, ampliable en detalle. Toda la tarjeta abre
la preparación. Un botón Play opcional debe tener un significado distinto:
iniciar tras la preparación, nunca arrancar el reloj silenciosamente.

Preparación: nombre completo, descripción, duración total, bloques con duración
y BPM, acción Start y edición secundaria. Una rutina sin bloques muestra qué
falta y una acción para editarla, en lugar de un Play que no funciona.

Estado vacío: “Build your first routine” con una frase breve y Create routine.

## 7. Práctica: leer, tocar y controlar

### Guiada

- Cabecera compacta con volver, nombre de rutina y “Block 2 of 4”.
- Nombre del bloque y contexto: tiempo restante, BPM y progreso.
- Recurso asociado visible como contenido principal. Conservar la lectura de
  tablatura, digitación, técnicas y acceso a detalles y al recurso completo.
- Indicaciones largas y detalles secundarios desplegables. No ocultar una
  instrucción esencial para ejecutar el bloque.
- Metrónomo en una fila compacta con BPM y activar/desactivar. Al abrirlo,
  mostrar los controles disponibles en un panel; no prometer capacidades nuevas.
- Franja inferior persistente: anterior, Start/Pause, siguiente. Debajo o en
  menú accesible, Finish practice. Al terminar cambia a Save practice.
- Terminar antes explica que se guardará práctica parcial; descartar requiere
  una acción explícita y separada de guardar.

El temporizador y el metrónomo son controles distintos: activar el metrónomo
no inicia la sesión. Los estados Ready, Practicing, Paused y Complete tienen
texto y acciones coherentes. El inicio siempre es explícito.

Sin recurso: mostrar las indicaciones del bloque en el espacio principal.
Con referencia a recurso ausente: explicar que no está disponible y permitir
seguir practicando. No confundir ausencia con un fallo de dibujo.

### Libre

Antes de empezar: habilidad y recurso opcional. Durante la práctica: tiempo
transcurrido, habilidad actual y recurso seleccionado. Cambiar habilidad o
recurso desde controles compactos, manteniendo el espacio de lectura.
Start/Pause y Finish practice permanecen accesibles. Sin recurso, el tiempo
puede ocupar más espacio; los controles conservan su posición.

### Esquema de jerarquía (no es una captura ni una medida exacta)

```text
HOME                   ROUTINES                 PRACTICE
Marca          Ajustes Routines              +   Volver · Routine
Hello, Carlos          Continue si pendiente    Block 2 of 4
Fecha                  Free practice            Block title
                                                04:32 · 80 BPM
Continuar / rutina     Your routines
Nombre                 Nombre                   Recurso musical
Contexto               4 blocks · 20 min        visible
[Continue / Practice]  Categoría
                                                Indicaciones / detalles
Free practice          Nombre
                       3 blocks · 15 min        Metronome · 80 BPM
Today      This week
Objetivo semanal                                Anterior  [Pause]  Siguiente
                                                Finish practice
Navegación inferior    Navegación inferior       Controles persistentes
```

## 8. Traslado a Compose

Primero completar MuskopTheme.kt y extraer solo los patrones repetidos:
cabecera de sección, tarjeta de acción, fila de métricas y controles de práctica.
Mantener MaterialTheme para colores, tipografía y formas; centralizar los
espaciados usados por varios componentes. No añadir dependencias para este pulido.

Usar Scaffold para reservar el espacio de controles persistentes y respetar
insets del sistema. Las listas y el visor reciben el padding correspondiente.
La tablatura conserva su desplazamiento cuando lo requiere el contenido musical;
el resto de la pantalla no debe desbordarse horizontalmente.

Antes de ocultar navegación o mover callbacks, comprobar reanudación,
temporizadores y ciclo de vida. El diseño no cambia el JSON de la sesión.

## 9. Orden de ejecución y criterios de aceptación

1. Tema y Home: validar la densidad, identidad y contraste en un primer corte.
2. Rutinas y preparación: trasladar esa misma jerarquía a contenido variable.
3. Práctica: reorganizar contenido y controles verificando el comportamiento.
4. Extender el sistema a Library, Progress y Settings.

Para cada corte: claro y oscuro; anchos 320, 360 y 412 dp; fuente al 100 % y
200 %; nombres largos; sesión vacía y con contenido; iconos etiquetados para
TalkBack. Reservar áreas táctiles sin solapamiento y comprobar contraste de
texto, iconos, bordes y selección. Evitar anunciar cada segundo del temporizador
como una actualización de accesibilidad.

En práctica comprobar que los controles permanecen visibles con un recurso
largo, pausa/reanudación conservan tiempos, volver permite continuar después y
guardar parcial/completa mantiene la semántica actual. Compilar con wrapper y
JDK 21 tras modificar Android. Una build correcta no sustituye estas pruebas.

## 10. Referencias

Las decisiones de jerarquía y paleta son propuestas específicas para Muskop.
Las bases técnicas se apoyan en la documentación oficial:

- [Material 3 en Compose](https://developer.android.com/develop/ui/compose/designsystems/material3):
  roles de color y escalas compartidas por los componentes.
- [Accesibilidad por defecto en Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults):
  objetivos táctiles de al menos 48 dp y descripciones de contenido.

Contexto local: AGENTS.md, ../GUIDE.md y el código Android revisado.

## 11. Extensión a Progress y Library

Primer corte de estas dos áreas, solicitado el 2026-11-12. Pendiente de
compilación y validación visual en dispositivo.

Progress organiza la lectura en tiempo total, actividad de los últimos siete
días, objetivos y reparto por habilidad. El total admite horas y minutos;
la gráfica conserva los minutos de cada día y una descripción accesible.
Con texto ampliado pasa a filas para mantener la legibilidad. El objetivo
semanal se muestra primero y los mensuales/anuales se despliegan.

Las habilidades muestran tiempo registrado y avance a un objetivo, cuando
existe, o porcentaje del tiempo por habilidad. El tiempo practicado no se
presenta como una evaluación del dominio musical. Los registros antiguos
sin reparto explícito indican el uso de los bloques actuales de rutina.

Library prioriza búsqueda, tipo de recurso y acceso a lectura. El botón Add
resource abre el editor existente, que permite importar tablaturas y crear
notas; su etiqueta ya no lo limita a teoría. Las etiquetas para filtrar se
despliegan a petición. Cada tarjeta tiene título, tipo/categoría y etiquetas
como metadatos, con una sola acción para abrirla. Búsqueda, filtros y posición
se conservan al volver del recurso. Las bibliotecas vacías y las búsquedas sin
resultados tienen mensajes y acciones diferentes.

El visor conserva su motor musical y sus modos Practice / Full resource.
La cabecera usa volver y editar con iconos; editar regresa al recurso al salir.

## 12. Lectura dentro del recurso

La revisión posterior prioriza el interior del recurso. La cabecera compacta
con título y acciones permanece visible; categoría, etiquetas y título completo
se consultan en About this resource. El contenido musical aparece primero.

Afinación, notas de estudio y detalles por evento usan divulgación progresiva.
Cuando el diagrama no puede mostrar los datos, los detalles correspondientes
se abren para que la limitación no oculte su contenido. Se elimina el scroll
vertical anidado de los detalles.

Practice y Full resource mantienen su función. El zoom se despliega a petición
y escala el espacio del dibujo junto con sus textos; los números sobre naranja
usan el color oscuro de contenido. Los cambios del visor compartido también
se aplican al leer un recurso desde una práctica. La validación en dispositivo
de este corte sigue pendiente.

## 13. Cierre de la primera actualización

El propietario da por terminada esta primera actualización el 2026-11-12:
ha probado los cambios y los considera buenos por ahora. Ha valorado
favorablemente Progreso y, tras pedir centrar Library en el interior del recurso,
ha probado también esa revisión. Quiere usar más la app antes de continuar
el diseño en el futuro.

Quedan implementados los cortes de los worklogs 038–042: tema/Inicio,
Rutinas/preparación, Práctica con controles fijos, Progreso/Library y lectura
del recurso. La sección 4 conserva una propuesta pendiente: no se ha reducido
la navegación global a cuatro destinos ni se ha movido Settings a la cabecera.

Los avisos de validación pendiente en secciones anteriores describen el estado
al entregar cada corte. Este cierre añade feedback manual inicial favorable del
usuario; no acredita una matriz completa de dispositivos o accesibilidad.
La revisión estática y `git diff --check` pasaron. Los intentos de build de este
chat fallaron antes de compilar por el bloqueo de red al descargar Gradle 8.14.3;
no se ejecutaron pruebas automatizadas ni se verificó un APK desde este entorno.

Para retomar: partir del uso real y de observaciones concretas del propietario,
conservar esta base visual y comprobar cada cambio del visor también desde
práctica libre/guiada. No reiniciar una revisión general por el mero hecho de
encontrar propuestas pendientes en este documento.

## 14. Recursos relacionados y feedback posterior

Tras el cierre visual se añadió Library → recurso → **Connections**, junto a
Read. Muestra dificultad, conceptos y tarjetas de preparación, aplicaciones y
explicaciones. Mantiene las superficies del tema y la lectura como vista inicial.
La navegación entre recursos conserva el recorrido de vuelta. El visor compartido
de práctica no incorpora controles de conexiones.

El propietario comunica que ha instalado la app y funciona bien, pero todavía
no puede comprobar las relaciones entre varios recursos porque no tiene contenido
adicional suficiente. Mantener esta comprobación como pendiente; no registrar
los tags/conexiones como validados en dispositivo por esa observación general.

El corte de conexiones pasó 31 tests del núcleo y compilación directa de todos
los fuentes Kotlin/Compose. Gradle no llegó a compilar por restricciones del
servicio de bloqueo/red local. Esos resultados posteriores no reescriben el
histórico de validación de la primera actualización visual.

La guía para generar material a partir de una sesión está en
[CONTENT_AGENT_CONTEXT.md](CONTENT_AGENT_CONTEXT.md). El siguiente paso de producto
es aprendizaje personal e historial por recurso; no se ha implementado en este
cierre de documentación.

## 15. Aprendizaje personal por recurso

Feedback posterior del propietario: ha probado las conexiones con nuevos
recursos que utilizan los metadatos musicales y comunica que son correctas.
Esto resuelve la prueba de contenido pendiente descrita en la sección 14 para
su uso inicial; no acredita todos los casos de restauración o accesibilidad.

Se añade un control compacto **Learning status** bajo la cabecera del recurso,
disponible en Read y Connections. Abre un diálogo con opciones de radio,
Save y Cancel. Not assessed mantiene la ausencia de valoración distinta de
Not started; también permite quitar una valoración previa. La biblioteca y
las tarjetas de conexiones muestran el estado cuando existe. Textos amplios
pueden envolver y el diálogo permite desplazamiento vertical.

Es una autoevaluación manual: no cambia por tiempo, dificultad o conexiones.
No modifica el visor compartido de práctica ni añade controles al temporizador.
Persistencia y significado están en [RESOURCE_LEARNING.md](RESOURCE_LEARNING.md).
El estado manual todavía requiere comprobación del propietario en dispositivo.

## 16. Práctica sin recurso y selección en Free practice

La siguiente revisión se centra en el espacio de práctica cuando no hay un
recurso visible. Free practice muestra un panel de tiempo destacado con estado
y habilidad; un bloque de rutina sin recurso muestra tiempo restante y avance.
Los controles fijos siguen disponibles, y las instrucciones del bloque no
se sustituyen por el panel.

El selector de recursos de Free practice pasa de una fila de todos los títulos
a una acción Add a practice resource. Abre un diálogo con búsqueda por título,
categoría, etiquetas y tipo, filtro de tipo y tarjetas legibles. Permite cambiar
o quitar la selección y practicar sin material visible. Seleccionar contenido
no inicia ni pausa el reloj. La selección es estado visual; no añade atribución
de tiempo o historial por recurso al documento de sesión.

Lo anterior describe ese corte del selector. La persistencia y atribución se
añadieron después en el paso documentado a continuación.

El filtrado de este corte corresponde a recursos dentro de Free practice,
interpretación comunicada mientras se esperaba aclaración del propietario.
No añade un buscador de rutinas guardadas. Requiere revisión visual en dispositivo.

## 17. Tiempo y última práctica por recurso

La selección de Free practice pasa a conservarse al reanudar y atribuye segundos
al recurso mientras el contador está en marcha. Se mantiene el selector existente
y se añade Hide resource / Show resource para practicar de memoria sin quitarlo.
En rutinas se usa el recurso del bloque actual. Pasar la app al fondo pausa la
práctica y guarda un checkpoint; volver exige Continue.

Library muestra Recorded practice con total, número de prácticas y última fecha
local. En Read aparece después del contenido musical; en Connections antes de
las relaciones. Es solo tiempo guardado: no incluye la pendiente ni convierte
registros antiguos sin atribución en tiempo por recurso. El aprendizaje personal
permanece manual. Contrato: [RESOURCE_PRACTICE.md](RESOURCE_PRACTICE.md).

Este corte pasó 46 tests del núcleo y compilación directa Kotlin/Compose. Gradle
no arrancó por el lock de solo lectura. Quedan pendientes APK y prueba en
dispositivo, incluyendo pausa/fondo, cambio de recurso/bloque y reanudación.

## 18. Lectura continua mientras se toca

El propietario necesita seguir leyendo sin cambiar manualmente de compás.
Full resource pasa a ser el modo inicial, con compases distribuidos en filas
según ancho y zoom. Los compases densos continúan en partes numeradas para
evitar notas ocultas a la derecha; su contenido y duración no se modifican.
Practice conserva el recorrido por bloques.

El icono de pantalla completa, junto al zoom, abre una hoja de lectura con
todas las secciones. Oculta los detalles secundarios inicialmente, mantiene
la pantalla encendida mientras está abierta y ofrece zoom y desplazamiento
automático opcional con tres velocidades. Arrastrar pausa el desplazamiento,
igual que alcanzar el final o mandar la aplicación al fondo. Es una ayuda de
lectura independiente del reloj de práctica, el metrónomo y la reproducción.

Los recursos cortos pueden verse juntos; los largos siguen necesitando recorrido
y pueden leerse con desplazamiento automático. No se promete que cualquier
recurso quepa legiblemente en una sola pantalla. En modo completo los fragmentos
conservan las notas por cuerda y posición temporal; digitación, técnicas y otros
campos compatibles siguen consultándose desde los detalles.

La comprobación de este corte es de fuentes Kotlin/Compose y tests del núcleo;
quedan pendientes la generación del APK y la prueba de lectura en dispositivo.

## 19. Recorrido hacia un recurso objetivo

El detalle añade Study path junto a Read y Connections, en la misma fila
desplazable. Mantiene Read como entrada y conserva posición por pestaña al
abrir recursos relacionados y regresar. El visor compartido de práctica no cambia.

La secuencia Understand → Prepare → Apply agrupa explicaciones opcionales,
ejercicios disponibles y acceso a la lectura del objetivo. Reutiliza tarjetas
de Connections con dificultad, autoevaluación y ubicaciones de los conceptos.
Preparation to add muestra los conceptos usados sin ejercicio correspondiente;
la cifra de conceptos con preparación describe la biblioteca, no el dominio.

No se obliga a realizar todas las alternativas ni se marca progreso automático.
El recurso abierto es el objetivo y el recorrido se deriva de relaciones
directas de la biblioteca. La creación de una rutina desde el recorrido queda
para otra entrega. Contrato: [RESOURCE_STUDY_PATH.md](RESOURCE_STUDY_PATH.md).

Validación: 57 tests del núcleo y compilación directa de todos los fuentes
Kotlin/Compose. Gradle se detuvo antes de compilar por el lock de solo lectura.
Pendiente APK, prueba en dispositivo, temas y tamaños de pantalla.

## 20. Porcentaje manual de dominio

Learning status incorpora una valoración opcional de 0–100 %, activada desde
Assess mastery percentage y ajustada con un slider en pasos de 1 %. Sin valorar
se distingue de 0 %. El diálogo desplazable mantiene Save/Cancel; mover el slider
no escribe la sesión hasta guardar. Estado y porcentaje se guardan juntos cuando
se editan ambos, conservando los campos que no se han tocado.

La cifra aparece en la biblioteca, tarjetas de Connections y valoración del
objetivo en Study path. No se convierte a estados ni se deriva de la dificultad,
los minutos o la cantidad de conceptos con preparación disponible. Es una
autoevaluación, no una medida automática de dominio. Pendiente prueba de slider,
cancelación, valores extremos, tamaño de texto y temas en dispositivo.

## 21. Lectura de tablaturas con PIMA

Primera entrega aprobada tras la revisión de lectura: PIMA de mano derecha,
espacio y contraste. Los trastes pasan a texto de alto contraste sobre fondo
neutro; una pequeña etiqueta naranja debajo muestra el dedo indicado. Se
reserva más altura para las letras, incluidas notas simultáneas en varias cuerdas.
La leyenda y Show/Hide PIMA están disponibles en la lectura completa y por compás.

Las partes de un compás se calculan por ataques completos y separación mínima,
con guías de pulso acordes al espacio adaptado. La lectura compartida lleva el
cambio a pantalla completa, Library y práctica libre/guiada. Se conserva el
scroll horizontal para casos donde el zoom hace imposible encajar una fila.

No se inventa digitación ni se añaden todavía figuras rítmicas, técnicas gráficas
o mano izquierda. Los detalles siguen conservando esa información y la que el
diagrama no representa. Ver [TAB_READING.md](TAB_READING.md). Pendiente prueba
visual en dispositivo con temas, fuente ampliada y los recursos del propietario.

## 22. Organización y prioridad de recursos

Library añade Active/Hidden/Completed/All resources para apartar material sin
eliminarlo. Active es la entrada inicial y la prioridad High/Normal/Low ordena
las tarjetas de forma estable. El detalle ofrece Organize con un diálogo de
estado/prioridad y Save/Cancel, dentro del contenido desplazable.

Los selectores de práctica libre y edición de rutina comparten búsqueda,
filtros y prioridades; abren en Active. La selección actual conserva su etiqueta
aunque se haya ocultado/completado, y una rutina ya vinculada sigue mostrándola.
No se reordenan bloques ni se convierte Completed a 100 % de dominio. Los
vínculos musicales siguen consultándose desde Connections/Study path.

Las vistas vacías permiten mostrar todos los recursos y recuperar clasificación.
Contrato en [RESOURCE_ORGANIZATION.md](RESOURCE_ORGANIZATION.md). Este corte pasó
79 pruebas del núcleo y compilación directa Kotlin/Compose; queda pendiente APK
y revisión del flujo en dispositivo, incluidos filtros, temas y texto ampliado.

## 23. Importar varios recursos

Library ofrece Import resources mediante un icono de subida en cabecera y una
acción en el estado vacío. El diálogo permite elegir varios archivos o pegar
JSON, revisar títulos/tipos y confirmar el lote. Los errores identifican el
archivo o elemento; no se guarda una selección parcialmente válida. El editor
individual sigue disponible desde Add resource.

La vista previa avisa de títulos ya existentes y de recursos fuera de Active.
Tras guardar se comunica el número añadido y se restablecen búsqueda/filtros a
Active; All resources permite encontrar los ocultos/completados importados.
Durante el guardado se bloquean el cierre y la doble pulsación. Se puede cerrar
durante la revisión/lectura sin guardar. El borrador no usa SavedState por su
tamaño; una recreación puede requerir seleccionar/pegar otra vez.

Contrato y límites en [RESOURCE_IMPORT.md](RESOURCE_IMPORT.md). Validación:
88 tests del núcleo y compilación directa de los fuentes Android con Compose.
El wrapper quedó bloqueado por escritura en su caché; APK y prueba del selector
múltiple, persistencia, recreación, temas y pantalla estrecha quedan pendientes.

## 24. Resumen de práctica y autoevaluación

Al guardar una práctica libre, rutina completa o parcial se abre Practice summary.
Conserva el foco sin navegación global hasta Done/Back: tarjeta de duración y
resultado, Day overview con fecha explícita y recursos practicados en tarjetas
verticales. Incluye tiempo no atribuido y un estado vacío de recursos. No se
presentan los minutos previstos como tiempo practicado ni una nota de dominio.

Cada recurso muestra su valoración actual mediante el mismo Learning status y
slider usados en Library. Actualizar es opcional y no modifica el tiempo ni la
organización. Se muestran guardado/error de valoración. No se exige rellenar una
valoración para terminar. Los textos fluyen en columnas para anchos estrechos.

El guardado de práctica precede a la confirmación; error con Retry save o Return
to practice, conservando el checkpoint exacto de UI y volviendo pausada. Durante
el guardado no se abandona la pantalla. Contrato en
[PRACTICE_REVIEW.md](PRACTICE_REVIEW.md). Pendiente comprobación real en dispositivo
de los estados de error/recreación, accesibilidad, fuente ampliada y ambos temas.
