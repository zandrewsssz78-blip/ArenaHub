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
| Prueba automatizada de concurrencia | Implementada (`CrearReservaIntegracionTest`) |
| Ejecución de la prueba concurrente | Realizada: 1 confirmada, 19 rechazadas, 1 fila persistida |
| Validación del mecanismo de protección | Validado: índice único parcial `uq_reserva_confirmada` |
| Actualización del registro durante el desarrollo | Actualizado en la semana 15 (sección 6) |

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

---

## 6. Registro de Uso de IA — Semana 15 (frontend y documentación final)

### 6.1 Frontend web

| Pregunta / Campo | Respuesta registrada |
|---|---|
| **1. ¿Qué herramienta se usó?** | Claude (Anthropic) |
| **2. ¿Para qué se usó?** | Para construir la interfaz web (HTML, CSS y JavaScript) sobre la API REST ya existente, sin modificar el backend. |
| **3. ¿Qué se aceptó?** | Un frontend sin frameworks servido por Spring Boot desde `src/main/resources/static`, con pantallas de acceso, reservar, mis reservas y escenarios, y el manejo del 409 del problema duro como "Alguien reservó primero". |
| **4. ¿Qué se modificó?** | Se ajustaron detalles visuales tras revisar capturas en computador y celular, y se agregó al README la sección "Interfaz web" con el procedimiento para volver administrador a un usuario. |
| **5. ¿Qué se rechazó?** | Usar un framework (React o Angular), porque añadía un servidor y un build aparte y rompía el contrato de ejecución de 2 comandos. Tampoco se cambió el backend para validar roles: se dejó como DT-2. |
| **6. ¿Cómo se verificó?** | Se ejecutó la aplicación real (`mvnw spring-boot:run` con PostgreSQL en Docker) y se probaron los flujos HU1–HU8 desde el navegador. El cambio de rol a administrador se hizo directamente en PostgreSQL. |
| **7. ¿Cuál fue la decisión humana final?** | Integrar el frontend mediante la rama `feature/frontend` y un Pull Request revisado por el equipo. |
| **8. ¿Qué riesgo permanece?** | La interfaz oculta la administración a quien no es administrador, pero el backend no lo impide (DT-1, DT-2). |

### 6.2 Documentación final

| Pregunta / Campo | Respuesta registrada |
|---|---|
| **1. ¿Qué herramienta se usó?** | Claude (Anthropic) |
| **2. ¿Para qué se usó?** | Para redactar la guía de instalación, el manual de usuario, el informe técnico y el paquete de entrevista a partir del código y los documentos existentes. |
| **3. ¿Qué se aceptó?** | La estructura de los documentos y la consolidación de decisiones, pruebas y deuda en un solo informe. |
| **4. ¿Qué se modificó?** | Se incluyeron en la guía de instalación los problemas reales encontrados al ejecutar el proyecto en Windows (`Path` y `ComSpec` dañados, contenedor creado desde otra carpeta). |
| **5. ¿Qué se rechazó?** | Afirmaciones que no estuvieran respaldadas por el repositorio: el RNF-1 se reporta como pendiente de medir, no como cumplido. |
| **6. ¿Cómo se verificó?** | Se contrastaron los datos (endpoints, número de pruebas, restricciones, mensajes de error) con el código fuente y se siguió la guía de instalación en un clon limpio. |
| **7. ¿Cuál fue la decisión humana final?** | Publicar los documentos en `/docs` mediante el Pull Request de documentación final. |
| **8. ¿Qué riesgo permanece?** | Los documentos deben actualizarse cuando se pague la deuda técnica (por ejemplo, al implementar JWT cambia el manual de usuario). |

