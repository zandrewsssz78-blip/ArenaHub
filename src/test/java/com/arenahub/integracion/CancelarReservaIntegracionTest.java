package com.arenahub.integracion;

import com.arenahub.dominio.EstadoReserva;
import com.arenahub.web.ReservaController.CancelarReservaRequest;
import com.arenahub.web.ReservaController.CrearReservaRequest;
import com.arenahub.web.ReservaController.ReservaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU5 — Cancelar una reserva (integración HTTP + PostgreSQL).
 *
 * Criterios de aceptación (docs/historias-usuario.md):
 * - Dada una reserva propia, cuando la cancelo, entonces cambia a estado
 *   "cancelada" y el horario queda libre.
 * - Dada una reserva de otro usuario, cuando intento cancelarla, entonces el
 *   sistema rechaza la acción.
 */
class CancelarReservaIntegracionTest extends PruebaIntegracionBase {

    @Test
    void hu5_cancelarReservaPropiaLaDejaCanceladaEnLaBaseDeDatos() {
        Long usuario = crearUsuario();
        ReservaResponse reserva = reservar(usuario, crearEscenario("ACTIVO"), enDias(10), franjaDeLas(8));

        ResponseEntity<ReservaResponse> respuesta = cancelar(reserva.id(), usuario, ReservaResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().estado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(estadoEnBaseDeDatos(reserva.id())).isEqualTo("CANCELADA");
    }

    /** Criterio "el horario queda libre": otra persona puede reservar la misma franja. */
    @Test
    void hu5_alCancelarElHorarioQuedaLibreParaOtroUsuario() {
        Long escenario = crearEscenario("ACTIVO");
        LocalDate fecha = enDias(11);
        Long franja = franjaDeLas(19);
        Long duenio = crearUsuario();
        ReservaResponse original = reservar(duenio, escenario, fecha, franja);

        cancelar(original.id(), duenio, ReservaResponse.class);
        ResponseEntity<ReservaResponse> nueva = http.postForEntity("/api/reservas",
                new CrearReservaRequest(crearUsuario(), escenario, fecha, franja), ReservaResponse.class);

        assertThat(nueva.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(nueva.getBody().estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        Integer confirmadas = jdbc.queryForObject(
                "SELECT count(*) FROM reserva WHERE escenario_id = ? AND fecha = ? AND franja_id = ? "
                        + "AND estado = 'CONFIRMADA'", Integer.class, escenario, fecha, franja);
        assertThat(confirmadas).isEqualTo(1);
    }

    @Test
    void hu5_cancelarReservaDeOtroUsuarioSeRechazaYNoCambiaNada() {
        Long duenio = crearUsuario();
        Long otro = crearUsuario();
        ReservaResponse reserva = reservar(duenio, crearEscenario("ACTIVO"), enDias(12), franjaDeLas(9));

        ResponseEntity<String> respuesta = cancelar(reserva.id(), otro, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody()).contains("otro usuario");
        assertThat(estadoEnBaseDeDatos(reserva.id())).isEqualTo("CONFIRMADA");
    }

    @Test
    void hu5_cancelarReservaInexistenteResponde404() {
        ResponseEntity<String> respuesta = cancelar(999_999L, crearUsuario(), String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ReservaResponse reservar(Long usuario, Long escenario, LocalDate fecha, Long franja) {
        ResponseEntity<ReservaResponse> respuesta = http.postForEntity("/api/reservas",
                new CrearReservaRequest(usuario, escenario, fecha, franja), ReservaResponse.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return respuesta.getBody();
    }

    private <T> ResponseEntity<T> cancelar(Long reservaId, Long usuarioId, Class<T> tipo) {
        return http.exchange("/api/reservas/" + reservaId + "/cancelar", HttpMethod.PATCH,
                new HttpEntity<>(new CancelarReservaRequest(usuarioId)), tipo);
    }

    private String estadoEnBaseDeDatos(Long reservaId) {
        return jdbc.queryForObject("SELECT estado FROM reserva WHERE id = ?", String.class, reservaId);
    }
}
