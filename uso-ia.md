# Declaración de Uso Responsable de IA Generativa — ArenaHub

## 1. Política Inicial del Equipo

La inteligencia artificial generativa se utilizará como una herramienta de apoyo durante el desarrollo de ArenaHub, sin sustituir las decisiones ni la responsabilidad del equipo.

El equipo establece las siguientes reglas:

- La IA se utilizará como herramienta de apoyo para aclarar conceptos de Ingeniería de Software, analizar alternativas técnicas, revisar errores y proponer casos de prueba.

- Ningún código, diseño o prueba generado con ayuda de IA será incorporado al proyecto sin que un integrante del equipo lo revise, ejecute, comprenda y verifique.

- Las decisiones finales sobre arquitectura, implementación y alcance serán tomadas por los integrantes del equipo.

- Cada integrante deberá ser capaz de explicar y defender las decisiones y el código asociado a su trabajo durante la defensa del proyecto.

---

## 2. Registro de Uso de IA — Semana 2

| Pregunta / Campo | Respuesta registrada |
|---|---|
| **1. ¿Qué herramienta se usó?** | ChatGPT |
| **2. ¿Para qué se usó?** | Para analizar alternativas de problemas duros para ArenaHub y determinar cuál se adapta mejor al sistema de reservas de escenarios deportivos. |
| **3. ¿Qué se aceptó?** | La propuesta de utilizar la concurrencia como problema duro, debido a que varias solicitudes pueden intentar reservar simultáneamente una misma cancha para la misma fecha y franja horaria. |
| **4. ¿Qué se modificó?** | La propuesta se adaptó al dominio de ArenaHub, definiendo el escenario de concurrencia sobre canchas, fechas, franjas horarias y reservas, así como el invariante de negocio que debe cumplirse. |
| **5. ¿Qué se rechazó?** | Se descartaron alternativas de problemas técnicos que no fueran proporcionales al alcance definido para el MVP. |
| **6. ¿Cómo se verificó?** | La propuesta se contrastó con los requisitos planteados para ArenaHub. La prueba automatizada de concurrencia queda pendiente de implementación y ejecución para verificar el comportamiento real del sistema. |
| **7. ¿Cuál fue la decisión humana final?** | El equipo decidió utilizar la concurrencia en las reservas como el problema duro principal de ArenaHub. |
| **8. ¿Qué riesgo permanece?** | Se debe validar durante la implementación cuál mecanismo de protección resulta más adecuado para garantizar la consistencia de las reservas y comprobar su comportamiento mediante la prueba concurrente. |

---

## 3. Uso Responsable de la IA

La información obtenida mediante herramientas de inteligencia artificial generativa será considerada un recurso de apoyo y no sustituirá las decisiones, la implementación ni la validación realizadas por el equipo.

Antes de incorporar cualquier propuesta generada con IA al proyecto, los integrantes deberán:

1. Revisar la propuesta.
2. Comprender su funcionamiento.
3. Adaptarla al contexto de ArenaHub cuando sea necesario.
4. Ejecutarla cuando corresponda.
5. Verificar que su comportamiento sea correcto.
6. Ser capaces de explicar y defender la decisión tomada.

La responsabilidad final sobre el código, los diseños, las decisiones técnicas y los resultados del proyecto corresponde al equipo.

---

## 4. Estado de Verificación del Registro

| Elemento | Estado |
|---|---|
| Herramienta de IA utilizada | Confirmado |
| Análisis de alternativas de problemas duros | Realizado |
| Concurrencia seleccionada como problema duro | Confirmado |
| Adaptación de la propuesta al dominio ArenaHub | Realizada |
| Decisión humana final | Confirmada |
| Prueba automatizada de concurrencia | Pendiente de implementación |
| Ejecución de la prueba concurrente | Pendiente |
| Validación del mecanismo de protección | Pendiente |
| Actualización del registro durante el desarrollo | Pendiente de futuras interacciones |

---

## 5. Actualización del Registro

Este documento deberá actualizarse durante el desarrollo del proyecto para documentar las interacciones reales que el equipo tenga con herramientas de inteligencia artificial.

No se registrarán como realizadas pruebas, verificaciones, implementaciones o decisiones que todavía no hayan ocurrido.

Cuando una propuesta generada mediante IA sea:

- aceptada;
- modificada;
- rechazada;
- implementada; o
- validada,

la decisión correspondiente deberá registrarse de acuerdo con lo que realmente haya ocurrido durante el desarrollo.

El registro deberá mantenerse actualizado para garantizar la trazabilidad del uso de IA durante el proyecto.