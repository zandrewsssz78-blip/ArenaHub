# TAREA 5 — Trazabilidad

| elemento del diagrama    | archivo de origen           | línea o sección que lo respalda                                                     |
| ------------------------ | --------------------------- | ----------------------------------------------------------------------------------- |
| `Usuario`                | `docs/modelo-dominio.md`    | Sección de entidades principales del dominio, clase `Usuario`.                      |
| `id: Long`               | `docs/modelo-dominio.md`    | Clase `Usuario`, atributo `id: Long`.                                               |
| `nombre: String`         | `docs/modelo-dominio.md`    | Clase `Usuario`, atributo `nombre: String`.                                         |
| `email: String`          | `docs/modelo-dominio.md`    | Clase `Usuario`, atributo `email: String`.                                          |
| `contrasenaHash: String` | `docs/modelo-dominio.md`    | Clase `Usuario`, atributo `contrasenaHash: String`.                                 |
| `rol: RolUsuario`        | `docs/modelo-dominio.md`    | Clase `Usuario`, atributo `rol: RolUsuario`.                                        |
| `activo: Boolean`        | `docs/modelo-dominio.md`    | Clase `Usuario`, atributo `activo: Boolean`.                                        |
| Registro de usuario      | `docs/historias-usuario.md` | Sección 1. Registro de usuario. Define el registro con nombre, correo y contraseña. |
| `RolUsuario`             | `docs/modelo-dominio.md`    | Sección de enumeraciones y valores del negocio, enumeración `RolUsuario`.           |
| `USUARIOS`               | `docs/esquema-bd.md`        | Sección correspondiente a la tabla `USUARIOS`.                                      |
| `USUARIOS.id`            | `docs/esquema-bd.md`        | Definición del campo `id` de `USUARIOS`.                                            |
| `USUARIOS.nombre`        | `docs/esquema-bd.md`        | Definición del campo `nombre` de `USUARIOS`.                                        |
| `USUARIOS.correo`        | `docs/esquema-bd.md`        | Definición del campo `correo` de `USUARIOS`.                                        |
| `USUARIOS.password_hash` | `docs/esquema-bd.md`        | Definición del campo `password_hash` de `USUARIOS`.                                 |
| `USUARIOS.rol`           | `docs/esquema-bd.md`        | Definición del campo `rol` de `USUARIOS`.                                           |
| `USUARIOS.activo`        | `docs/esquema-bd.md`        | Definición del campo `activo` de `USUARIOS`.                                        |

## VACÍOS DETECTADOS

No se detectan vacíos en los elementos incluidos en el diagrama de clases corregido de la Tarea 3.
