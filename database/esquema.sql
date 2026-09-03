
-- ==========================================
-- 2. CREATE TABLES (ESQUEMA COMPLETO)
-- ==========================================

-- Tabla: USUARIO (HU1, HU2)
CREATE TABLE USUARIO (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    contrasena_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL DEFAULT 'JUGADOR',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    
    CONSTRAINT chk_usuario_rol CHECK (rol IN ('JUGADOR', 'ENTRENADOR', 'ADMINISTRADOR'))
);

-- Tabla: ESCENARIO_DEPORTIVO (HU3, HU7, HU8)
CREATE TABLE ESCENARIO_DEPORTIVO (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    tipo VARCHAR(30) NOT NULL,
    ubicacion VARCHAR(200) NOT NULL,
    capacidad INT NULL,
    descripcion TEXT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    
    CONSTRAINT chk_escenario_tipo CHECK (tipo IN ('FUTBOL', 'FUTBOL_5', 'BALONCESTO', 'VOLEIBOL', 'TENIS', 'OTRO')),
    CONSTRAINT chk_escenario_estado CHECK (estado IN ('ACTIVO', 'INACTIVO', 'EN_MANTENIMIENTO')),
    CONSTRAINT chk_escenario_capacidad CHECK (capacidad IS NULL OR capacidad > 0)
);

-- Tabla: FRANJA_HORARIA (Valor de dominio temporal)
CREATE TABLE FRANJA_HORARIA (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    
    CONSTRAINT uq_franja_horaria UNIQUE (fecha, hora_inicio, hora_fin),
    CONSTRAINT chk_franja_orden_tiempo CHECK (hora_inicio < hora_fin)
);

-- Tabla: RESERVA (HU4, HU5, HU6)
CREATE TABLE RESERVA (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_reserva VARCHAR(50) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    escenario_id BIGINT NOT NULL,
    franja_horaria_id BIGINT NOT NULL UNIQUE, -- Composición 1 a 1 con la franja
    estado VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA',
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion TEXT NULL,
    
    -- Foreign Keys
    CONSTRAINT fk_reserva_usuario FOREIGN KEY (usuario_id) 
        REFERENCES USUARIO(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_escenario FOREIGN KEY (escenario_id) 
        REFERENCES ESCENARIO_DEPORTIVO(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reserva_franja FOREIGN KEY (franja_horaria_id) 
        REFERENCES FRANJA_HORARIA(id) ON DELETE RESTRICT,
        
    -- Restricción de Estado
    CONSTRAINT chk_reserva_estado CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'RECHAZADA'))
);

-- ==========================================
-- 3. TRIGGER: INVARIANTE DEL PROBLEMA DURO
-- ==========================================
-- Previene en MySQL la inserción de reservas CONFIRMADAS que se solapen
-- en horario para el mismo escenario y fecha.

DELIMITER //
CREATE TRIGGER trg_prevenir_solapamiento_reserva
BEFORE INSERT ON RESERVA
FOR EACH ROW
BEGIN
    DECLARE v_solapados INT;
    DECLARE v_fecha DATE;
    DECLARE v_inicio TIME;
    DECLARE v_fin TIME;

    -- Solo evaluamos la regla si la nueva reserva intenta ser CONFIRMADA
    IF NEW.estado = 'CONFIRMADA' THEN
        -- Obtenemos los datos temporales de la franja que se intenta reservar
        SELECT fecha, hora_inicio, hora_fin 
        INTO v_fecha, v_inicio, v_fin
        FROM FRANJA_HORARIA 
        WHERE id = NEW.franja_horaria_id;

        -- Contamos si ya existen reservas CONFIRMADAS para el mismo escenario que se crucen en el tiempo
        SELECT COUNT(*) INTO v_solapados
        FROM RESERVA r
        JOIN FRANJA_HORARIA fh ON r.franja_horaria_id = fh.id
        WHERE r.escenario_id = NEW.escenario_id
          AND r.estado = 'CONFIRMADA'
          AND fh.fecha = v_fecha
          AND (v_inicio < fh.hora_fin AND v_fin > fh.hora_inicio);

        -- Si hay al menos un solapamiento, abortamos la transacción con error
        IF v_solapados > 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Error de concurrencia: Ya existe una reserva confirmada en este escenario con horario solapado.';
        END IF;
    END IF;
END;
//
DELIMITER ;

-- ==========================================
-- 4. INSERT (DATOS DE PRUEBA / SEMILLA)
-- ==========================================

-- Usuarios (HU1)
INSERT INTO USUARIO (nombre, email, contrasena_hash, rol, activo) VALUES 
('Andrés Rubiano', 'andres@example.com', '$2a$10$e8N...hash1', 'JUGADOR', TRUE),
('Carlos Entrenador', 'carlos@example.com', '$2a$10$e8N...hash2', 'ENTRENADOR', TRUE),
('Admin ArenaHub', 'admin@arenahub.com', '$2a$10$e8N...hash3', 'ADMINISTRADOR', TRUE);

-- Escenarios (HU7, HU8)
INSERT INTO ESCENARIO_DEPORTIVO (nombre, tipo, ubicacion, capacidad, estado) VALUES 
('Cancha Sintética 1', 'FUTBOL_5', 'Sede Norte - Bloque A', 10, 'ACTIVO'),
('Coliseo Principal', 'BALONCESTO', 'Sede Central', 30, 'ACTIVO'),
('Cancha Grama 2', 'FUTBOL', 'Sede Campestre', 22, 'EN_MANTENIMIENTO');

-- Franjas Horarias
INSERT INTO FRANJA_HORARIA (fecha, hora_inicio, hora_fin) VALUES 
('2026-09-01', '10:00:00', '11:00:00'), -- Franja 1
('2026-09-01', '11:00:00', '12:00:00'), -- Franja 2 (Contigua)
('2026-09-01', '10:30:00', '11:30:00'); -- Franja 3 (Se solapa con Franja 1)

-- Creación de la Primera Reserva exitosa
INSERT INTO RESERVA (codigo_reserva, usuario_id, escenario_id, franja_horaria_id, estado) VALUES 
('RES-0001', 1, 1, 1, 'CONFIRMADA');

-- ==========================================
-- 5. FETCH / CONSULTAS DE VERIFICACIÓN
-- ==========================================

-- Consulta HU3: Ver escenarios disponibles activos
SELECT id, nombre, tipo, ubicacion, estado 
FROM ESCENARIO_DEPORTIVO 
WHERE estado = 'ACTIVO';

-- Consulta HU6: Historial de reservas de un usuario
SELECT 
    r.codigo_reserva,
    e.nombre AS escenario,
    fh.fecha,
    fh.hora_inicio,
    fh.hora_fin,
    r.estado
FROM RESERVA r
JOIN ESCENARIO_DEPORTIVO e ON r.escenario_id = e.id
JOIN FRANJA_HORARIA fh ON r.franja_horaria_id = fh.id
WHERE r.usuario_id = 1;

-- Insertar franjas pegadas límite a límite
INSERT INTO FRANJA_HORARIA (fecha, hora_inicio, hora_fin) VALUES 
('2026-09-02', '14:00:00', '15:00:00'), -- Franja A
('2026-09-02', '15:00:00', '16:00:00'); -- Franja B (Inicia exactamente cuando termina la A)

-- Reserva 1: Exito
INSERT INTO RESERVA (codigo_reserva, usuario_id, escenario_id, franja_horaria_id, estado) 
VALUES ('RES-EDGE-1', 1, 1, (SELECT id FROM FRANJA_HORARIA WHERE fecha='2026-09-02' AND hora_inicio='14:00:00'), 'CONFIRMADA');

-- Reserva 2: Debe PERMITIRSE porque 15:00 no es menor que 15:00 (v_inicio < fh.hora_fin AND v_fin > fh.hora_inicio)
INSERT INTO RESERVA (codigo_reserva, usuario_id, escenario_id, franja_horaria_id, estado) 
VALUES ('RES-EDGE-2', 2, 1, (SELECT id FROM FRANJA_HORARIA WHERE fecha='2026-09-02' AND hora_inicio='15:00:00'), 'CONFIRMADA');

-- Verificación de éxito (deben aparecer ambas reservas)
SELECT r.codigo_reserva, fh.hora_inicio, fh.hora_fin, r.estado
FROM RESERVA r
JOIN FRANJA_HORARIA fh ON r.franja_horaria_id = fh.id
WHERE r.codigo_reserva LIKE 'RES-EDGE%';