# ArenaHub

Sistema de gestión y reserva de escenarios deportivos que garantiza,
incluso ante solicitudes simultáneas, que dos personas nunca reserven
el mismo espacio en el mismo horario.

## Equipo

| Integrante | Rol |
|---|---|
| Ana Sofía Siachoque | Bases de datos / desarrollo en SQL |
| Andres Felipe Rubiano Duarte | Desarrollo en Java |

## Cómo ejecutar (contrato de ejecución)

```bash
git clone https://github.com/zandrewsssz78-blip/ArenaHub.git && cd ArenaHub

docker compose up -d          # 1. levanta PostgreSQL 16
./mvnw spring-boot:run        # 2. levanta la aplicación en http://localhost:8080

./mvnw verify                 # pruebas (requiere la base de datos del paso 1 arriba)
```

En Windows usar `mvnw.cmd` en lugar de `./mvnw`.

### Requisitos de entorno

| Herramienta | Versión |
|---|---|
| Java (JDK) | 21 LTS |
| Maven | 3.9.11 (lo descarga `./mvnw`, no hay que instalarlo) |
| Spring Boot | 3.5.6 |
| Docker / Docker Compose | Docker Engine 24+ con Compose v2 |
| PostgreSQL | 16 (imagen `postgres:16` de `docker-compose.yml`) |

Las tablas las crea **Flyway** al arrancar (`src/main/resources/db/migration`) en el
esquema `arenahub`. Las pruebas usan el esquema `arenahub_test`, que se borra y se
vuelve a migrar en cada ejecución.

### Probar a mano (Historias de Usuario HU1 - HU8)

```bash
# HU1 — Registrarse como nuevo usuario
curl -X POST localhost:8080/api/usuarios -H "Content-Type: application/json" \
  -d '{"nombre":"Ana","email":"ana@example.com","contrasena":"secreta123"}'

# HU2 — Iniciar sesión (autenticación)
curl -X POST localhost:8080/api/usuarios/login -H "Content-Type: application/json" \
  -d '{"email":"ana@example.com","contrasena":"secreta123"}'

# HU3 — Ver escenarios activos y franjas ocupadas en una fecha
curl "localhost:8080/api/escenarios/disponibilidad?fecha=2026-10-15"

# HU4 — Reservar (usuarioId del paso 1, escenarioId y franjaId del paso 3)
curl -X POST localhost:8080/api/reservas -H "Content-Type: application/json" \
  -d '{"usuarioId":1,"escenarioId":1,"fecha":"2026-10-15","franjaId":13}'

# HU6 — Ver historial de reservas propias
curl "localhost:8080/api/reservas?usuarioId=1"

# HU5 — Cancelar una reserva propia (id de reserva del paso anterior)
curl -X PATCH localhost:8080/api/reservas/1/cancelar -H "Content-Type: application/json" \
  -d '{"usuarioId":1}'

# HU7 — Registrar nuevo escenario deportivo (Administración)
curl -X POST localhost:8080/api/escenarios -H "Content-Type: application/json" \
  -d '{"nombre":"Cancha Sintética 4","tipo":"FUTBOL_5","ubicacion":"Sede Norte","capacidad":10,"descripcion":"Pasto sintético con iluminación LED"}'

# HU8 — Desactivar un escenario deportivo (Mantenimiento)
curl -X PATCH localhost:8080/api/escenarios/1/desactivar
```

Detalle del esqueleto andante y cobertura de HU: [`docs/esqueleto-andante.md`](./docs/esqueleto-andante.md).

## Estructura de documentación

| Documento | Ruta | Contenido |
|---|---|---|
| Visión del producto | [`docs/vision-producto.md`](./docs/vision-producto.md) | Usuarios, problema, alcance inicial y restricciones |
| Acuerdos de equipo | [`docs/acuerdos-equipo.md`](./docs/acuerdos-equipo.md) | Roles, toma de decisiones, política de IA |
| Historias de usuario | [`docs/historias-usuario.md`](./docs/historias-usuario.md) | Backlog priorizado completo (HU1 - HU8) |
| Problema duro | [`docs/problema-duro.md`](./docs/problema-duro.md) | Concurrencia en reservas: declaración, mecanismo y evidencia |
| Esqueleto andante | [`docs/esqueleto-andante.md`](./docs/esqueleto-andante.md) | Historias implementadas, endpoints y pruebas |
| Registro de uso de IA | [`docs/uso-ia.md`](./docs/uso-ia.md) | Sugerencias de IA aceptadas/rechazadas y por qué |

## Problema duro: Concurrencia

El sistema garantiza que nunca existan dos reservas activas para el
mismo escenario deportivo con horarios solapados, incluso cuando dos
solicitudes llegan al mismo tiempo. Ver el mecanismo de control y la
estrategia de verificación (20 solicitudes concurrentes con barrera de
sincronización) en [`docs/problema-duro.md`](./docs/problema-duro.md).

## Estado del proyecto

Sistema implementado con las **8 historias funcionales del backlog (HU1 a HU8)**,
100% desarrollado con **Java 21 LTS** y **Spring Boot 3.5.6**, arquitectura por capas
(Web REST, Aplicación, Dominio, Persistencia con Spring Data JPA y PostgreSQL 16)
y la prueba de concurrencia del problema duro con índice único parcial.
