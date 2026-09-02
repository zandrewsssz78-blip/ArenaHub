```mermaid
classDiagram
    direction TB
    class Usuario {
        +Long id
        +String nombre
        +String email
        +String contrasenaHash
        +RolUsuario rol
        +Boolean activo
    }
    class EscenarioDeportivo {
        +Long id
        +String nombre
        +TipoEscenario tipo
        +String ubicacion
        +Integer capacidad
        +String descripcion
        +EstadoEscenario estado
    }
    class Reserva {
        +Long id
        +String codigoReserva
        +Date fecha
        +EstadoReserva estado
        +LocalDateTime fechaCreacion
        +String observacion
    }
    class FranjaHoraria {
        +Long id
        +Time horaInicio
        +Time horaFin
    }
    class RolUsuario {
        <<enumeration>>
        JUGADOR
        ENTRENADOR
        ADMINISTRADOR
    }
    class TipoEscenario {
        <<enumeration>>
        FUTBOL
        FUTBOL_5
        BALONCESTO
        VOLEIBOL
        TENIS
        OTRO
    }
    class EstadoReserva {
        <<enumeration>>
        PENDIENTE
        CONFIRMADA
        CANCELADA
        RECHAZADA
    }
    class EstadoEscenario {
        <<enumeration>>
        ACTIVO
        INACTIVO
        EN_MANTENIMIENTO
    }
    Usuario --> Reserva : realiza
    EscenarioDeportivo --> Reserva : alberga
    FranjaHoraria --> Reserva : define
    
    note for EscenarioDeportivo "Solo escenarios ACTIVOS reciben reservas."
    note for FranjaHoraria "Define bloques temporales y evita solapamientos."
    note for Reserva "Invariante: No pueden coexistir dos reservas CONFIRMADAS para el mismo escenario, fecha y franja."
    ```
    