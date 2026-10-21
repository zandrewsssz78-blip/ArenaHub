# Esqueleto andante y Cobertura de Historias de Usuario — ArenaHub

El sistema recorre todas las fronteras técnicas con las **8 historias de usuario funcionales** del backlog (`docs/historias-usuario.md`):

```
HTTP (Spring MVC) → Aplicación (servicios) → Dominio (entidades) → JPA/Hibernate → PostgreSQL 16 (Flyway)
```

## Historias de usuario implementadas (100% Java + Spring Boot)

| HU | Historia | Endpoint | Respuestas principales |
|---|---|---|---|
| **HU1** | Registro de usuario | `POST /api/usuarios` | `201` usuario creado · `409` correo ya registrado · `400` datos inválidos |
| **HU2** | Inicio de sesión | `POST /api/usuarios/login` | `200` token y datos del usuario · `401` credenciales inválidas (sin revelar campo) · `400` datos inválidos |
| **HU3** | Consultar escenarios disponibles | `GET /api/escenarios/disponibilidad?fecha=AAAA-MM-DD` | `200` escenarios **ACTIVOS** con cada franja marcada `ocupada: true/false` |
| **HU4** | Crear una reserva | `POST /api/reservas` | `201` reserva `CONFIRMADA` · `409` horario ocupado · `422` escenario no activo · `404` usuario/escenario/franja inexistente |
| **HU5** | Cancelar una reserva | `PATCH /api/reservas/{id}/cancelar` | `200` reserva `CANCELADA` y horario liberado · `422` reserva ajena o ya cancelada · `404` no existe |
| **HU6** | Ver historial de reservas | `GET /api/reservas?usuarioId=X` | `200` lista de reservas con escenario, franja y estado · `404` usuario inexistente |
| **HU7** | Registrar nuevo escenario deportivo | `POST /api/escenarios` | `201` escenario creado con `estado = ACTIVO` · `409` nombre duplicado · `400` datos incompletos |
| **HU8** | Desactivar escenario deportivo | `PATCH /api/escenarios/{id}/desactivar` o `PATCH /api/escenarios/{id}/estado` | `200` escenario con `estado = INACTIVO/EN_MANTENIMIENTO` (queda excluido de HU3 y bloqueado en HU4) · `404` escenario inexistente |

## Estructura del código

```
src/main/java/com/arenahub/
├── dominio/        Usuario, EscenarioDeportivo, FranjaHoraria, Reserva + enums (reglas del dominio)
├── persistencia/   Repositorios Spring Data JPA (UsuarioRepository, EscenarioDeportivoRepository, etc.)
├── aplicacion/     RegistroUsuarioService, AutenticacionService, DisponibilidadService, ReservaService, EscenarioService
└── web/            Controladores REST (UsuarioController, EscenarioController, ReservaController) y ManejadorErrores
src/main/resources/db/migration/
├── V1__esquema_inicial.sql   Tablas 3NF + índice único parcial del problema duro
└── V2__datos_semilla.sql     16 franjas (06:00–22:00) y 3 escenarios
```

## Mecanismo del problema duro (concurrencia)

La garantía la da PostgreSQL con un **índice único parcial**:

```sql
CREATE UNIQUE INDEX uq_reserva_confirmada
    ON reserva (escenario_id, fecha, franja_id)
    WHERE estado = 'CONFIRMADA';
```

- Si 20 transacciones intentan insertar la misma (escenario, fecha, franja) a la vez,
  la base de datos acepta **una** y las demás fallan con violación de unicidad.
  `ReservaService` traduce ese error a **HTTP 409**.
- La verificación previa `existsBy...` solo da un mensaje rápido en el caso
  secuencial; **no** es la garantía (dos hilos pueden pasarla al mismo tiempo).
- Solo cuentan las reservas `CONFIRMADA`: una reserva `CANCELADA` libera el horario (HU5).

## Pruebas (`./mvnw verify` / `mvnw.cmd test`)

| Prueba | Tipo | Qué demuestra |
|---|---|---|
| `dominio/ReservaTest` | Unitaria (sin Spring ni BD) | Reserva queda CONFIRMADA; escenario en mantenimiento se rechaza; fecha pasada se rechaza y el mismo día se permite; franja inválida; reglas de cancelación |
| `aplicacion/ReservaServiceTest` | Unitaria con Mockito | HU5 y HU6: cancelación exitosa, reglas de negocio y consulta de historial |
| `aplicacion/AutenticacionServiceTest` | Unitaria con Mockito | HU2: login exitoso, fallo por correo o contraseña incorrecta sin revelar causa, usuario inactivo |
| `aplicacion/EscenarioServiceTest` | Unitaria con Mockito | HU7 y HU8: registro con estado ACTIVO, nombre duplicado a 409, desactivación y mantenimiento |
| `integracion/RegistroUsuarioIntegracionTest` | Integración HTTP + PostgreSQL | HU1: id asignado y contraseña con hash BCrypt; correo repetido → 409 sin persistir |
| `integracion/AutenticacionIntegracionTest` | Integración HTTP + PostgreSQL | HU2: login con credenciales válidas genera token; credenciales erróneas o correo inexistente → 401 |
| `integracion/DisponibilidadEscenariosIntegracionTest` | Integración HTTP + PostgreSQL | HU3: solo escenarios activos; franjas ocupadas marcadas |
| `integracion/CrearReservaIntegracionTest` | Integración HTTP + PostgreSQL | HU4: confirmada, conflicto 409, escenario inactivo 422 y **evidencia del problema duro** |
| `integracion/EscenarioIntegracionTest` | Integración HTTP + PostgreSQL | HU7 y HU8: creación de escenario, validación 400, desactivación a INACTIVO, exclusión de disponibilidad y bloqueo de reservas |
| `integracion/CancelarReservaIntegracionTest` | Integración HTTP + PostgreSQL | HU5: reserva propia queda CANCELADA en BD; el horario queda libre y otro usuario puede reservarlo; reserva ajena → 422 sin cambios; inexistente → 404 |
| `integracion/HistorialReservasIntegracionTest` | Integración HTTP + PostgreSQL | HU6: solo reservas del usuario con escenario, fecha, horario y estado; incluye canceladas; sin reservas → lista vacía; usuario inexistente → 404 |
| `integracion/CasosBordeIntegracionTest` | Integración HTTP + PostgreSQL | Casos borde: fecha pasada → 422 (y hoy sí se permite); otra franja u otro escenario no es conflicto; franja/usuario inexistente → 404; sin fecha → 400; doble cancelación → 422; capacidad 0 → 400; nombre repetido con otras mayúsculas → 409; contraseña corta → 400; login sin importar mayúsculas del correo |

### Evidencia del problema duro

`CrearReservaIntegracionTest.veinteSolicitudesSimultaneasSoloConfirmanUna`:

1. Crea 20 usuarios distintos y un escenario activo.
2. 20 hilos esperan en una barrera (`CountDownLatch`) y se liberan al mismo tiempo.
3. Cada hilo hace `POST /api/reservas` por HTTP real al mismo escenario, fecha y franja.
4. Verifica: **1** respuesta `201`, **19** respuestas `409` y **1** sola fila en `reserva`.

La prueba corre sobre un esquema recién migrado (Flyway `clean` + `migrate`), así que
también detecta errores en las migraciones.
