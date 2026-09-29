package com.arenahub.integracion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

/**
 * Base de las pruebas de integración: levanta la aplicación completa en un
 * puerto aleatorio y habla con ella por HTTP real, contra PostgreSQL real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(LimpiarBaseDeDatosConfig.class)
abstract class PruebaIntegracionBase {

    @Autowired
    protected TestRestTemplate http;

    @Autowired
    protected JdbcTemplate jdbc;

    protected static String unico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected Long crearUsuario() {
        return jdbc.queryForObject(
                "INSERT INTO usuario (nombre, email, contrasena_hash) VALUES (?, ?, 'hash') RETURNING id",
                Long.class, "Jugador", unico("jugador") + "@example.com");
    }

    protected Long crearEscenario(String estado) {
        return jdbc.queryForObject(
                "INSERT INTO escenario_deportivo (nombre, tipo, ubicacion, estado) "
                        + "VALUES (?, 'FUTBOL_5', 'Sede de pruebas', ?) RETURNING id",
                Long.class, unico("Cancha"), estado);
    }

    protected Long franjaDeLas(int hora) {
        return jdbc.queryForObject(
                "SELECT id FROM franja_horaria WHERE hora_inicio = make_time(?, 0, 0)", Long.class, hora);
    }
}
