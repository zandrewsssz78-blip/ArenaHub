-- =========================================
-- ArenaHub — Esquema relacional (PostgreSQL)
-- =========================================

CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('JUGADOR', 'ENTRENADOR', 'ADMINISTRADOR')),
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE escenarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('FUTBOL', 'FUTBOL_5', 'BALONCESTO', 'VOLEIBOL', 'TENIS', 'OTRO')),
    ubicacion VARCHAR(150),
    descripcion VARCHAR(255),
    capacidad INT,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO', 'EN_MANTENIMIENTO'))
);

CREATE TABLE franjas_horarias (
    id SERIAL PRIMARY KEY,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CHECK (hora_fin > hora_inicio)
);

CREATE TABLE reservas (
    id SERIAL PRIMARY KEY,
    usuario_id INT NOT NULL REFERENCES usuarios(id),
    escenario_id INT NOT NULL REFERENCES escenarios(id),
    franja_id INT NOT NULL REFERENCES franjas_horarias(id),
    fecha DATE NOT NULL,
    codigo_reserva VARCHAR(20) UNIQUE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'RECHAZADA')),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW(),
    observacion VARCHAR(255),
    UNIQUE (escenario_id, fecha, franja_id)
);

-- =========================================
-- Datos de prueba
-- =========================================

INSERT INTO usuarios (nombre, correo, password_hash, rol, activo) VALUES
('Sofia Ramirez', 'sofia@example.com', 'hash1', 'JUGADOR', TRUE),
('Carlos Perez', 'carlos@example.com', 'hash2', 'JUGADOR', TRUE),
('Laura Gomez', 'laura@example.com', 'hash3', 'ENTRENADOR', TRUE),
('Admin ArenaHub', 'admin@example.com', 'hash4', 'ADMINISTRADOR', TRUE);

INSERT INTO escenarios (nombre, tipo, ubicacion, descripcion, capacidad, estado) VALUES
('Cancha 1', 'FUTBOL_5', 'Parque Norte', 'Cancha sintetica techada', 10, 'ACTIVO'),
('Cancha 2', 'BALONCESTO', 'Parque Norte', 'Cancha al aire libre', 12, 'ACTIVO'),
('Cancha 3', 'TENIS', 'Coliseo Sur', 'Cancha de arcilla', 4, 'EN_MANTENIMIENTO');

INSERT INTO franjas_horarias (hora_inicio, hora_fin) VALUES
('06:00', '07:00'),
('07:00', '08:00'),
('08:00', '09:00'),
('18:00', '19:00');

INSERT INTO reservas (usuario_id, escenario_id, franja_id, fecha, codigo_reserva, estado, observacion) VALUES
(1, 1, 2, '2026-09-10', 'RES-0001', 'CONFIRMADA', 'Reserva regular de entrenamiento'),
(2, 2, 4, '2026-09-10', 'RES-0002', 'CONFIRMADA', NULL),
(3, 1, 1, '2026-09-11', 'RES-0003', 'PENDIENTE', 'Pendiente de pago');

-- =========================================
-- Consultas de ejemplo (responden a historias de usuario)
-- =========================================

-- HU: ver escenarios disponibles (estado ACTIVO)
SELECT id, nombre, tipo, ubicacion
FROM escenarios
WHERE estado = 'ACTIVO';

-- HU: ver reservas confirmadas de un usuario
SELECT r.codigo_reserva, e.nombre AS escenario, r.fecha, f.hora_inicio, f.hora_fin
FROM reservas r
JOIN escenarios e ON e.id = r.escenario_id
JOIN franjas_horarias f ON f.id = r.franja_id
WHERE r.usuario_id = 1 AND r.estado = 'CONFIRMADA';

-- HU: verificar disponibilidad de un escenario en una fecha y franja especifica
SELECT COUNT(*) AS reservas_existentes
FROM reservas
WHERE escenario_id = 1
  AND fecha = '2026-09-10'
  AND franja_id = 2
  AND estado = 'CONFIRMADA';