CREATE DATABASE reservas_deportivas;
USE reservas_deportivas;

CREATE TABLE usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE escenarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    ubicacion VARCHAR(150),
    estado BOOLEAN DEFAULT TRUE
);

CREATE TABLE reservas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    escenario_id INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'confirmada',
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    FOREIGN KEY (escenario_id) REFERENCES escenarios(id),
    UNIQUE KEY no_duplicado (escenario_id, fecha, hora_inicio)
);
