# Revisión de código — Defectos, code smells, SOLID y GRASP

Revisión hecha sobre `main` después de fusionar el PR #2 (HU1–HU8), con el objetivo de
detectar defectos y *code smells* y contrastar el código con los principios **SOLID** y
**GRASP**. Cada hallazgo dice dónde está, qué principio afecta y qué se decidió.

**Decisión** puede ser:
- **Corregido**: había un defecto (comportamiento incorrecto) y se arregló con su prueba.
- **Refactorizado**: se mejoró la estructura **sin alterar el comportamiento observable**
  (mismos endpoints, mismas respuestas, mismas pruebas en verde).
- **Deuda técnica**: se registra en [`deuda-tecnica.md`](./deuda-tecnica.md) porque
  arreglarlo cambia el comportamiento o requiere más trabajo del que cabe ahora.

## 1. Defectos encontrados

| # | Defecto | Dónde | Decisión |
|---|---|---|---|
| D1 | Se podían crear reservas en **fechas pasadas**. | `dominio/Reserva.confirmar` | **Corregido**: la regla vive en el dominio y responde 422. Pruebas: `ReservaTest.reservaEnFechaPasadaSeRechaza`, `CasosBordeIntegracionTest.hu4_reservaEnFechaPasadaSeRechazaYNoSePersiste`. |
| D2 | `capacidad = 0` pasaba la validación (`@PositiveOrZero`) pero la BD exige `capacidad > 0`; el `catch` de `EscenarioService.registrar` convertía ese error en un **409 "nombre duplicado"** engañoso. | `web/EscenarioController.CrearEscenarioRequest` | **Corregido**: `@Positive` → 400. Prueba: `CasosBordeIntegracionTest.hu7_escenarioConCapacidadCeroResponde400`. La causa de fondo (el `catch` genérico) queda como S6. |

## 2. Code smells y principios

| # | Smell | Dónde | Principio afectado | Decisión |
|---|---|---|---|---|
| S1 | **Código duplicado**: `findById(id).orElseThrow(...)` repetido 3 veces. | `aplicacion/EscenarioService` | DRY | **Refactorizado**: método privado `buscar(id)`. |
| S2 | **Responsabilidad fuera de lugar**: el controlador armaba a mano la respuesta de disponibilidad con un lambda anidado de 8 líneas, mientras las demás respuestas ya tenían su método `de(...)`. | `web/EscenarioController.disponibilidad` | SRP (SOLID) · Experto en información (GRASP) | **Refactorizado**: `EscenarioDisponibilidadResponse.de(...)` y `FranjaResponse.de(...)`; el endpoint quedó en 3 líneas. |
| S3 | **Código muerto**: constructor secundario de `ReservaResponse` que rellena con `null`; nadie lo usa. | `web/ReservaController.ReservaResponse` | YAGNI | **Refactorizado**: eliminado. |
| S4 | **Comprobaciones de `null` innecesarias** en `ReservaResponse.de` para relaciones que el modelo declara obligatorias (`optional = false`). | `web/ReservaController.ReservaResponse.de` | Claridad | **Deuda técnica** (DT-7): se deja hasta tener más pruebas sobre las respuestas. |
| S5 | **Dos endpoints para la misma operación**: `PATCH /api/reservas/{id}/cancelar` y `DELETE /api/reservas/{id}` hacen exactamente lo mismo. | `web/ReservaController` | Diseño de API (una sola forma de hacer algo) | **Deuda técnica** (DT-6): quitar uno cambia el comportamiento observable, así que se acuerda en equipo. |
| S6 | **Manejo de errores demasiado genérico**: cualquier `DataIntegrityViolationException` se traduce como "nombre duplicado" (fue la causa de D2). `ReservaService` sí revisa el nombre de la restricción; `EscenarioService` no. | `aplicacion/EscenarioService.registrar` | Consistencia · Bajo acoplamiento a supuestos | **Deuda técnica** (DT-4). |
| S7 | **Identidad del usuario en el cuerpo de la petición**: crear, cancelar y ver historial reciben `usuarioId` del cliente; el token del login no se usa. | `web/ReservaController` | Seguridad · Controlador (GRASP) | **Deuda técnica** (DT-1). |
| S8 | **Sin control por rol**: cualquiera puede crear o desactivar escenarios (HU7, HU8 son de administrador). | `web/EscenarioController` | Seguridad · SRP | **Deuda técnica** (DT-2). |

## 3. Lo que está bien (y se mantiene)

- **Arquitectura por capas** (`web` → `aplicacion` → `dominio` → `persistencia`): cada capa
  depende solo de la de abajo (**Bajo acoplamiento**, GRASP).
- **Reglas de negocio en el dominio** (`Reserva.confirmar`, `Reserva.cancelar`,
  `EscenarioDeportivo.admiteReservas`): la entidad que tiene los datos decide
  (**Experto en información**, GRASP).
- **Inyección por constructor** en todos los servicios y controladores (**DIP**, SOLID):
  los servicios se prueban con Mockito sin levantar Spring.
- **Garantía de concurrencia en la base de datos** (índice único parcial): no depende de
  que el código Java "gane la carrera".
- **Manejo de errores centralizado** en `ManejadorErrores` con un cuerpo de error uniforme.

## 4. Refactorización sin alterar el comportamiento

Los cambios S1, S2 y S3 son solo de estructura:

- No cambia ninguna ruta, parámetro, código de estado ni campo del JSON.
- Las pruebas existentes de HU3, HU7 y HU8 (`DisponibilidadEscenariosIntegracionTest`,
  `EscenarioIntegracionTest`, `EscenarioServiceTest`) siguen pasando sin modificarse:
  esa es la evidencia de que el comportamiento observable es el mismo.

Comando de verificación:

```bash
docker compose up -d
./mvnw verify
```
