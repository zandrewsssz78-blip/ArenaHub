-- ArenaHub — Esquema inicial (PostgreSQL 16)
-- Traducción a PostgreSQL de docs/esquema-bd.md (3NF)

CREATE TABLE usuario (
    id              BIGSERIAL    PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol             VARCHAR(20)  NOT NULL DEFAULT 'JUGADOR',
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('JUGADOR', 'ENTRENADOR', 'ADMINISTRADOR'))
);

CREATE TABLE escenario_deportivo (
    id          BIGSERIAL    PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    tipo        VARCHAR(30)  NOT NULL,
    ubicacion   VARCHAR(200) NOT NULL,
    capacidad   INTEGER,
    descripcion VARCHAR(500),
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    CONSTRAINT uq_escenario_nombre UNIQUE (nombre),
    CONSTRAINT chk_escenario_tipo CHECK (tipo IN ('FUTBOL', 'FUTBOL_5', 'BALONCESTO', 'VOLEIBOL', 'TENIS', 'OTRO')),
    CONSTRAINT chk_escenario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'EN_MANTENIMIENTO')),
    CONSTRAINT chk_escenario_capacidad CHECK (capacidad IS NULL OR capacidad > 0)
);

-- Bloques horarios fijos y reutilizables (p. ej. 18:00-19:00)
CREATE TABLE franja_horaria (
    id          BIGSERIAL PRIMARY KEY,
    hora_inicio TIME      NOT NULL,
    hora_fin    TIME      NOT NULL,
    CONSTRAINT uq_franja_horaria UNIQUE (hora_inicio, hora_fin),
    CONSTRAINT chk_franja_orden CHECK (hora_inicio < hora_fin)
);

CREATE TABLE reserva (
    id             BIGSERIAL    PRIMARY KEY,
    codigo_reserva VARCHAR(50)  NOT NULL,
    usuario_id     BIGINT       NOT NULL,
    escenario_id   BIGINT       NOT NULL,
    franja_id      BIGINT       NOT NULL,
    fecha          DATE         NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMADA',
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT now(),
    observacion    VARCHAR(500),
    CONSTRAINT uq_reserva_codigo UNIQUE (codigo_reserva),
    CONSTRAINT fk_reserva_usuario   FOREIGN KEY (usuario_id)   REFERENCES usuario (id)             ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_escenario FOREIGN KEY (escenario_id) REFERENCES escenario_deportivo (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_franja    FOREIGN KEY (franja_id)    REFERENCES franja_horaria (id)      ON DELETE RESTRICT,
    CONSTRAINT chk_reserva_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'RECHAZADA'))
);

-- ============================================================
-- INVARIANTE DEL PROBLEMA DURO (docs/problema-duro.md)
-- Para un escenario, una fecha y una franja solo puede existir
-- UNA reserva CONFIRMADA. Índice único parcial: la base de datos
-- lo garantiza de forma atómica aunque lleguen 20 solicitudes a la vez.
-- Las reservas CANCELADAS no cuentan, así el horario queda libre (HU5).
-- ============================================================
CREATE UNIQUE INDEX uq_reserva_confirmada
    ON reserva (escenario_id, fecha, franja_id)
    WHERE estado = 'CONFIRMADA';

CREATE INDEX idx_reserva_fecha ON reserva (fecha);
CREATE INDEX idx_reserva_usuario ON reserva (usuario_id);
