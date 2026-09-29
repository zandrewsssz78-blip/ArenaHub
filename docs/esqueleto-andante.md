# Esqueleto andante — ArenaHub

El esqueleto recorre todas las fronteras técnicas del sistema con **3 historias de
usuario funcionales** del backlog (`docs/historias-usuario.md`):

```
HTTP (Spring MVC) → Aplicación (servicios) → Dominio (entidades) → JPA/Hibernate → PostgreSQL 16 (Flyway)
```

## Historias implementadas

| HU | Endpoint | Respuestas |
|---|---|---|
| **HU1** Registro de usuario | `POST /api/usuarios` | `201` usuario creado · `409` correo ya registrado · `400` datos inválidos |
| **HU3** Consultar escenarios disponibles | `GET /api/escenarios/disponibilidad?fecha=AAAA-MM-DD` | `200` escenarios **ACTIVOS** con cada franja marcada `ocupada: true/false` |
| **HU4** Crear una reserva | `POST /api/reservas` | `201` reserva `CONFIRMADA` · `409` horario ocupado · `422` escenario no activo · `404` usuario/escenario/franja inexistente |

> **Limitación conocida:** HU2 (inicio de sesión) aún no existe, por eso `usuarioId`
> viaja en el cuerpo de `POST /api/reservas`. Cuando se implemente HU2 saldrá del token.

## Estructura del código

```
src/main/java/com/arenahub/
├── dominio/        Usuario, EscenarioDeportivo, FranjaHoraria, Reserva + enums (reglas del dominio)
├── persistencia/   Repositorios Spring Data JPA
├── aplicacion/     RegistroUsuarioService, DisponibilidadService, ReservaService
└── web/            Controladores REST y ManejadorErrores
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

## Pruebas (`./mvnw verify`)

| Prueba | Tipo | Qué demuestra |
|---|---|---|
| `dominio/ReservaTest` | Unitaria (sin Spring ni BD) | Reserva queda CONFIRMADA; escenario en mantenimiento se rechaza; franja inválida |
| `integracion/RegistroUsuarioIntegracionTest` | Integración HTTP + PostgreSQL | HU1: id asignado y contraseña con hash BCrypt; correo repetido → 409 sin persistir |
| `integracion/DisponibilidadEscenariosIntegracionTest` | Integración HTTP + PostgreSQL | HU3: solo escenarios activos; franjas ocupadas marcadas |
| `integracion/CrearReservaIntegracionTest` | Integración HTTP + PostgreSQL | HU4: confirmada, conflicto 409, escenario inactivo 422 y **evidencia del problema duro** |

### Evidencia del problema duro

`CrearReservaIntegracionTest.veinteSolicitudesSimultaneasSoloConfirmanUna`:

1. Crea 20 usuarios distintos y un escenario activo.
2. 20 hilos esperan en una barrera (`CountDownLatch`) y se liberan al mismo tiempo.
3. Cada hilo hace `POST /api/reservas` por HTTP real al mismo escenario, fecha y franja.
4. Verifica: **1** respuesta `201`, **19** respuestas `409` y **1** sola fila en `reserva`.

La prueba corre sobre un esquema recién migrado (Flyway `clean` + `migrate`), así que
también detecta errores en las migraciones.
