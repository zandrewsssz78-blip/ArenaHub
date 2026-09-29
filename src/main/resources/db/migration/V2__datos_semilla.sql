-- Datos semilla para probar el esqueleto a mano

-- Franjas de una hora, de 06:00 a 22:00
INSERT INTO franja_horaria (hora_inicio, hora_fin)
SELECT make_time(h, 0, 0), make_time(h + 1, 0, 0)
FROM generate_series(6, 21) AS h;

INSERT INTO escenario_deportivo (nombre, tipo, ubicacion, capacidad, descripcion, estado) VALUES
('Cancha Sintética 1', 'FUTBOL_5',   'Sede Norte - Bloque A', 10, 'Grama sintética con iluminación', 'ACTIVO'),
('Coliseo Principal',  'BALONCESTO', 'Sede Central',          30, 'Coliseo cubierto',                'ACTIVO'),
('Cancha Grama 2',     'FUTBOL',     'Sede Campestre',        22, 'Grama natural',                   'EN_MANTENIMIENTO');
