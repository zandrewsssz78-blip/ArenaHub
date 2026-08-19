# Declaración del Problema Duro — ArenaHub

## 1. Problema Técnico Elegido

**Concurrencia / Condiciones de Carrera:** prevención de reservas duplicadas de una misma cancha cuando se procesan solicitudes de reserva de manera simultánea.

---

## 2. Descripción del Desafío Técnico

Cuando múltiples usuarios consultan y reservan escenarios deportivos simultáneamente, existe el riesgo de que dos o más solicitudes intenten reservar la misma cancha para la misma fecha y franja horaria.

Por ejemplo, dos usuarios pueden consultar al mismo tiempo la disponibilidad de la Cancha 1 para la franja de **6:00 PM a 7:00 PM** y ambos recibir una respuesta indicando que está disponible. Si el sistema no controla correctamente la concurrencia, las dos solicitudes podrían intentar crear una reserva para la misma cancha, fecha y franja horaria.

Esto produciría una inconsistencia en el sistema, ya que dos usuarios podrían recibir confirmación para un único espacio disponible.

El problema duro no consiste simplemente en registrar una reserva, sino en **garantizar que, ante solicitudes concurrentes, como máximo una reserva quede confirmada para una misma cancha, fecha y franja horaria**.

Para efectos del MVP, se considera que existe un conflicto cuando dos o más reservas corresponden a:

- la misma cancha;
- la misma fecha;
- la misma franja horaria.

---

## 3. Invariante del Negocio

ArenaHub deberá mantener las siguientes condiciones:

- Para una cancha determinada, una fecha y una franja horaria, **únicamente una (1) reserva puede quedar confirmada**.

- Ante **N solicitudes simultáneas** que intenten reservar la misma cancha, en la misma fecha y franja horaria, **exactamente una (1) debe prosperar y las restantes N-1 deben ser rechazadas correctamente**.

- El sistema **nunca debe dejar en la base de datos dos reservas confirmadas** para la misma cancha, fecha y franja horaria.

Este invariante debe mantenerse incluso cuando las solicitudes de reserva sean procesadas de manera concurrente.

---

## 4. Evidencia Específica Exigida

Para comprobar el comportamiento de ArenaHub frente a solicitudes concurrentes, se implementará una **prueba automatizada de integración**.

La prueba utilizará:

- `CountDownLatch` para coordinar el inicio de las solicitudes concurrentes.
- Un pool de **N = 20 hilos**.
- 20 solicitudes que intentarán reservar simultáneamente:
  - la misma cancha;
  - la misma fecha;
  - la misma franja horaria.

El escenario de prueba será:

```text
20 solicitudes concurrentes
          ↓
     Misma cancha
          ↓
      Misma fecha
          ↓
    Misma franja horaria
          ↓
       ArenaHub
          ↓
1 reserva → APROBADA
19 reservas → RECHAZADAS
          ↓
   Base de datos
          ↓
1 única reserva confirmada