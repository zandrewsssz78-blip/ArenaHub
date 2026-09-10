# TAREA 4 — Contrato de la prueba única

## Rebanada seleccionada

Crear una reserva.

## Punto de entrada

La prueba comienza fuera del sistema enviando solicitudes HTTP para crear una reserva.

## Precondición de datos

La base de datos debe estar disponible y contener un escenario y una franja horaria que puedan utilizarse para la prueba.

Para la ejecución de la prueba no debe existir previamente una reserva que produzca el conflicto que se desea comprobar para el escenario, fecha y franja horaria seleccionados.

## Acción

Se envían 20 solicitudes HTTP concurrentes intentando crear una reserva para el mismo escenario y la misma fecha y franja horaria.

Las solicitudes se procesan simultáneamente para verificar el problema duro de concurrencia.

## Aserción observable

La prueba debe verificar que exactamente:

- 1 solicitud queda confirmada.
- 19 solicitudes son rechazadas.
- No se producen duplicados persistidos para el mismo escenario, fecha y franja horaria.

## Estado esperado en la base de datos real

Después de procesar las 20 solicitudes, la base de datos debe contener exactamente una reserva confirmada para el escenario, fecha y franja horaria utilizados en la prueba.

No deben existir reservas duplicadas para esa combinación.

## Cobertura de fronteras

| Frontera | ¿La prueba la detecta? | Cómo |
|---|---|---|
| Entrada externa | Sí | Las solicitudes se realizan mediante HTTP. |
| Aplicación | Sí | La solicitud debe atravesar la aplicación para producir el resultado esperado. |
| Dominio | Sí | La creación de la reserva debe producir el comportamiento definido. |
| Mapeo | Sí | Los datos de la reserva deben quedar correctamente representados en la persistencia. |
| Driver | Sí | La aplicación debe comunicarse correctamente con la base de datos. |
| Migración | Sí, si se ejecuta sobre una base de datos nueva | La estructura necesaria debe estar disponible mediante la preparación definida para la base de datos. |
| Base de datos | Sí | La prueba verifica el estado real de las reservas persistidas. |
| Respuesta | Sí | La prueba contabiliza las respuestas obtenidas por las 20 solicitudes. |

## Posible frontera no cubierta

La migración podría no ser detectada si la prueba se ejecuta contra una base de datos que ya contiene previamente la estructura necesaria.

Para evitar este punto ciego, la prueba debe ejecutarse sobre una base de datos nueva o vacía antes de preparar el esquema.

## Criterio de éxito

La prueba pasa únicamente cuando las 20 solicitudes concurrentes para el mismo escenario, fecha y franja horaria producen:

- 1 reserva confirmada.
- 19 solicitudes rechazadas.
- 1 sola reserva persistida para esa combinación.