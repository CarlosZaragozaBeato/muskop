# Importación múltiple de recursos

Library → **Import resources** (icono de subida en la cabecera, también disponible
en el estado vacío). Permite seleccionar varios archivos JSON o pegar un lote,
revisar sus títulos/tipos y pulsar Import. Añade recursos a la sesión actual;
no importa ni reemplaza una sesión completa.

## Formatos admitidos

1. Varios archivos, cada uno con una tablatura v2 o un recurso TAB/THEORY.
2. Un array JSON de documentos de tablatura v2 y/o objetos recurso.
3. Un paquete portable `muskopResources: 1` con `resources` como array.
4. Un único documento/recurso, por comodidad, desde el mismo flujo.

Ejemplo mínimo de paquete con dos recursos THEORY, válido para comprobar el lote:

```json
{
  "muskopResources": 1,
  "resources": [
    {"title": "Practice note", "type": "THEORY", "content": {"kind": "theory", "body": "Keep a steady pulse."}},
    {"title": "Review note", "type": "THEORY", "content": {"kind": "theory", "body": "Repeat slowly before increasing tempo."}}
  ]
}
```

El [ejemplo completo PIMA](muskop_android/session-core/src/test/resources/fixtures/resource-bundle-v1.json)
incluye una tablatura con digitación y su explicación teórica relacionada; es
contenido de demostración sintético, no una exportación personal.

Para un recurso TAB, `content` es el documento v2 completo. Para THEORY,
`content` tiene `kind: "theory"` y `body` de texto. Los metadatos musicales
permanecen en `content.musical` según [RESOURCE_METADATA.md](RESOURCE_METADATA.md).
Los objetos recurso admiten `title`, `type`, categoría/tags opcionales y campos
adicionales. Tags del objeto recurso prevalecen sobre los del contenido; si
faltan se reutilizan los del documento. Los paquetes heredados TAB/THEORY del
frontend usan esta misma envoltura.

No se admiten otros tipos (MEDIA, COLLECTION, CHORD, SNIPPET), paquetes de
ejercicios `muskopExercisePack`, rutinas ni sesiones completas en esta entrada.
Tampoco paquetes anidados o versiones futuras. Import generated tablature del
editor individual sigue esperando un documento TAB; los lotes van a la entrada
Import resources de Library.

## Validación, identidad y conservación

- Se valida todo antes de guardar. Si falla un archivo o elemento, el mensaje
  indica su nombre/posición y no se importa una parte de la selección.
- Máximo 100 recursos, 100 archivos, 2 MB por archivo o texto pegado y 10 MB
  entre todos los archivos y entre los recursos preparados.
- TAB usa la validación de importTabResource, incluidos metadatos y referencias
  de sección. THEORY valida kind/body y los metadatos musicales presentes.
  No equivale a una evaluación musical completa.
- Los ids de recurso del lote no se reutilizan. Cada recurso obtiene un UUID
  nuevo, sin sobrescribir recursos o vínculos existentes. Los ids válidos de
  sección y los ids de conceptos musicales sí se conservan.
- Los títulos repetidos están permitidos: se avisa de coincidencias con la
  biblioteca y se crean copias separadas. Importar otra vez crea nuevas copias;
  no hay actualización o deduplicación automática por título.
- Se conservan campos adicionales de cada recurso/contenido, exercise, PIMA,
  técnicas y metadatos. Los metadatos adicionales del contenedor del paquete no
  se incorporan a la sesión. Los campos learning/organization incluidos en un
  objeto recurso se conservan; los documentos TAB simples no los promueven desde
  el contenido a datos personales.
- Si el lote contiene recursos ocultos/completados o estados no interpretables,
  el resumen y el resultado avisan de que están fuera de Active. Se accede a ellos
  desde All resources. Los nuevos recursos sin clasificación quedan Active/Normal.
- Se conserva la sesión actual: rutinas, historial, valoraciones, objetivos,
  trash, práctica pendiente y campos desconocidos. Solo se añaden recursos y
  se actualiza user.updatedAt. No se importan rutinas ni se remapean referencias
  arbitrarias a ids de recurso contenidos en campos ajenos al contrato.

## Guardado y experiencia

La lectura/validación se ejecuta fuera del hilo de UI. El lote preparado no se
guarda hasta Import. Antes de guardar se revalida y se añade en una sola mutación
de SessionDocument; SessionViewModel serializa esa operación con las demás y usa
el guardado existente de AndroidSessionRepository. Se comunica éxito después
del guardado; un error permite revisar/reintentar desde el diálogo.

Se puede cerrar el diálogo antes de Import, incluso durante la lectura. Durante
el guardado se bloquea una segunda pulsación y el cierre del diálogo. Tras éxito
se muestra el resultado en Library y se limpian búsqueda/filtros a Active.
La vista previa no se persiste en SavedState por su tamaño: una recreación puede
requerir volver a elegir los archivos o pegar el JSON. El guardado ya iniciado
pertenece al ViewModel y no se convierte en una serie de guardados por recurso.

La importación completa desde Settings sigue reemplazando la sesión activa.
Este flujo permite añadir material sin devolver una copia antigua del historial.
Para generar lotes: [CONTENT_AGENT_CONTEXT.md](CONTENT_AGENT_CONTEXT.md).
