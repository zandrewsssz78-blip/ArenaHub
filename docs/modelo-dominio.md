<<<<<<< HEAD
classDiagram
    direction TB

    %% --- ENTIDADES PRINCIPALES DEL DOMINIO ---
    class Usuario {
        id: Long
        nombre: String
        email: String
        contrasenaHash: String
        rol: RolUsuario
        activo: Boolean
    }

    class EscenarioDeportivo {
        id: Long
        nombre: String
        tipo: TipoEscenario
        ubicacion: String
        capacidad: Integer
        descripcion: String
        estado: EstadoEscenario
    }

    class Reserva {
        id: Long
        codigoReserva: String
        estado: EstadoReserva
        fechaCreacion: LocalDateTime
        observacion: String
    }

    class FranjaHoraria {
        fecha: LocalDate
        horaInicio: LocalTime
        horaFin: LocalTime
    }

    %% --- ENUMERACIONES Y VALORES DEL NEGOCIO ---
=======
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
>>>>>>> 0b937e2487656bfad06bbd6eb7faa214a23fb7b4
    class RolUsuario {
        <<enumeration>>
        JUGADOR
        ENTRENADOR
        ADMINISTRADOR
    }
<<<<<<< HEAD

=======
>>>>>>> 0b937e2487656bfad06bbd6eb7faa214a23fb7b4
    class TipoEscenario {
        <<enumeration>>
        FUTBOL
        FUTBOL_5
        BALONCESTO
        VOLEIBOL
        TENIS
        OTRO
    }
<<<<<<< HEAD

=======
>>>>>>> 0b937e2487656bfad06bbd6eb7faa214a23fb7b4
    class EstadoReserva {
        <<enumeration>>
        PENDIENTE
        CONFIRMADA
        CANCELADA
        RECHAZADA
    }
<<<<<<< HEAD

=======
>>>>>>> 0b937e2487656bfad06bbd6eb7faa214a23fb7b4
    class EstadoEscenario {
        <<enumeration>>
        ACTIVO
        INACTIVO
        EN_MANTENIMIENTO
    }
<<<<<<< HEAD

    %% --- ESTRUCTURA DE RELACIONES Y NÚCLEO ---
    Usuario "1" --> "0..*" Reserva : realiza
    EscenarioDeportivo "1" --> "0..*" Reserva : alberga
    Reserva "1" *-- "1" FranjaHoraria : compuesta por

    %% --- NOTAS FORMALES (INVARIANTES vs PRUEBAS DE EVIDENCIA) ---
    note for EscenarioDeportivo "REGLA DE DISPONIBILIDAD (HU3, HU8):\nSolo los escenarios con EstadoEscenario = ACTIVO\npueden ser consultados y recibir reservas."

    note for FranjaHoraria "VALUE OBJECT TEMPORAL:\nReifica el intervalo [horaInicio, horaFin]\nen una fecha dada para validar el solapamiento."

    note for Reserva "INVARIANTE DE DOMINIO (Regla de Negocio):\nPara un mismo EscenarioDeportivo y fecha, NO PUEDEN\ncoexistir dos instancias de Reserva en estado CONFIRMADA\ncuyas FranjaHoraria se solapen temporalmente.\n\nEVIDENCIA Y CONCURRENCIA (HU4 - Criterio 3):\nAnte 20 solicitudes simultáneas para la misma franja:\n- Exactamente 1 pasa a estado CONFIRMADA.\n- 19 son desestimadas (las rechazadas por concurrencia síncrona NO persisten en BD)."
=======
    Usuario --> Reserva : realiza
    EscenarioDeportivo --> Reserva : alberga
    FranjaHoraria --> Reserva : define
    
    note for EscenarioDeportivo "Solo escenarios ACTIVOS reciben reservas."
    note for FranjaHoraria "Define bloques temporales y evita solapamientos."
    note for Reserva "Invariante: No pueden coexistir dos reservas CONFIRMADAS para el mismo escenario, fecha y franja."
    ```
    
>>>>>>> 0b937e2487656bfad06bbd6eb7faa214a23fb7b4
