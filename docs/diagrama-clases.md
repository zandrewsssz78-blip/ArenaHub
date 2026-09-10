# TAREA 3 — Diagrama de clases (derivado)

## Rebanada seleccionada

Registro de un usuario.

## Diagrama de clases

```mermaid
classDiagram

    class Usuario {
        <<Domain>>
        +Long id
        +String nombre
        +String email
        +String contrasenaHash
        +RolUsuario rol
        +Boolean activo
    }

    class RegistroUsuarioHTTP {
        <<HTTP/UI>>
        +registrarUsuario()
    }

    class RegistroUsuarioAplicacion {
        <<Application>>
        +registrarUsuario()
    }

    class PersistenciaUsuario {
        <<Port>>
        +guardar(Usuario)
    }

    class PersistenciaUsuarioImpl {
        <<Implementation>>
        +guardar(Usuario)
    }

    RegistroUsuarioHTTP --> RegistroUsuarioAplicacion : solicita registro
    RegistroUsuarioAplicacion --> Usuario : crea/usa
    RegistroUsuarioAplicacion --> PersistenciaUsuario : guarda
    PersistenciaUsuario <|.. PersistenciaUsuarioImpl : implementa
    PersistenciaUsuarioImpl --> Usuario : mapea