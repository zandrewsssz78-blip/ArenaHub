# Guía de instalación — ArenaHub

Con esta guía se levanta ArenaHub desde cero en un equipo nuevo: base de datos, backend y
frontend, más las pruebas. El tiempo esperado es de 10 a 15 minutos; la primera ejecución
descarga Maven y las dependencias.

## 1. Requisitos previos

| Herramienta | Versión | Cómo comprobarla |
|---|---|---|
| Git | Cualquiera reciente | `git --version` |
| Java (JDK) | 21 LTS (probado con Eclipse Temurin 21.0.12) | `java -version` |
| Docker Desktop / Docker Engine | Engine 24+ con Compose v2 | `docker compose version` |
| Navegador | Chrome, Edge o Firefox actuales | — |

No hace falta instalar Maven ni PostgreSQL:
- Maven 3.9.11 lo descarga el wrapper (`mvnw` / `mvnw.cmd`).
- PostgreSQL 16 corre en un contenedor de Docker.

Puertos que deben estar libres: **5432** (PostgreSQL) y **8080** (aplicación).

## 2. Instalación paso a paso

### 2.1 Clonar el repositorio

```bash
git clone https://github.com/zandrewsssz78-blip/ArenaHub.git
cd ArenaHub
```

En Windows se recomienda clonar fuera de OneDrive (por ejemplo `C:\proyectos`), porque la
sincronización puede bloquear archivos mientras Maven compila.

### 2.2 Levantar la base de datos

Docker Desktop debe estar abierto ("Engine running").

```bash
docker compose up -d
```

Crea el contenedor `arenahub-db` (imagen `postgres:16`) con la base `reservas_deportivas`, el
usuario `arenahub` y un volumen `db_data` para que los datos sobrevivan a reinicios.
Comprobación: `docker ps` debe mostrar `arenahub-db` con estado `Up`.

### 2.3 Levantar la aplicación

| Sistema | Comando |
|---|---|
| Mac / Linux | `./mvnw spring-boot:run` |
| Windows (PowerShell) | `.\mvnw.cmd spring-boot:run` |

Al arrancar, **Flyway** crea las tablas en el esquema `arenahub` y carga los datos semilla
(`src/main/resources/db/migration`). La aplicación está lista cuando la consola muestra
`Started ArenaHubApplication`.

### 2.4 Abrir la interfaz

Abrir **http://localhost:8080**. El frontend lo sirve el mismo Spring Boot desde
`src/main/resources/static`; no hay que instalar nada más.

Para detener la aplicación: `Ctrl + C` en su terminal. Para detener la base de datos:
`docker compose stop` (los datos se conservan).

### 2.5 Ejecutar las pruebas

Con la base de datos del paso 2.2 arriba:

```bash
./mvnw verify          # Windows: .\mvnw.cmd verify
```

Las pruebas de integración usan el esquema `arenahub_test`, que se borra y se vuelve a migrar
en cada ejecución, así que no tocan los datos de la aplicación.

Solo la prueba del problema duro (20 solicitudes concurrentes):

```bash
./mvnw -Dtest=CrearReservaIntegracionTest test
```

## 3. Configuración

### 3.1 Variables de entorno

Todas son opcionales; los valores por defecto coinciden con `docker-compose.yml`.

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `ARENAHUB_DB_URL` | `jdbc:postgresql://localhost:5432/reservas_deportivas?currentSchema=arenahub` | Conexión de la aplicación |
| `ARENAHUB_DB_USER` | `arenahub` | Usuario de la base de datos |
| `ARENAHUB_DB_PASSWORD` | `arenahub123` | Contraseña de la base de datos |
| `ARENAHUB_TEST_DB_URL` | `jdbc:postgresql://localhost:5432/reservas_deportivas?currentSchema=arenahub_test` | Conexión de las pruebas |

### 3.2 Archivos de configuración

| Archivo | Contenido |
|---|---|
| `docker-compose.yml` | Contenedor PostgreSQL 16, puerto 5432, volumen `db_data` |
| `src/main/resources/application.yml` | Datasource, Flyway (esquema `arenahub`), JPA en modo `validate`, puerto 8080 |
| `src/test/resources/application-test.yml` | Esquema de pruebas `arenahub_test` |
| `src/main/resources/db/migration/V1__esquema_inicial.sql` | Tablas, restricciones e índice `uq_reserva_confirmada` |
| `src/main/resources/db/migration/V2__datos_semilla.sql` | 16 franjas (06:00–22:00) y 3 escenarios de ejemplo |

### 3.3 Crear un usuario administrador

El registro desde la interfaz siempre crea usuarios con rol `JUGADOR`. Para usar la pantalla
**Escenarios** (HU7 y HU8), se registra la cuenta y luego se cambia su rol:

```bash
docker exec arenahub-db psql -U arenahub -d reservas_deportivas \
  -c "UPDATE arenahub.usuario SET rol = 'ADMINISTRADOR' WHERE email = 'correo@ejemplo.com';"
```

Debe responder `UPDATE 1`. Después hay que cerrar sesión en la interfaz y volver a entrar.

## 4. Solución de problemas

| Síntoma | Causa | Solución |
|---|---|---|
| `failed to connect to the docker API ... dockerDesktopLinuxEngine` | Docker Desktop está cerrado | Abrirlo y esperar "Engine running". |
| `service "db" is not running` al usar `docker compose exec` | El contenedor se creó desde otra carpeta del proyecto (Compose agrupa por nombre de carpeta) | Usar el nombre del contenedor: `docker exec arenahub-db ...`, o `docker start arenahub-db`. |
| `Conflict. The container name "/arenahub-db" is already in use` | Ya existe un contenedor `arenahub-db` de otra copia del repositorio | `docker start arenahub-db` y seguir usando ese contenedor. |
| `port 5432 is already allocated` | Hay otro PostgreSQL local usando el puerto | Detener ese servicio o cambiar el puerto en `docker-compose.yml` y en `ARENAHUB_DB_URL`. |
| `./mvnw: Permission denied` (Mac / Linux) | El archivo no tiene permiso de ejecución (DT-9) | `chmod +x mvnw` |
| Windows: `mvnw.cmd` responde "Acceso denegado", "El parámetro no es correcto" o termina sin mensajes | La variable de entorno `ComSpec` está dañada | En PowerShell, `$env:ComSpec` debe ser `C:\WINDOWS\system32\cmd.exe`. Corregirla en *Variables de entorno* con el valor `%SystemRoot%\system32\cmd.exe`, sin espacios, y reiniciar la terminal. |
| Windows: `cmd` no se reconoce | Faltan las carpetas de Windows en `Path` | Agregar `%SystemRoot%\system32`, `%SystemRoot%`, `%SystemRoot%\System32\Wbem` y `%SystemRoot%\System32\WindowsPowerShell\v1.0\` a `Path` y reiniciar la terminal. |
| La aplicación no arranca y menciona `Connection refused` | La base de datos no está arriba | Paso 2.2. |
| La interfaz dice "No hay conexión con el servidor" | La aplicación se detuvo | Paso 2.3. |
| `Web server failed to start. Port 8080 was already in use` | Otra instancia de la aplicación sigue corriendo | Cerrar la otra terminal o el proceso Java. |
| No aparece la pestaña **Escenarios** | El usuario no es administrador, o inició sesión antes del cambio de rol | Punto 3.3 y volver a iniciar sesión. |
