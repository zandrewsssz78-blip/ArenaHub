# Paquete de evidencia para la entrevista técnica — ArenaHub

Este documento reúne las decisiones y su
justificación, el conocimiento técnico detrás del problema duro y las respuestas a las preguntas
previsibles.

## 1. Decisiones importantes y su justificación

| # | Decisión | Justificación | Evidencia |
|---|---|---|---|
| 1 | Concurrencia como problema duro | Es el riesgo real del dominio: dos personas reservando la misma cancha a la misma hora | [`problema-duro.md`](./problema-duro.md), [`uso-ia.md`](./uso-ia.md) |
| 2 | Garantía en la base de datos con índice único parcial | Atómica; no depende del orden de los hilos ni de cuántas instancias corran | `V1__esquema_inicial.sql` (`uq_reserva_confirmada`) |
| 3 | PostgreSQL 16 en lugar de MySQL | MySQL no tiene índices parciales | [`rnf-candidatos.md`](./rnf-candidatos.md), historial de la semana 5 |
| 4 | Franjas fijas de una hora | Convierte el solapamiento en igualdad de franja, resoluble con un índice | `V2__datos_semilla.sql` (16 franjas) |
| 5 | Índice **parcial** (`WHERE estado = 'CONFIRMADA'`) | Una reserva cancelada libera la franja (HU5) | `CancelarReservaIntegracionTest` |
| 6 | Arquitectura por capas y reglas en el dominio | Bajo acoplamiento y Experto en información (GRASP) | [`revision-codigo.md`](./revision-codigo.md) §3 |
| 7 | Flyway y esquema de pruebas separado | Base reproducible; las pruebas no tocan los datos reales | `application-test.yml` |
| 8 | Registrar deuda en lugar de cambiar comportamiento a última hora | Los cambios de seguridad (DT-1, DT-2) tocan varias capas y el contrato de la API | [`deuda-tecnica.md`](./deuda-tecnica.md) |
| 9 | Frontend sin frameworks dentro de Spring Boot | No añade comandos al contrato de ejecución | `src/main/resources/static` |

## 2. Conocimiento técnico que hay que dominar

### 2.1 Condición de carrera en la reserva

```text
Solicitud A: ¿franja libre? → sí ─┐
Solicitud B: ¿franja libre? → sí ─┤  (las dos verifican antes de que alguna guarde)
                                  ├→ A inserta → OK
                                  └→ B inserta → con un if en Java: duplicado
                                                  con el índice único: rechazada → 409
```

La verificación previa (`existsBy…`) no protege nada bajo concurrencia; solo da un mensaje
rápido en el caso normal. La protección real es la restricción en la base de datos, que se evalúa
dentro de la inserción.

### 2.2 Cómo se traduce el rechazo

`saveAndFlush` → `DataIntegrityViolationException` → `ReservaService` comprueba que la
restricción violada sea `uq_reserva_confirmada` → `ConflictoException` → `ManejadorErrores`
responde **409** → el frontend muestra "Alguien reservó primero".

Se revisa el nombre de la restricción para no confundir este caso con otros errores de
integridad. `EscenarioService` todavía no lo hace (DT-4), y eso causó el defecto D2.

### 2.3 Cómo se demuestra

`CrearReservaIntegracionTest.veinteSolicitudesSimultaneasSoloConfirmanUna`:
- Pool de 20 hilos y 20 usuarios distintos.
- `CountDownLatch listos` espera a que los 20 estén preparados; `CountDownLatch salida` los
  libera al mismo tiempo.
- Las solicitudes van por HTTP contra la API real y PostgreSQL real.
- Verifica: 1 respuesta 201, 19 respuestas 409 y `count(*) = 1` en `reserva`.

Una ejecución secuencial no sirve de evidencia: siempre pasaría, aunque el sistema tuviera el error.

Ejecución: `./mvnw -Dtest=CrearReservaIntegracionTest test`.

### 2.4 Conceptos del curso aplicados

| Concepto | Dónde se ve en ArenaHub |
|---|---|
| 3NF | Cuatro tablas; `reserva` solo guarda llaves foráneas, fecha y estado |
| SOLID: SRP, DIP | Respuestas con `de(...)` propio; inyección por constructor en servicios |
| GRASP: Experto en información, Controlador, Bajo acoplamiento | Reglas en `Reserva`; controladores delgados; capas |
| Refactorización segura | S1–S3 sin cambiar el comportamiento observable, con las mismas pruebas en verde |
| Deuda técnica priorizada | Impacto primero, luego esfuerzo |
| INVEST y criterios verificables | [`historias-usuario.md`](./historias-usuario.md) |
| RNF medibles | [`rnf-candidatos.md`](./rnf-candidatos.md): RNF-2 verificado; RNF-1 pendiente de medir |

## 3. Preguntas anticipadas

**¿Por qué tomaron esta decisión de diseño (índice en la base de datos)?**
Porque es la única capa que ve todas las inserciones a la vez. Bloquear en Java no sirve si hay
dos instancias; un bloqueo pesimista funcionaría, pero es más lento y requiere más código.

**¿Qué problemas encontraron y cómo los resolvieron?**
- D1: se podía reservar en fechas pasadas → regla en el dominio, con prueba.
- D2: `capacidad = 0` devolvía un 409 engañoso → validación `@Positive` (400), con prueba.
- MySQL no soportaba la solución → migración a PostgreSQL en la semana 5.
- Entorno Windows con `Path` y `ComSpec` dañados → documentado en la guía de instalación.

**¿Qué alternativas evaluaron y por qué no las eligieron?**
`synchronized` en Java, `SELECT … FOR UPDATE`, horarios libres con restricción de exclusión y
MySQL. Las razones están en el punto 1 y en [`informe-tecnico.md`](./informe-tecnico.md) §2.3.

**¿Cuáles son los riesgos actuales del sistema?**
DT-1 (identidad sin validar), DT-2 (rol sin validar en el backend) y DT-3 (reservas huérfanas al
desactivar un escenario). Están priorizados en el plan de mejora.

**¿Qué harían si tuvieran más tiempo?**
JWT y control por rol con 403; decidir y aplicar la política de reservas al desactivar un
escenario; medir el RNF-1 con una prueba de carga; quitar el endpoint duplicado de cancelación.

**¿Cómo garantizan la calidad del software?**
58 pruebas automatizadas (22 unitarias y 36 de integración con base de datos real), revisión de
código con SOLID/GRASP, refactorizaciones respaldadas por pruebas y deuda registrada.

**¿Cómo usaron la IA?**
Como apoyo, bajo la política de [`uso-ia.md`](./uso-ia.md): nada entra sin revisión, ejecución y
comprensión del equipo. El registro dice qué se aceptó, qué se modificó y qué se rechazó.

**¿Qué aprendieron como equipo?**
Que las garantías críticas se ponen en la capa que no se puede saltar, que la evidencia tiene
que reproducir el problema real (concurrencia, no secuencia) y que dividir por capas exige
acordar el contrato antes de implementar.

**¿Quién hizo qué?**
Ana Sofía: modelo de datos, esquema 3NF, migraciones, restricciones e índice del problema duro,
consultas y documentación de BD; frontend con apoyo de IA (registrado en `uso-ia.md`). Andres
Felipe: lógica de negocio, backend en Java e integración con la base de datos. Las
decisiones que tocaban ambas partes se tomaron por consenso ([`acuerdos-equipo.md`](./acuerdos-equipo.md)).
