# TAREA 1 — Selección de rebanada

## Candidata 1 — Registro de usuario

**Como** persona interesada en usar el sistema,
**quiero** registrarme con nombre, correo y contraseña,
**para** poder reservar espacios deportivos.

El backlog establece que, dado un correo no registrado, cuando se envía el registro, se recibe un usuario con `id` asignado y la contraseña queda almacenada como hash.

También define el caso de un correo ya registrado, en el cual el sistema rechaza la operación y no persiste nada.

## Candidata 2 — Crear una reserva

**Como** usuario autenticado,
**quiero** reservar un escenario en fecha y horario específico,
**para** asegurar el uso del espacio.

El backlog establece que, dado un horario libre, cuando se realiza la reserva, esta queda `confirmada`.

También establece el rechazo cuando existe solapamiento y, como evidencia del problema duro, que ante 20 solicitudes simultáneas para el mismo escenario y horario exactamente 1 debe quedar confirmada, 19 deben ser rechazadas y no deben quedar duplicados persistidos.

## Rebanada elegida

Se elige **Crear una reserva**.

La elección se basa en que esta rebanada atraviesa más fronteras técnicas relevantes para el esqueleto que el registro de usuario y, al mismo tiempo, constituye la funcionalidad directamente relacionada con el problema duro de concurrencia definido para ArenaHub.

La rebanada permite verificar el recorrido desde una entrada externa hasta la persistencia real de una reserva y posteriormente utilizar la prueba de concurrencia como evidencia de que la integración funciona correctamente.

## Candidata descartada

Se descarta **Registro de usuario** porque, aunque atraviesa la entrada externa, la aplicación y la persistencia de un usuario, contiene menos integración técnica relacionada con el problema duro que la creación de reservas.

Además, crear una reserva permite comprobar una interacción con mayor relevancia para la integración entre aplicación, dominio y persistencia, incluyendo la garantía de no duplicación bajo solicitudes simultáneas.

## Fronteras técnicas de la rebanada elegida

La rebanada **Crear una reserva** cruza:

1. Entrada externa HTTP.
2. Aplicación.
3. Dominio.
4. Persistencia.
5. Base de datos real.

La migración forma parte de la preparación de la persistencia, aunque no constituye una interacción de negocio durante una solicitud individual.
