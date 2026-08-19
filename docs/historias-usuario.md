# Backlog Priorizado — ArenaHub

## 1. Registro de usuario
**Como** persona interesada en usar el sistema,
**quiero** registrarme con nombre, correo y contraseña,
**para** poder reservar espacios deportivos.

- Dado un correo no registrado, cuando envío el registro, entonces recibo un usuario con id asignado y la contraseña queda almacenada como hash.
- Dado un correo ya registrado, cuando intento registrarme de nuevo, entonces el sistema rechaza la operación y no persiste nada.

## 2. Inicio de sesión
**Como** usuario registrado,
**quiero** iniciar sesión con correo y contraseña,
**para** acceder a mis reservas.

- Dadas credenciales correctas, cuando inicio sesión, entonces recibo acceso válido (token/sesión).
- Dadas credenciales incorrectas, cuando intento iniciar sesión, entonces el sistema rechaza el acceso sin indicar cuál campo falló.

## 3. Consultar escenarios disponibles
**Como** usuario autenticado,
**quiero** ver escenarios y su disponibilidad por fecha,
**para** decidir cuál y cuándo reservar.

- Dada una fecha, cuando consulto escenarios, entonces solo veo los que tienen `estado = true`, con sus horarios ya ocupados marcados.

## 4. Crear una reserva (incluye la garantía del problema duro)
**Como** usuario autenticado,
**quiero** reservar un escenario en fecha y horario específico,
**para** asegurar el uso del espacio.

- Dado un horario libre, cuando reservo, entonces la reserva queda `confirmada`.
- Dado un horario que se solapa con una reserva existente, cuando intento reservar, entonces el sistema rechaza la solicitud con mensaje de conflicto.
- Dadas 20 solicitudes simultáneas para el mismo escenario/horario, cuando se procesan a la vez, entonces exactamente 1 queda confirmada, 19 rechazadas, y no quedan duplicados persistidos (evidencia del problema duro).

## 5. Cancelar una reserva
**Como** usuario autenticado,
**quiero** cancelar una reserva propia,
**para** liberar el espacio si ya no lo necesito.

- Dada una reserva propia, cuando la cancelo, entonces cambia a estado `cancelada` y el horario queda libre.
- Dada una reserva de otro usuario, cuando intento cancelarla, entonces el sistema rechaza la acción.

## 6. Ver historial de reservas propias
**Como** usuario autenticado,
**quiero** ver mis reservas pasadas y futuras,
**para** hacer seguimiento de mi actividad.

- Dado un usuario autenticado, cuando consulta su historial, entonces solo ve sus propias reservas, con escenario, fecha, horario y estado.

## 7. Registrar un nuevo escenario deportivo (administración)
**Como** administrador,
**quiero** registrar un escenario con nombre, tipo y ubicación,
**para** que esté disponible para reservas.

- Dado nombre y tipo válidos, cuando registro el escenario, entonces queda creado con `estado = true`.
- Dado un registro sin nombre o sin tipo, cuando intento guardarlo, entonces el sistema rechaza la operación y no persiste nada.

## 8. Desactivar un escenario deportivo (administración)
**Como** administrador,
**quiero** marcar un escenario como no disponible (mantenimiento),
**para** evitar que se reserve mientras no está en uso.

- Dado un escenario activo, cuando lo desactivo, entonces su `estado` pasa a `false` y deja de aparecer en la consulta de disponibilidad.
- Dado un escenario ya desactivado, cuando alguien intenta reservarlo, entonces el sistema rechaza la solicitud.