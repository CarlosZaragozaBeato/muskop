# Resource practice time and last practice — step 3b

El tiempo por recurso se registra desde esta versión, tanto en práctica libre
como guiada. Los datos viven en el log de la sesión; los recursos no mantienen
un contador duplicado. La sesión sigue siendo `muskopSession: 2`.

## Comportamiento

- Práctica libre: elegir un recurso con el selector existente. Cambiarlo o
  quitarlo afecta solo a los segundos siguientes, nunca al tiempo anterior.
- **Hide resource · practice from memory** oculta la lectura y mantiene el
  recurso seleccionado para atribuirle tiempo; Show resource vuelve a mostrarlo.
- Rutinas: cada segundo corresponde al recurso del bloque actual si existe.
  Saltar o volver a un bloque no cuenta su duración prevista como practicada.
- Sin recurso o con referencia ausente: el tiempo total/por habilidad se conserva
  y ese tramo queda sin atribuir a un recurso.
- Se cuentan ticks de un segundo mientras la práctica está en marcha. Pausar,
  completar o mandar la app al fondo detiene el contador. Volver a primer plano
  no lo arranca; hay que pulsar Continue.
- La práctica pendiente guarda selección, modo de lectura y tiempos. Se hacen
  checkpoints cada 15 segundos y al pausar, salir, pasar al fondo, cambiar la
  selección/modo/habilidad o cambiar de bloque. Una terminación abrupta puede
  perder los segundos posteriores al último checkpoint; no se inventa tiempo
  durante el intervalo sin app.
- **Recorded practice** en el detalle de Library muestra tiempo acumulado,
  número de prácticas guardadas y fecha local de la última práctica del recurso,
  tanto en Read como en Connections. Una práctica pendiente aún no se incluye.
- Guardar práctica parcial cuenta solo lo transcurrido; descartar una pendiente
  no añade tiempo al historial. El estado manual de aprendizaje no cambia.

Al finalizar y guardar, Practice summary presenta el registro, los recursos y el
acumulado del día, con actualización manual opcional de aprendizaje/maestría.
La autoevaluación es un guardado separado y no altera los tiempos. Contrato en
[PRACTICE_REVIEW.md](PRACTICE_REVIEW.md).

## Campos nuevos del log

Cada entrada nueva de `practiceLog` conserva los campos previos y añade:

```json
{
  "id": "practice-uuid",
  "date": "2026-11-12",
  "routineId": "free",
  "routineName": "Free practice",
  "minutes": 1,
  "seconds": 45,
  "completed": false,
  "skillMinutes": {"general": 1},
  "resourceSeconds": {"resource-a": 20, "resource-b": 15},
  "resourceLastPracticedAt": {
    "resource-a": "2026-11-12T12:00:20Z",
    "resource-b": "2026-11-12T12:00:35Z"
  }
}
```

Ejemplo: 45 segundos totales, de los cuales 35 están atribuidos y 10 se
practicaron sin recurso. `minutes`/`skillMinutes` mantienen la compatibilidad
del formato previo; para duración exacta del registro usar `seconds` positivo.
El mínimo de un minuto heredado no debe convertir 45 segundos en 60.

- `id` identifica la práctica y hace idempotente su guardado. No se reutiliza
  para otra práctica. Reintentar Save con ese id no duplica el registro.
- `resourceSeconds`: id de recurso → segundos enteros positivos. La suma no
  puede superar la duración total. No se exige que cada id siga en la biblioteca:
  su registro histórico se conserva aunque el recurso desaparezca.
- `resourceLastPracticedAt`: id → instante ISO UTC del último tick atribuido.
  Hay una fecha por recurso con tiempo positivo. No es la hora de guardar ni
  la fecha de cambiar la autoevaluación.
- Los campos son repartos del mismo tiempo, no actividad que se sume de nuevo.
  `skillMinutes` y `resourceSeconds` no determinan una matriz recurso/habilidad.
- `date` sigue siendo el día de guardado utilizado por el log existente. La
  última práctica por recurso usa su instante real incluso al cruzar medianoche.

## Práctica pendiente y reanudación

`activePractice` añade `practiceId` (el mismo que `practiceLog[].id` al guardar),
`resourceSeconds`, `resourceLastPracticedAt`, `selectedResourceId` opcional y
`showResource` booleano. Mantiene `elapsedBySkillSeconds`, bloque y tiempo restante.

Un checkpoint sustituye la instantánea de esa práctica, no suma otra copia de
sus segundos. Al reanudar se continúa desde sus mapas acumulados. Los campos
desconocidos de una instantánea de la misma práctica se conservan. Un autosave
tardío de una práctica ya guardada se ignora para no resucitarla. Un reintento de
guardado antiguo no borra otra práctica pendiente con distinta identidad.

Los estados de tiempo/bloque también usan SavedState de Android para recreaciones
de UI; no constituyen un segundo historial y nunca se suman al log por separado.

## Compatibilidad y consulta

Los registros previos sin atribución permanecen intactos y **sin atribución**.
No reconstruir segundos por recurso a partir de rutinas actuales, tags,
conceptos compartidos o estimaciones. Al reanudar una pendiente antigua, el
tiempo previo continúa sin asignar y solo los ticks nuevos reciben recurso.

`resourcePracticeSummary(id)` deriva total, última fecha válida y número de
prácticas a partir del log; ignora la pendiente y deduplica ids de prácticas.
No depende del título o de los bloques actuales. La consulta sigue funcionando
por id aunque el recurso ya no exista. Una fecha importada inválida no impide
mostrar el tiempo explícito, pero no se usa como última fecha.

Las mutaciones validan atribución y preservan contenido, valoraciones, registros
antiguos y campos de otras features. No se copian minutos a canciones relacionadas
ni se infiere dominio. El agente de contenido puede utilizar estos datos como
actividad registrada explícita; debe distinguirla de la ausencia de datos antiguos.

Referencias: [RESOURCE_LEARNING.md](RESOURCE_LEARNING.md),
[RESOURCE_METADATA.md](RESOURCE_METADATA.md),
[CONTENT_AGENT_CONTEXT.md](CONTENT_AGENT_CONTEXT.md).
