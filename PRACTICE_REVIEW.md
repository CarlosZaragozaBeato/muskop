# Resumen al finalizar la práctica

Free practice → Finish, rutina completa → Save practice y rutina parcial →
Save practice abren **Practice summary**. El temporizador y metrónomo se detienen
al salir de la pantalla activa. Salir con Back sigue dejando la práctica pendiente;
no abre un resumen ni registra una práctica completada.

## Contenido

- Nombre, duración exacta, práctica libre/rutina completa/parcial y número de
  recursos con tiempo registrado. Los bloques saltados no suman tiempo previsto.
- Tiempo sin recurso vinculado, si lo hubo. Elegir un recurso sin tocar el
  temporizador no lo convierte en practicado.
- Day overview: fecha de guardado, tiempo acumulado, número de prácticas,
  recursos distintos y rutinas completadas del día. Incluye esta práctica una vez.
  No existe un objetivo diario; no se inventa una puntuación de cumplimiento.
- Practiced resources: segundos de esta práctica, tiempo acumulado y número de
  prácticas guardadas; organización actual y control Learning status con el
  porcentaje de mastery. Incluye recursos ocultos/completados si se practicaron.
- Un recurso ausente o con identidad ambigua mantiene su tiempo histórico, pero
  no permite editar la valoración. Una práctica sin recursos sigue teniendo
  resumen y acumulado diario.

Se usa la fecha de `practiceLog[].date`, el día de guardado, también al cruzar
medianoche o revisar tras una recreación. La fecha se muestra explícitamente;
no se reparte retrospectivamente la sesión entre varios días. `seconds` positivo
prevalece sobre los minutos heredados; logs antiguos sin atribución no generan
recursos practicados ficticios. Los ids de práctica repetidos se cuentan una vez,
mientras registros históricos sin id mantienen su identidad por posición.

## Guardado y aprendizaje

El resumen confirma éxito solo cuando el registro ya está en la sesión publicada
después de `repository.save`. Mientras guarda se bloquea salir. Si falla se muestra
el error y se permite Retry save con el mismo id o Return to practice, pausada y
con el checkpoint exacto conservado en UI. Reintentos/recreaciones no duplican el
log: se mantiene `practiceId` y se usa `recordPractice` idempotente con el mutex
existente. El guardado sigue usando SessionFileStore; no se introduce otra base.

Learning status reutiliza la valoración existente: estado y maestría opcional
0–100 %, con ausencia distinta de cero. Save modifica solo los campos editados
en `resources[].learning` y conserva contenido, organización y log. No se aumenta
maestría por tiempo, no se cambia Completed ni se genera una valoración histórica
de cada práctica. El resumen muestra la valoración **actual**. Los errores de
valoración se muestran en el resumen y permiten volver a abrir el control;
la práctica ya guardada permanece guardada. Done/Back cierran el resumen sin
exigir una valoración, y no se habilitan durante un guardado de aprendizaje.

La entrada por guardar y el checkpoint de retorno se conservan con
`rememberSaveable` (datos serializables, sin contenido musical). Esto permite
recrear la pantalla y consultar primero el id ya guardado antes de reintentar.
No garantiza recuperación del borrador tras force-stop, borrado de la tarea o
terminación sin SavedState; el historial persistido y los checkpoints existentes
siguen siendo la recuperación durable. La prueba JVM de serialización no
sustituye una prueba de ciclo de vida real Android.

## Implementación y comprobación

- `session-core/PracticeReview.kt`: proyección sin mutaciones de log/biblioteca.
- `practice/PracticeSummaryScreen.kt`: resumen común, reintentos y autoevaluación.
- `RoutinesScreen.kt`: transición con entrada/id y checkpoint, vuelta pausada.
- `SessionViewModel.kt`: guardados con resultado explícito e inline errors.

Prueba manual pendiente: práctica libre cambiando de recurso y sin recurso,
rutina completa/parcial, pausas, repetición del mismo recurso en varios bloques,
fallo/reintento de persistencia, Back, rotación al guardar y al valorar, reabrir/
exportar, recursos ocultos/ausentes, medianoche, temas y fuente ampliada. Ver
[RESOURCE_PRACTICE.md](RESOURCE_PRACTICE.md) y [RESOURCE_LEARNING.md](RESOURCE_LEARNING.md).
