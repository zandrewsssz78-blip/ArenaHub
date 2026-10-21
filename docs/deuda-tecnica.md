# Registro de deuda técnica — ArenaHub

Deuda identificada en la revisión de código ([`revision-codigo.md`](./revision-codigo.md)).
Cada ítem tiene su impacto, su esfuerzo estimado y su prioridad.

**Cómo se prioriza.** Primero lo que tiene **impacto alto** (seguridad o datos incorrectos);
dentro del mismo impacto, lo de **menor esfuerzo**.

| Escala | Impacto | Esfuerzo |
|---|---|---|
| Alto | Riesgo de seguridad o de datos incorrectos | Más de un día, toca varias capas |
| Medio | Comportamiento confuso o inconsistente para el usuario | Unas horas |
| Bajo | Mantenibilidad, sin efecto visible | Menos de una hora |

## Registro

| ID | Prioridad | Deuda | Impacto | Esfuerzo | Origen | Propuesta |
|---|---|---|---|---|---|---|
| DT-1 | **1** | El token de HU2 se genera (UUID) pero **no se guarda ni se valida**; las operaciones reciben `usuarioId` en el cuerpo, así que un cliente puede actuar en nombre de otro usuario. | Alto | Alto | S7 | Guardar la sesión o usar JWT; leer el usuario del encabezado `Authorization` y quitar `usuarioId` de los requests. |
| DT-2 | **2** | HU7 y HU8 **no verifican el rol ADMINISTRADOR**. | Alto | Medio (depende de DT-1) | S8 | Con el usuario autenticado, rechazar con 403 si `rol != ADMINISTRADOR`. |
| DT-3 | **3** | Al **desactivar un escenario** con reservas futuras confirmadas, esas reservas siguen confirmadas y nadie es avisado. | Alto | Medio | Revisión de HU8 | Decidir en equipo: cancelarlas automáticamente o impedir la desactivación mientras existan. |
| DT-4 | **4** | `EscenarioService.registrar` traduce **cualquier** violación de integridad como "nombre duplicado" (causa del defecto D2). | Medio | Bajo | S6 | Revisar el nombre de la restricción (`uq_escenario_nombre`) como ya hace `ReservaService`. |
| DT-5 | **5** | Hoy se puede reservar una **franja que ya pasó** (por ejemplo, a las 5 p. m. reservar de 6 a 7 a. m. del mismo día). | Medio | Bajo | Caso borde de HU4 | Rechazar si la fecha es hoy y `horaInicio` ya pasó; recibir un `Clock` para poder probarlo. |
| DT-6 | **6** | Dos endpoints para cancelar (`PATCH …/cancelar` y `DELETE …/{id}`). | Bajo | Bajo | S5 | Dejar solo `PATCH …/cancelar` (documentado en el README) y avisar el cambio. |
| DT-7 | **7** | Comprobaciones de `null` innecesarias en `ReservaResponse.de`. | Bajo | Bajo | S4 | Quitarlas cuando haya pruebas que cubran todas las respuestas. |
| DT-8 | **8** | RNF-1 (consulta de disponibilidad < 400 ms p95) **no está medido**, y `rnf-candidatos.md` todavía menciona MySQL. | Medio | Medio | `rnf-candidatos.md` | Prueba de carga con 50 usuarios concurrentes; actualizar el entorno a PostgreSQL 16. |
| DT-9 | **9** | `mvnw` está en el repositorio **sin permiso de ejecución**: en Mac/Linux `./mvnw` falla con "Permission denied". | Bajo | Bajo | Contrato de ejecución del README | `git update-index --chmod=+x mvnw`. |
| DT-10 | **10** | Ramas antiguas sin borrar (`bd/modelo-datos`, `java/esqueleto-inicial`, `feature/esqueleto-andante`). | Bajo | Bajo | Repositorio | Borrarlas: su contenido útil ya está en `main`. |

## Ya pagada

| Deuda | Cómo se pagó |
|---|---|
| Reservas en fechas pasadas (D1) | Regla en `Reserva.confirmar` + pruebas unitaria y de integración. |
| `capacidad = 0` devolvía 409 engañoso (D2) | `@Positive` en el request + prueba de integración. |
| Lógica duplicada y mapeos en el controlador (S1, S2, S3) | Refactorización sin cambio de comportamiento. |
| Pruebas con fechas fijas que caducarían con el calendario | Fechas relativas (`enDias(n)`, `LocalDate.now().plusDays(n)`). |
