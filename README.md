# ArenaHub

Sistema de gestión y reserva de escenarios deportivos que garantiza,
incluso ante solicitudes simultáneas, que dos personas nunca reserven
el mismo espacio en el mismo horario.

## Equipo

| Integrante | Rol |
|---|---|
| Ana Sofía Siachoque | Bases de datos / desarrollo en SQL |
| Andres Felipe Rubiano Duarte | Desarrollo en Java |

## Estructura de documentación

| Documento | Ruta | Contenido |
|---|---|---|
| Visión del producto | [`docs/vision-producto.md`](./docs/vision-producto.md) | Usuarios, problema, alcance inicial y restricciones |
| Acuerdos de equipo | [`docs/acuerdos-equipo.md`](./docs/acuerdos-equipo.md) | Roles, toma de decisiones, política de IA |
| Problema duro | [`docs/problema-duro.md`](./docs/problema-duro.md) | Concurrencia en reservas: declaración, mecanismo y evidencia |
| Registro de uso de IA | [`docs/uso-ia.md`](./docs/uso-ia.md) | Sugerencias de IA aceptadas/rechazadas y por qué |

## Problema duro: Concurrencia

El sistema garantiza que nunca existan dos reservas activas para el
mismo escenario deportivo con horarios solapados, incluso cuando dos
solicitudes llegan al mismo tiempo. Ver el mecanismo de control y la
estrategia de verificación (20 solicitudes concurrentes con barrera de
sincronización) en [`docs/problema-duro.md`](./docs/problema-duro.md).

## Estado del proyecto

Proyecto en desarrollo — curso de Ingeniería de Software.
