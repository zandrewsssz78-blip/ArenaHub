# Visión del Producto — ArenaHub

## 1. Declaración del Problema

La gestión de reservas de escenarios deportivos puede realizarse mediante mensajes, llamadas, hojas de cálculo u otros procesos manuales. Estos mecanismos dificultan mantener un registro centralizado de las reservas y consultar de manera confiable la disponibilidad de los escenarios.

Uno de los principales problemas ocurre cuando dos o más usuarios intentan reservar la misma cancha para la misma fecha y franja horaria prácticamente al mismo tiempo. Si las solicitudes no son procesadas de manera segura, existe el riesgo de que más de una solicitud sea aceptada para un único espacio disponible.

Esto puede generar:

- conflictos entre usuarios;
- reservas duplicadas;
- inconsistencias en la información de las reservas;
- información incorrecta sobre la disponibilidad de los escenarios deportivos.

ArenaHub busca abordar este problema mediante un sistema centralizado que permita consultar la disponibilidad y gestionar las reservas, incorporando un mecanismo de control de concurrencia que será implementado y posteriormente validado mediante pruebas automatizadas.

---

## 2. Usuarios y Actores Principales

### Jugador / Usuario

**Rol:** Persona que utiliza ArenaHub para consultar y gestionar sus reservas de escenarios deportivos.

**Acción principal dentro del sistema:**

- Consultar escenarios disponibles.
- Seleccionar una fecha y franja horaria.
- Realizar reservas.
- Consultar sus reservas.
- Cancelar sus propias reservas.

**Valor que obtiene:** Puede gestionar sus reservas y consultar la disponibilidad de los escenarios desde un sistema centralizado.

---

### Entrenador

**Rol:** Usuario que utiliza ArenaHub para gestionar reservas de escenarios deportivos destinados a entrenamientos, prácticas o actividades programadas.

**Acción principal dentro del sistema:**

- Consultar la disponibilidad de escenarios.
- Seleccionar fechas y franjas horarias.
- Realizar reservas.
- Consultar y cancelar sus reservas.

**Valor que obtiene:** Puede gestionar los escenarios necesarios para sus actividades deportivas mediante un sistema centralizado de reservas.

---

### Administrador

**Rol:** Responsable de gestionar la información necesaria para el funcionamiento de los escenarios deportivos y sus reservas.

**Acción principal dentro del sistema:**

- Registrar y consultar escenarios deportivos.
- Configurar su disponibilidad.
- Consultar reservas.
- Gestionar la información necesaria para la operación del sistema.

**Valor que obtiene:** Dispone de herramientas para administrar los escenarios y la información relacionada con las reservas desde un sistema centralizado.

---

## 3. Propuesta de Valor

### PARA

Jugadores, entrenadores y administradores de escenarios deportivos.

### QUE NECESITAN

Consultar la disponibilidad de las canchas y gestionar reservas evitando conflictos de horario y reservas duplicadas.

### EL SISTEMA

**ArenaHub** es una plataforma web para la gestión y reserva de escenarios deportivos.

### QUE

Permite:

- consultar la disponibilidad de los escenarios;
- realizar reservas;
- cancelar reservas;
- consultar el historial de reservas;
- gestionar escenarios y reservas según el rol del usuario;
- controlar las solicitudes concurrentes que intenten reservar una misma cancha, fecha y franja horaria.

### A DIFERENCIA DE

Los mensajes, llamadas, hojas de cálculo y otros registros manuales utilizados para coordinar las reservas.

### NUESTRO PRODUCTO

ArenaHub busca proporcionar una gestión centralizada de las reservas y controlar las solicitudes concurrentes para evitar que una misma cancha sea confirmada para dos usuarios en la misma fecha y franja horaria.

La efectividad del mecanismo de control de concurrencia será validada mediante una prueba automatizada de integración que verificará el comportamiento del sistema ante múltiples solicitudes simultáneas.

---

## 4. Alcance del MVP

### 4.1 Incluido

Las siguientes funcionalidades y capacidades forman parte del alcance definido para el MVP:

- Registro e inicio de sesión de usuarios.
- Registro y consulta de escenarios deportivos.
- Consulta de disponibilidad de canchas por fecha y franja horaria.
- Creación de reservas.
- Cancelación de reservas.
- Consulta del historial de reservas.
- Gestión de escenarios y reservas por parte del administrador.
- Control de concurrencia para prevenir reservas duplicadas sobre una misma cancha, fecha y franja horaria durante la ejecución simultánea de solicitudes.
- Prueba automatizada de integración para validar el comportamiento del sistema ante solicitudes concurrentes.

---

### 4.2 Excluido

Las siguientes funcionalidades quedan fuera del alcance del MVP:

- Procesamiento de pagos en línea.
- Control físico de acceso a las canchas.
- Integración con sensores IoT para detectar ocupación de los escenarios.
- Sistemas de cámaras o vigilancia.
- Aplicaciones móviles nativas para Android o iOS durante el MVP.

---

## 5. Estado de Verificación

| Elemento | Estado |
|---|---|
| Problema de reservas y disponibilidad definido | Confirmado |
| Riesgo de reservas duplicadas bajo concurrencia | Confirmado como problema técnico seleccionado |
| Jugador / Usuario definido como actor | Confirmado |
| Entrenador definido como actor | Confirmado |
| Administrador definido como actor | Confirmado |
| Propuesta de valor definida | Confirmado |
| Alcance del MVP definido | Confirmado |
| Funcionalidades incluidas | Definidas por el equipo |
| Funcionalidades excluidas | Definidas por el equipo |
| Mecanismo específico de control de concurrencia | Pendiente de selección |
| Implementación del control de concurrencia | Pendiente |
| Prueba automatizada de integración | Pendiente de implementación |
| Validación del comportamiento concurrente | Pendiente |

---

## 6. Límites de Verificación

La existencia de la propuesta de control de concurrencia no implica que su funcionamiento ya haya sido demostrado.

La implementación deberá comprobar posteriormente que el sistema mantiene el siguiente comportamiento:

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
