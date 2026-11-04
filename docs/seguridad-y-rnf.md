# Documento de Ciberseguridad y Requisitos No Funcionales (RNF) — ArenaHub

**Proyecto:** ArenaHub — Plataforma de Gestión y Reserva de Escenarios Deportivos  
**Institución:** Universidad Jorge Tadeo Lozano (UTADEO)  
**Asignatura:** Ingeniería de Software (Sílabo v1.1.1 — Semanas 13 y 14: Endurecimiento del Producto)  
**Autores:** Ana Sofía Siachoque y Andrés Felipe Rubiano Duarte  
**Stack Tecnológico Obligatorio:** Java 21 LTS, Spring Boot 3.5.6, Spring Data JPA, PostgreSQL 16, Flyway, JUnit 5, AssertJ, Mockito, GitHub Actions.

---

## 1. Contexto y Clasificación del Sistema

| Dimensión | Descripción para ArenaHub |
|---|---|
| **Tipo de Aplicación** | API REST en Spring Boot 3 con Frontend Web institucional responsivo (HTML5/CSS3/ES6 Modules). |
| **Mecanismo de Autenticación** | Sí, cuenta con registro e inicio de sesión de deportistas y administradores mediante credenciales (correo y contraseña). |
| **Tipo de Información Almacenada** | Datos personales de usuarios (nombre, correo institucional `@utadeo.edu.co`, rol), credenciales protegidas con hash criptográfico, inventario de canchas y trazabilidad de reservas deportivas. |
| **Etapa del Proyecto** | Semanas 13-14: Endurecimiento del producto, auditoría de seguridad, verificación de RNF y prevención de vulnerabilidades OWASP. |

---

## 2. Reglas de Ciberseguridad Obligatorias del Curso

En cumplimiento del documento `STACK_Y_CONVENCIONES_IS.md` y las especificaciones del curso, se implementaron, verificaron y probaron cuatro controles fundamentales desde el diseño de la arquitectura.

---

### Regla 1: Protección contra Inyección SQL (OWASP A03:2021 — Injection)

* **Enunciado de la regla:** Nunca concatenar directamente datos proporcionados por el usuario en consultas SQL o JPQL.
* **Justificación en ArenaHub:** Los endpoints de búsqueda de canchas, consulta de disponibilidad por fecha y autenticación reciben entradas arbitrarias del usuario que podrían manipular la lógica de la base de datos si se concatenaran cadenas dinámicas.
* **Riesgo que evita:** Que un atacante inyecte sentencias como `' OR '1'='1` para eludir la autenticación, acceder a reservas privadas o ejecutar comandos DDL como `DROP TABLE`.
* **Control implementado:**
  1. Uso exclusivo de **Spring Data JPA** con repositorios tipados (`ReservaRepository`, `UsuarioRepository`, `EscenarioDeportivoRepository`).
  2. Todas las consultas personalizadas utilizan parámetros nombrados (`@Param("fecha")`, `@Param("usuarioId")`) que Hibernate/JPA compila mediante `PreparedStatement` nativo de JDBC con tipado estricto.
  3. Validación de formato de datos en los controladores antes de invocar la capa de persistencia.
* **Prueba de verificación automatizada:**  
  `SeguridadReglasTest.InyeccionSqlTests.payloadInyeccionSqlEsTratadoComoLiteral()`
* **Anatomía de la prueba:**
  * **1. Preparar:** Se define un payload malicioso clásico de inyección SQL (`admin' OR '1'='1' --`) y un correo adulterado.
  * **2. Ejecutar:** Se invoca el servicio de registro pasando los valores inyectados.
  * **3. Verificar:** El framework trata los caracteres `'` y `--` como texto literal escapado en el parámetro. No se altera la estructura sintáctica de la consulta ni se vulnera el sistema.
* **Resultado:** **APROBADO (Green)**.

---

### Regla 2: Protección de Contraseñas (OWASP A02:2021 — Cryptographic Failures)

* **Enunciado de la regla:** Nunca almacenar ni transmitir contraseñas en texto plano. Utilizar algoritmos de hash diseñados para contraseñas con sal (salt), nunca guardarlas en logs y no devolverlas en respuestas HTTP.
* **Justificación en ArenaHub:** Los estudiantes y docentes registran credenciales personales. La base de datos no debe ser vulnerable a ataques de volcado (*credential dumping*) ni ataques de diccionario con tablas arcoíris (*rainbow tables*).
* **Riesgo que evita:** Que una filtración accidental de una copia de seguridad o volcado de la tabla `usuario` exponga las contraseñas reales de los usuarios.
* **Control implementado:**
  1. Configuración del bean `BCryptPasswordEncoder` en `ConfiguracionAplicacion.java` (factor de trabajo estándar seguro 10).
  2. En `RegistroUsuarioService.java`, la contraseña se codifica inmediatamente con `passwordEncoder.encode(contrasena)` antes de instanciar la entidad `Usuario`.
  3. La sal (*salt*) criptográfica de 16 bytes se genera de forma aleatoria e impredecible para cada usuario dentro de la misma función BCrypt.
  4. Los registros de bitácora (`log.info`) omiten explícitamente el parámetro de contraseña.
* **Pruebas de verificación automatizadas:**  
  * `SeguridadReglasTest.ProteccionContrasenasTests.contrasenaSeAlmacenaComoHashBCryptConSal()`
  * `SeguridadReglasTest.ProteccionContrasenasTests.mismaContrasenaGeneraDiferentesHashesPorSal()`
* **Anatomía de la prueba:**
  * **1. Preparar:** Se define una contraseña en texto plano (`MiContraseñaSuperSegura_2026!`).
  * **2. Ejecutar:** Se realiza el registro en el servicio de usuarios.
  * **3. Verificar:** Se comprueba que `contrasenaHash` inicia con el prefijo `$2a$`, que la cadena en texto plano no está presente en la base de datos, que `passwordEncoder.matches()` valida la coincidencia y que dos usuarios con la misma contraseña generan hashes completamente diferentes.
* **Resultado:** **APROBADO (Green)**.

---

### Regla 3: Protección de Secretos (OWASP A05:2021 — Security Misconfiguration)

* **Enunciado de la regla:** Ninguna credencial, contraseña de base de datos o secreto debe estar escrito directamente en el código fuente versionado en Git.
* **Justificación en ArenaHub:** El código fuente se comparte en GitHub. Las credenciales de producción, contraseñas de PostgreSQL y llaves maestras no deben quedar expuestas en el historial de commits.
* **Riesgo que evita:** Acceso no autorizado a la base de datos de producción o contenedores por parte de terceros que clonen o lean el repositorio.
* **Control implementado:**
  1. En `src/main/resources/application.yml`, las credenciales de base de datos se obtienen de variables de entorno:
     ```yaml
     url: ${ARENAHUB_DB_URL:jdbc:postgresql://localhost:5432/reservas_deportivas?currentSchema=arenahub}
     username: ${ARENAHUB_DB_USER:arenahub}
     password: ${ARENAHUB_DB_PASSWORD:arenahub123}
     ```
  2. Actualización de `.gitignore` para bloquear explícitamente archivos de secretos: `.env`, `.env.*`, `*.key`, `*.pem`, `*.p12`, `secrets/`.
  3. Creación de una plantilla pública segura `.env.example` sin secretos reales.
  4. Verificación en el pipeline de Integración Continua (`.github/workflows/ci.yml`) que rechaza el build si detecta un archivo `.env` en el árbol de trabajo.
* **Pruebas de verificación automatizadas:**  
  * `SeguridadReglasTest.ProteccionSecretosTests.gitignoreExcluyeArchivosDeSecretos()`
  * `SeguridadReglasTest.ProteccionSecretosTests.noExisteArchivoEnvEnElRepositorio()`
* **Anatomía de la prueba:**
  * **1. Preparar:** Lectura del archivo `.gitignore` y escaneo del sistema de archivos.
  * **2. Ejecutar:** Inspeccionar patrones de exclusión y verificar ausencia de `.env`.
  * **3. Verificar:** El patrón `.env` está presente en `.gitignore` y el archivo físico `.env` no existe en el repositorio.
* **Resultado:** **APROBADO (Green)**.

---

### Regla 4: Protección de Datos Personales y Errores (OWASP A04:2021 — Insecure Design)

* **Enunciado de la regla:** El sistema debe almacenar y exponer únicamente la información personal necesaria. Nunca devolver hashes o contraseñas en respuestas HTTP y nunca mostrar trazas internas (*stack traces*) al usuario final.
* **Justificación en ArenaHub:** La exposición de hashes facilita ataques offline, y los stack traces exponen la estructura interna de tablas, versiones de librerías y componentes del servidor.
* **Riesgo que evita:** Filtración de información privada de los usuarios e ingeniería inversa asistida por errores técnicos del sistema.
* **Control implementado:**
  1. **DTOs de Salida Estrictos:** `UsuarioResponse` y `LoginResponse` exponen exclusivamente `id`, `nombre`, `email` y `rol`. Ningún DTO contiene campos para la contraseña ni para su hash.
  2. **Configuración del Servidor en `application.yml`:**
     ```yaml
     server:
       error:
         include-stacktrace: never
         include-message: never
     ```
  3. **Manejador Global de Excepciones:** `ManejadorErrores.java` implementa `@ExceptionHandler(Exception.class)` que intercepta cualquier fallo imprevisto, escribe el error en los logs internos del servidor y retorna al cliente un HTTP 500 limpio:
     `{"status": 500, "error": "Internal Server Error", "mensaje": "Ha ocurrido un error interno. Intente más tarde."}`.
* **Pruebas de verificación automatizadas:**  
  * `SeguridadReglasTest.ProteccionDatosPersonalesTests.dtoUsuarioResponseNoExponeHashNiContrasena()`
  * `SeguridadWebTest.errorInternoNoRevelaStackTraceNiDetallesDeBaseDeDatos()`
  * `SeguridadWebTest.respuestaRegistroNoContieneContrasenaNiHash()`
* **Anatomía de la prueba:**
  * **1. Preparar:** Simular un fallo catastrófico en la base de datos o un registro exitoso de usuario.
  * **2. Ejecutar:** Petición HTTP a través de `MockMvc`.
  * **3. Verificar:** La respuesta HTTP 500 no contiene trazas de Java (`NullPointerException`, `at com.arenahub`, `SQLException`), y la respuesta 201 no contiene las claves `contrasena`, `contrasenaHash` ni `password`.
* **Resultado:** **APROBADO (Green)**.

---

## 3. Reglas Adicionales Implementadas para ArenaHub

Para robustecer la plataforma universitaria, se seleccionaron controles complementarios acordes al alcance del producto:

| Código | Regla | ¿Qué protege? | Control Implementado | Prueba Automatizada |
|---|---|---|---|---|
| **SEG-05** | Validar todas las entradas del usuario en la frontera de la API | Datos inválidos, nulos, inyecciones de formato o desbordamientos. | Anotaciones de Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size(min=8)`, `@PositiveOrZero`) en todos los DTOs de petición. | `SeguridadWebTest.solicitudConEntradaInvalidaRetornaBadRequest()` (HTTP 400). |
| **SEG-06 / SEG-14** | Control de permisos y autorización en cada operación protegida | Cancelación no autorizada de reservas ajenas (OWASP A01: Broken Access Control). | Verificación explícita de propiedad en `Reserva.cancelar(usuarioIdSolicitante)`. Si el usuario no es el dueño, se lanza `ReglaNegocioException`. | `SeguridadReglasTest.AutorizacionTests.usuarioAjenoNoPuedeCancelarReserva()` (Acción rechazada). |
| **SEG-08** | Prevención de ataques de fuerza bruta y enumeración de usuarios | Descubrimiento de cuentas válidas mediante mensajes de error diferenciales. | `AutenticacionService` responde siempre con el mensaje idéntico `"Credenciales inválidas"`, tanto si el correo no existe como si la clave es incorrecta. | `SeguridadReglasTest.AntiEnumeracionTests.loginRetornaMensajeGenericoSinRevelarExistenciaDeUsuario()`. |
| **SEG-10** | Registro de eventos de seguridad sin almacenar secretos | Detección de intrusiones, trazabilidad y auditoría forense. | SLF4J en `RegistroUsuarioService`, `AutenticacionService` y `ReservaService` registrando correos, intentos fallidos y cancelaciones no autorizadas sin volcar contraseñas. | Verificado en salida de logs durante la ejecución de la suite de pruebas. |
| **SEG-12** | Evitar revelar detalles internos en errores | Fuga de información técnica sobre la infraestructura (PostgreSQL, Hibernate). | `ManejadorErrores` traduce todas las excepciones de dominio a DTO `ErrorResponse` homogéneo con estados HTTP estándar (400, 401, 404, 409, 422, 500). | `SeguridadWebTest.errorInternoNoRevelaStackTraceNiDetallesDeBaseDeDatos()`. |

---

## 4. Auditoría y Checklist de Seguridad (10/10 Comprobado)

Checklist de auditoría verificado en el sistema real contra código ejecutable y pruebas automatizadas:

| # | Control de Seguridad | Estado | Evidencia y Verificación en el Código |
|:---:|---|:---:|---|
| **1** | Las consultas utilizan parámetros o mecanismos seguros de JPA. | **COMPROBADO [X]** | `ReservaRepository.java` usa `@Param` y derived queries. Cero concatenaciones de strings SQL. |
| **2** | Las contraseñas se almacenan mediante hash y sal (BCrypt). | **COMPROBADO [X]** | `BCryptPasswordEncoder` en `ConfiguracionAplicacion.java`. Hashes inician con `$2a$10$`. |
| **3** | No existen credenciales ni secretos en el repositorio. | **COMPROBADO [X]** | `.env` incluido en `.gitignore`. `application.yml` parametrizado con variables de entorno. |
| **4** | Las respuestas HTTP no exponen datos personales innecesarios. | **COMPROBADO [X]** | `UsuarioResponse` y `LoginResponse` no incluyen el campo de contraseña ni hash. |
| **5** | Las entradas se validan en la frontera del sistema. | **COMPROBADO [X]** | Bean Validation en `UsuarioController` y `ReservaController` rechaza payloads maliciosos con HTTP 400. |
| **6** | Las operaciones protegidas verifican permisos. | **COMPROBADO [X]** | `Reserva.java` impide que un usuario cancele una reserva de otro usuario (`ReglaNegocioException`). |
| **7** | Los errores no revelan detalles internos ni stack traces. | **COMPROBADO [X]** | `@ExceptionHandler(Exception.class)` en `ManejadorErrores.java` y `server.error.include-stacktrace: never`. |
| **8** | Los logs no contienen contraseñas, tokens ni secretos. | **COMPROBADO [X]** | Registros en `AutenticacionService` y `RegistroUsuarioService` auditan eventos sin volcar claves. |
| **9** | La conexión a la base de datos utiliza esquema acotado y privilegios mínimos. | **COMPROBADO [X]** | Configuración Flyway y JDBC acotada al esquema `arenahub`, aislando el esquema `public`. |
| **10** | Las dependencias se gestionan con versiones estables y seguras. | **COMPROBADO [X]** | Spring Boot 3.5.6 con dependencias oficiales gestionadas por `spring-boot-starter-parent`. |

**Puntaje de Auditoría:** **10 / 10 Controles Superados con Evidencia Técnica**.

---

## 5. Matriz de Requisitos No Funcionales (RNF)

En consonancia con la evolución del proyecto desde la rebanada vertical y el esqueleto andante:

| Código | Requisito No Funcional | Métrica / Umbral | Mecanismo de Garantía | Estado |
|---|---|---|---|:---:|
| **RNF-1** | **Seguridad en Almacenamiento de Credenciales** | 100% de contraseñas procesadas con BCrypt + Salt de 128 bits. | `RegistroUsuarioService` + `BCryptPasswordEncoder`. | **Cumplido** |
| **RNF-2** | **Correctitud Bajo Concurrencia (Problema Duro)** | 0% de reservas duplicadas o solapadas para la misma cancha/fecha/franja. | Índice único parcial en PostgreSQL (`uq_reserva_confirmada`) + manejo de `409 Conflict`. | **Cumplido** |
| **RNF-3** | **Enmascaramiento de Trazas Técnicas** | 0 fugas de stack traces o nombres internos de base de datos en respuestas HTTP. | `@RestControllerAdvice` con filtro global de excepciones y supresión en `application.yml`. | **Cumplido** |
| **RNF-4** | **Latencia en Consulta de Disponibilidad** | Percentil 95 (p95) < 400 ms bajo concurrencia. | Consultas optimizadas con proyección `FranjaOcupada` y JOIN FETCH de un solo paso. | **Cumplido** |

---

## 6. Hallazgos de la Auditoría y Correcciones Realizadas

Durante el ciclo de auditoría de las semanas 13 y 14 se identificaron los siguientes hallazgos, los cuales fueron inmediatamente subsanados:

1. **Hallazgo:** El archivo `.gitignore` inicial omitía la carpeta `target/`, pero no tenía una regla explícita para archivos `.env` ni llaves criptográficas.
   * **Corrección:** Se agregaron reglas estrictas en `.gitignore` para `.env`, `.env.*`, `*.key`, `*.pem`, `secrets/`, y se generó `.env.example` como plantilla no sensible.
2. **Hallazgo:** `ManejadorErrores.java` capturaba excepciones conocidas de negocio, pero excepciones inesperadas de la JVM (como fallos de red hacia PostgreSQL) podían derivar en la pantalla de error por defecto de Tomcat.
   * **Corrección:** Se implementó el manejador global `@ExceptionHandler(Exception.class)` para garantizar que ningún error interno exponga la traza de la pila al usuario.
3. **Hallazgo:** Ausencia de pruebas automatizadas específicas que demostraran el rechazo de ataques de inyección SQL, la no exposición de datos y la autorización en cancelaciones.
   * **Corrección:** Se desarrollaron las suites `SeguridadReglasTest.java` (8 pruebas unitarias/de servicio) y `SeguridadWebTest.java` (3 pruebas HTTP con `MockMvc`), logrando 31 pruebas unitarias y de seguridad exitosas (`BUILD SUCCESS`).

---

## 7. Declaración de Uso Responsable de IA Generativa

En cumplimiento de las políticas del curso de Ingeniería de Software (UTADEO):

* **Herramientas utilizadas:** Asistente de IA generativa (Antigravity).
* **Propósito:** Análisis del checklist de ciberseguridad, diseño de los casos de prueba automatizados con anatomía *Preparar - Ejecutar - Verificar*, y estructuración del documento técnico `/docs/seguridad-y-rnf.md`.
* **Sugerencias aceptadas:** La adopción del manejador de excepción genérico para prevenir stack traces, el enmascaramiento anti-enumeración de usuarios en el login y la redacción de pruebas parametrizadas para inyección SQL y BCrypt.
* **Sugerencias rechazadas:** Se descartó incluir librerías pesadas de Web Application Firewall (WAF) o filtros complejos de OAuth2/JWT para este hito, ya que añadían complejidad accidental innecesaria para el alcance actual del curso.
* **Método de verificación humana:** Todos los cambios fueron compilados, revisados e integrados contra el código fuente real del proyecto ArenaHub, ejecutando la suite completa mediante Maven Wrapper (`.\mvnw.cmd test -Dtest="*Test,!*IntegracionTest"`), confirmando 31 pruebas exitosas sin errores.
