# Resource visibility, completion and priority

Library → abrir recurso → **Organize**. Guardar cambia su clasificación personal
sin borrarlo ni modificar su contenido musical. Cancel no cambia la sesión.

## Estados y listas

| Estado | Uso |
| --- | --- |
| Active | Material que se quiere tener a mano para practicar |
| Hidden | Apartado de las listas iniciales sin eliminarlo |
| Completed | Marcado como completado y apartado de las listas iniciales |

Library y los selectores de recursos muestran **Active** por defecto. Los filtros
Hidden, Completed y All resources permiten recuperar el material y devolverlo a
Active desde Organize. Library conserva el filtro al volver de un recurso; el
selector de práctica/rutinas abre de nuevo en Active.

La prioridad tiene tres valores: **High**, **Normal**, **Low**. Las listas de
cada filtro se ordenan por prioridad descendente, conservando el orden anterior
en los empates. Los recursos antiguos tienen clasificación efectiva Active y
Normal sin necesidad de reescribirlos. Las etiquetas High/Low y Hidden/Completed
aparecen en las tarjetas y en la selección actual.

El editor de rutinas usa el mismo selector con búsqueda, tipos y filtros que
práctica libre. Así se encuentran primero los recursos activos prioritarios.
La prioridad no cambia el orden de los bloques ya definidos en una rutina.

Ocultar/completar no rompe vínculos ni borra el historial o la práctica pendiente.
Una rutina existente sigue mostrando el recurso asociado, aunque esté oculto o
completado; el selector identifica esa clasificación. También puede elegirse
explícitamente material apartado usando otro filtro. Connections y Study path
conservan las relaciones musicales: esta clasificación filtra las listas iniciales
de Library y elección, no elimina recursos del grafo ni de la biblioteca guardada.

Completed no significa automáticamente 100 % de dominio y el porcentaje 100 %
no completa/oculta un recurso. Estado de aprendizaje, dominio, tiempo y esta
organización son datos personales independientes.

## Contrato

Campo opcional `organization` en el objeto recurso, fuera de `content` y
`learning`. No cambia `muskopSession: 2`:

```json
{
  "status": "active",
  "priority": "high",
  "updatedAt": "2026-11-12T12:00:00Z"
}
```

El ejemplo representa solo `resources[].organization`, no un archivo importable.
`status` admite active/hidden/completed; `priority` admite high/normal/low.
`updatedAt` es la fecha de la clasificación, no la última práctica. Los campos
ausentes/null reciben los valores efectivos por defecto; no se almacenan hasta
que se editan. Las actualizaciones parciales conservan el otro campo y claves
desconocidas, actualizando también user.updatedAt. Guardar lo mismo no cambia fecha.

Los valores futuros no interpretables se conservan y se accede al recurso desde
All resources; un estado desconocido no entra en Active por suposición. La
prioridad desconocida usa orden neutral y el diálogo indica que no está disponible.
Una edición explícita reemplaza la forma no compatible del bloque si no es un
objeto; abrir y cancelar no altera nada. Se rechazan ids ausentes/ambiguos.

`organizationStatus()` y `resourcePriority()` proyectan sin mutar. La operación
`updateResourceOrganization` pertenece a session-core y persiste a través de
SessionViewModel/AndroidSessionRepository. `organizedResources` filtra/ordena
una vista, nunca la lista canónica de la sesión. No usarlo para resolver las
referencias de bloques, historial o recursos ya seleccionados.

## Generación y comprobación

El agente de contenido conserva `organization` al devolver una sesión. Planifica
con activos y atiende prioridades como preferencias; incluye material apartado
cuando lo pida el usuario. No marca completado, oculta o cambia prioridades
deduciéndolo de tiempo, dificultad, porcentaje o similitud musical.

Probar en dispositivo ocultar/completar/recuperar, prioridad High/Normal/Low,
empates, filtros combinados con búsqueda/tags/tipos, biblioteca sin activos,
selección en rutina/libre y práctica de un vínculo ya oculto. Comprobar exportar/
reabrir, Cancel, claro/oscuro y texto ampliado. El contrato es independiente de
[RESOURCE_LEARNING.md](RESOURCE_LEARNING.md) y [RESOURCE_PRACTICE.md](RESOURCE_PRACTICE.md).
