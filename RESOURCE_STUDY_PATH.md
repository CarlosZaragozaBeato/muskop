# Study path — recorrido hacia un recurso objetivo

Library → abrir el recurso objetivo → **Study path**. El recurso abierto es el
objetivo del recorrido. Puede ser una pieza, un fragmento u otro recurso que
declare conceptos con relación `uses`. Read sigue siendo la vista inicial.

## Secuencia visual

1. **Understand**: explicaciones opcionales de conceptos que usa el objetivo.
2. **Prepare**: recursos que entrenan esos conceptos. Se muestran las alternativas
   disponibles, con su dificultad y valoración personal cuando existen.
3. **Apply**: volver a la lectura del objetivo y aplicar lo trabajado, usando sus
   secciones y notas de estudio.

Cada tarjeta explica qué conceptos comparte con el objetivo y dónde aparecen
en ambos recursos. Se puede abrir, estudiar y volver al objetivo con la pestaña
y posición anteriores. El botón Read target vuelve a Read sin iniciar un reloj.

**Preparation to add** enumera los conceptos usados por el objetivo para los que
no hay un recurso que los entrene. Muestra también su ubicación en el objetivo,
incluida una indicación cuando una referencia ya no está disponible. Sirve para
pedir al agente de contenido ejercicios específicos usando la sesión exportada.

## Reglas de cálculo

- El objetivo aporta únicamente sus conceptos `uses`. Los conceptos que solo
  `trains` o `explains` no se convierten en requisitos de aplicación.
- Se reutilizan las coincidencias exactas de `resourceConnections` por id.
  Preparación: objetivo `uses` → candidato `trains`. Explicación: objetivo
  `uses` → candidato `explains`.
- Compartir un tag, categoría, título o simplemente `uses` no crea preparación.
- Un recurso que entrena varios conceptos aparece una vez con todos los que
  coinciden. Las alternativas se mantienen; no es necesario practicarlas todas.
- Las explicaciones no cuentan como ejercicios disponibles para cubrir un
  concepto. El propio objetivo nunca es su preparación.
- El orden de las tarjetas sigue Connections: cantidad de conceptos compartidos,
  título e id. No es una evaluación pedagógica ni una recomendación de dificultad.
- El mensaje «X of Y listed concepts have preparation» describe material en la
  biblioteca. No es un porcentaje de aprendizaje, dominio o preparación musical.
- Los recursos ya marcados Playable/Maintaining se muestran igualmente: el
  usuario decide si necesita repaso. El tiempo no elimina pasos automáticamente.
- Una referencia de sección ausente no se resuelve por nombre o posición.
  Se conserva la relación musical con su indicación de ubicación no disponible.

## Alcance y persistencia

Es un recorrido directo derivado de la biblioteca actual. No necesita campos
nuevos ni cambia versiones de sesión, tablatura o metadatos. `resourceStudyPath`
en session-core no modifica contenido, historial ni aprendizaje personal.
La pestaña y el desplazamiento son estado de navegación, no progreso del recorrido.

No se inventa un grafo de prerrequisitos: los metadatos existentes no declaran
todas las dependencias ni su orden pedagógico. Por eso no se recorre de forma
recursiva la preparación de cada ejercicio; tampoco se añaden pasos por compartir
habilidades generales. Las referencias mutuas no generan un bucle de recorrido.

La lista se recalcula al añadir, editar o eliminar recursos. Si faltan conceptos
aplicados o los metadatos no son interpretables, se muestra un estado vacío con
acceso a la lectura. Sin ejercicios o explicaciones, se puede leer el objetivo.
La creación de rutinas desde este recorrido queda como ampliación posterior.

## Para el agente de contenido

La guía completa es [CONTENT_AGENT_CONTEXT.md](CONTENT_AGENT_CONTEXT.md). Generar
ejercicios con `trains`, aplicaciones con `uses` y teoría con `explains`, usando
los mismos ids solo cuando hay coincidencia musical real. No añadir `uses` solo
para que aparezca una ruta si el concepto no se aplica en el recurso.

No hay formato nuevo de «ruta» para importar. Se importan los recursos con su
contrato habitual y la app deriva el recorrido. Sigue siendo responsabilidad del
usuario valorar su aprendizaje; la app no marca los pasos como completados.

Referencias: [RESOURCE_METADATA.md](RESOURCE_METADATA.md),
[RESOURCE_LEARNING.md](RESOURCE_LEARNING.md), [RESOURCE_PRACTICE.md](RESOURCE_PRACTICE.md).
