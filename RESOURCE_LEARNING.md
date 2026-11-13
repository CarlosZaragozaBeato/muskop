# Personal resource learning status

Estado manual de aprendizaje por recurso. Es una valoración del usuario,
independiente de dificultad, conceptos musicales, XP y tiempo de práctica.

## Uso en Android

Library → abrir recurso → **Learning status** → elegir → **Save**.
El diálogo permite cancelar sin cambios. El control está disponible tanto en
Read como en Connections. La biblioteca y las tarjetas de conexiones muestran
la valoración cuando existe.

El mismo diálogo incluye **Assess mastery percentage**. Activarlo permite
ajustar un slider de 0 a 100 %, en pasos de 1 %. Save guarda el borrador completo
en una sola operación; Cancel no modifica la sesión. Desactivar el porcentaje
quita esa valoración. Sin valorar y 0 % son estados distintos. El porcentaje se
muestra también en Library, Connections y la valoración del objetivo de Study path.

Estado y porcentaje son independientes: 100 % no cambia el estado a Playable o
Maintaining, y quitar el estado no borra el porcentaje. Ambos reflejan una
autoevaluación manual, sin cambios automáticos por tiempo o dificultad.

| Valor JSON | Interfaz | Interpretación orientativa |
| --- | --- | --- |
| `not-started` | Not started | El usuario declara que aún no lo ha empezado |
| `learning` | Learning | Lo está aprendiendo |
| `playable` | Playable | Considera que puede tocarlo o aplicarlo |
| `maintaining` | Maintaining | Lo mantiene mediante repaso |
| Ausente o `null` | Not assessed | No hay valoración; no implica que no lo haya practicado |

Se puede pasar entre estados en cualquier dirección y quitar la valoración.
La práctica no cambia el estado automáticamente. Los recursos relacionados
tampoco heredan la valoración del recurso practicado.

## Contrato de sesión

La sesión sigue siendo `muskopSession: 2`. El campo opcional `learning` pertenece
al objeto recurso, **fuera de `content` y de `content.musical`**:

```json
{
  "id": "resource-id",
  "title": "Study",
  "type": "TAB",
  "content": {"version": 2, "title": "Study", "sections": []},
  "learning": {"status": "learning", "masteryPercent": 45, "updatedAt": "2026-11-12T12:00:00Z"}
}
```

El ejemplo es un objeto recurso de sesión, no una tablatura independiente.
La importación individual de tablaturas no copia una valoración personal del
contenido generado. En recursos nuevos, dejar `learning` ausente salvo que el
usuario haya comunicado expresamente su valoración.

- `status`: uno de los cuatro valores o `null` para quitar la valoración.
- `masteryPercent`: entero opcional entre 0 y 100. Ausente o null significa
  porcentaje sin valorar, no cero. No se deduce a partir de `status` ni viceversa.
- `updatedAt`: instante ISO-8601 UTC del último cambio de valoración. No es la
  fecha de última práctica ni un historial de todas las transiciones.
- Guardar el mismo estado no cambia la fecha. Quitar la valoración conserva
  el objeto con `status: null`, su nueva fecha y cualquier campo desconocido.
- La operación actualiza también `user.updatedAt`, igual que otras mutaciones
  de sesión. No modifica contenido musical, rutinas, práctica pendiente, XP o log.
- No se migran los recursos antiguos ni se deduce su estado. Los valores
  desconocidos se conservan al importar/exportar o editar el recurso. El lector
  no los trata como uno de los estados conocidos; el control indica Unavailable
  y permite sustituirlos mediante una elección explícita.
- La mutación conserva campos adicionales dentro del objeto `learning`. Si ese
  campo tiene una forma no compatible, una elección explícita lo reemplaza por
  un objeto compatible; abrir/cancelar no lo modifica.
- Se rechaza actualizar una referencia ausente o ambigua de recurso.

## Desarrollo y generación de contenido

`SessionResource.learning` conserva el valor JSON original. `learningStatus()`
es una proyección tolerante, igual que `masteryPercent()`. `updateResourceLearning`
guarda estado y/o porcentaje de forma inmutable con indicadores de qué campos se
han editado. Esto conserva valores desconocidos del otro campo. El setter previo
`setResourceLearningStatus` sigue cambiando solo el estado y conserva el porcentaje.
Los porcentajes nuevos fuera de 0–100 se rechazan; los importados no interpretables
se conservan sin convertirlos o limitarlos silenciosamente.
Android persiste a través de SessionViewModel y
AndroidSessionRepository. No añadir una segunda fuente de verdad en la UI.

El agente de contenido puede utilizar esta valoración para ajustar propuestas,
identificándola como declaración del usuario. No debe interpretarla como una
evaluación objetiva o inventar valoraciones a partir del historial.
Conservarla al devolver una copia actualizada de la sesión.

El tiempo y última práctica por recurso se añadieron después, según
[RESOURCE_PRACTICE.md](RESOURCE_PRACTICE.md), sin cambiar automáticamente esta
valoración. El recorrido directo hacia un objetivo está en
[RESOURCE_STUDY_PATH.md](RESOURCE_STUDY_PATH.md). No se añade evaluación automática
de BPM dominado ni se usa el porcentaje personal para completar sus pasos.
