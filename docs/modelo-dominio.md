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

    %% --- ESTRUCTURA DE RELACIONES Y NÚCLEO ---
    Usuario "1" --> "0..*" Reserva : realiza
    EscenarioDeportivo "1" --> "0..*" Reserva : alberga
    Reserva "1" *-- "1" FranjaHoraria : compuesta por

    %% --- NOTAS FORMALES (INVARIANTES vs PRUEBAS DE EVIDENCIA) ---
    note for EscenarioDeportivo "REGLA DE DISPONIBILIDAD (HU3, HU8):\nSolo los escenarios con EstadoEscenario = ACTIVO\npueden ser consultados y recibir reservas."

    note for FranjaHoraria "VALUE OBJECT TEMPORAL:\nReifica el intervalo [horaInicio, horaFin]\nen una fecha dada para validar el solapamiento."

    note for Reserva "INVARIANTE DE DOMINIO (Regla de Negocio):\nPara un mismo EscenarioDeportivo y fecha, NO PUEDEN\ncoexistir dos instancias de Reserva en estado CONFIRMADA\ncuyas FranjaHoraria se solapen temporalmente.\n\nEVIDENCIA Y CONCURRENCIA (HU4 - Criterio 3):\nAnte 20 solicitudes simultáneas para la misma franja:\n- Exactamente 1 pasa a estado CONFIRMADA.\n- 19 son desestimadas (las rechazadas por concurrencia síncrona NO persisten en BD)."