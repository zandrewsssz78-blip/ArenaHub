# TAREA 5 — Trazabilidad

| elemento del diagrama | archivo de origen | línea o sección que lo respalda |
|---|---|---|
| `Reserva` | `docs/modelo-dominio.md` | Sección de entidades principales del dominio, clase `Reserva`. |
| `id: Long` | `docs/modelo-dominio.md` | Clase `Reserva`, atributo `id`. |
| `codigoReserva: String` | `docs/modelo-dominio.md` | Clase `Reserva`, atributo `codigoReserva`. |
| `estado: EstadoReserva` | `docs/modelo-dominio.md` | Clase `Reserva`, atributo `estado`. |
| `fechaCreacion: LocalDateTime` | `docs/modelo-dominio.md` | Clase `Reserva`, atributo `fechaCreacion`. |
| `observacion: String` | `docs/modelo-dominio.md` | Clase `Reserva`, atributo `observacion`. |
| `Crear una reserva` | `docs/historias-usuario.md` | Sección 4. Crear una reserva. |
| Reserva confirmada | `docs/historias-usuario.md` | Sección 4, primer criterio de aceptación: dado un horario libre, la reserva queda confirmada. |
| Rechazo por solapamiento | `docs/historias-usuario.md` | Sección 4, segundo criterio de aceptación: dado un horario que se solapa, la solicitud es rechazada. |
| 20 solicitudes simultáneas | `docs/historias-usuario.md` | Sección 4, tercer criterio de aceptación: 20 solicitudes simultáneas para el mismo escenario/horario. |
| 1 confirmada y 19 rechazadas | `docs/historias-usuario.md` | Sección 4, tercer criterio de aceptación. |
| `RESERVAS` | `docs/esquema-bd.md` | Sección correspondiente a la tabla `RESERVAS`. |
| `RESERVAS.id` | `docs/esquema-bd.md` | Campo `id` de `RESERVAS`. |
| `RESERVAS.usuario_id` | `docs/esquema-bd.md` | Campo `usuario_id` de `RESERVAS`. |
| `RESERVAS.escenario_id` | `docs/esquema-bd.md` | Campo `escenario_id` de `RESERVAS`. |
| `RESERVAS.franja_id` | `docs/esquema-bd.md` | Campo `franja_id` de `RESERVAS`. |
| `RESERVAS.fecha` | `docs/esquema-bd.md` | Campo `fecha` de `RESERVAS`. |
| `RESERVAS.codigo_reserva` | `docs/esquema-bd.md` | Campo `codigo_reserva` de `RESERVAS`. |
| `RESERVAS.estado` | `docs/esquema-bd.md` | Campo `estado` de `RESERVAS`. |
| `RESERVAS.fecha_creacion` | `docs/esquema-bd.md` | Campo `fecha_creacion` de `RESERVAS`. |
| `RESERVAS.observacion` | `docs/esquema-bd.md` | Campo `observacion` de `RESERVAS`. |

## VACÍOS DETECTADOS

No se detectan vacíos para los elementos incluidos en el diagrama de clases corregido de la rebanada `Crear una reserva`.

No se agregan nombres de controladores, servicios, repositorios, interfaces o implementaciones concretas porque no están definidos explícitamente en los documentos fuente.