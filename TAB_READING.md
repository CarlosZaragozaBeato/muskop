# Tablature reading — PIMA, spacing and contrast

Primera entrega de la mejora de lectura aprobada por el propietario. La
digitación solicitada es la de la **mano derecha** que toca la cuerda, PIMA.

## Presentación

- Traste en texto de alto contraste, con fondo neutro que interrumpe la línea
  de la cuerda para facilitar su lectura. Se eliminan los círculos naranjas grandes.
- Cuando una nota declara `finger: p/i/m/a`, se muestra su letra debajo del traste
  en una etiqueta naranja pequeña con el color de contenido del tema.
- Las notas simultáneas comparten posición horizontal, pero cada cuerda conserva
  su propio traste y dedo. No se reduce un acorde a una única letra.
- Se reserva más altura entre cuerdas cuando se muestra PIMA. Las etiquetas no
  ocupan el espacio del número ni invaden la nota de la cuerda siguiente.
- La leyenda explica `p thumb · i index · m middle · a ring` y Show/Hide PIMA
  controla la visibilidad por sección en esa vista. Inicialmente está visible.
- Funciona en Full resource, Practice por compás, pantalla completa y lectura
  desde práctica libre/guiada. Cada vista conserva su propio estado de lectura.

## Datos y límites

Se usa el campo `notes[].finger` existente. No hay nueva versión de documento,
migración, modificación del JSON ni deducción automática del dedo por cuerda.
Mayúsculas y espacios se normalizan solo para mostrar una PIMA reconocida;
los valores originales se conservan. La digitación de mano izquierda no se
añade en esta entrega.

Si falta la digitación, el visor lo indica. Los valores no reconocidos no se
dibujan como PIMA; sus detalles se mantienen accesibles y se abren cuando es
necesario, incluso si se ocultaron detalles secundarios en pantalla completa.
Los recursos antiguos que ya contienen PIMA no necesitan regenerarse. Los que
carecen de ella requieren contenido con digitación explícita para poder mostrarla.

Duración, técnicas, afinación y notas de estudio se preservan. Esta entrega
no añade figuras rítmicas, símbolos de técnicas, reproducción, digitación izquierda
ni cursor sincronizado. Su representación visual ampliada pertenece a otra fase.

## Distribución de compases

`TabReadingLayout.kt` proyecta notas y calcula filas sin cambiar datos. Los
ataques se ordenan por beat, manteniendo juntas todas las notas simultáneas,
y se reparten según el ancho y la escala. La separación mínima tiene en cuenta
el número de dígitos del traste y el zoom/texto del sistema.

Cada ataque aparece en una sola parte. Una continuación numerada sigue siendo
el mismo compás musical. Las guías de pulso se interpolan usando las posiciones
de esos ataques; el espacio físico se adapta a la legibilidad y no representa
una escala temporal estricta ni cambia el ritmo guardado.

Normalmente las filas caben en el ancho disponible. Con zoom o texto extremo,
una fila puede necesitar desplazamiento horizontal para conservar números y
etiquetas legibles. Las posiciones fuera del diagrama continúan en los detalles.
No se promete representar todas las extensiones desconocidas del formato.

## Implementación y comprobación

`TabNotationView.kt` comparte el dibujo y opciones PIMA. LibraryScreen.kt lo
conecta al visor completo y al modo por compás. Las propiedades de texto/color
dependen de MaterialTheme y la escala existente. Canvas aporta descripción
accesible de pulso, cuerda, traste y PIMA, además del texto de detalles.

Las pruebas de `TabReadingLayoutTest` comprueban preservación, simultaneidad,
digitación ausente/no compatible, partición de ataques muy próximos y límites
de compás con varios anchos/zooms. La revisión en dispositivo debe cubrir claro/
oscuro, 320/360/412 dp, fuente ampliada, acordes simultáneos, trastes de dos
dígitos, Show/Hide PIMA, pantalla completa, Practice y lectura durante una rutina.

Contrato de generación: [AGENT_CONTEXT.md](AGENT_CONTEXT.md) y
[CONTENT_AGENT_CONTEXT.md](CONTENT_AGENT_CONTEXT.md).
