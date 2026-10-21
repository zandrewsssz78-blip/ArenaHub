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

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU6 — Ver historial de reservas propias (integración HTTP + PostgreSQL).
 *
 * Criterio de aceptación (docs/historias-usuario.md):
 * - Dado un usuario autenticado, cuando consulta su historial, entonces solo ve
 *   sus propias reservas, con escenario, fecha, horario y estado.
 */
class HistorialReservasIntegracionTest extends PruebaIntegracionBase {

    @Test
    void hu6_soloMuestraLasReservasDelUsuarioConEscenarioFechaHorarioYEstado() {
        Long ana = crearUsuario();
        Long otro = crearUsuario();
        Long escenario = crearEscenario("ACTIVO");
        reservar(ana, escenario, franjaDeLas(7), 20);
        reservar(ana, escenario, franjaDeLas(8), 21);
        reservar(otro, escenario, franjaDeLas(9), 22);

        ResponseEntity<ReservaResponse[]> respuesta = http.getForEntity(
                "/api/reservas?usuarioId=" + ana, ReservaResponse[].class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ReservaResponse> historial = Arrays.asList(respuesta.getBody());
        assertThat(historial).hasSize(2);
        assertThat(historial).allSatisfy(r -> {
            assertThat(r.usuarioId()).isEqualTo(ana);
            assertThat(r.escenarioNombre()).isNotBlank();
            assertThat(r.fecha()).isNotNull();
            assertThat(r.horaInicio()).isNotNull();
            assertThat(r.horaFin()).isNotNull();
            assertThat(r.estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        });
        assertThat(historial).extracting(ReservaResponse::horaInicio)
                .containsExactlyInAnyOrder(LocalTime.of(7, 0), LocalTime.of(8, 0));
    }

    @Test
    void hu6_elHistorialIncluyeLasReservasCanceladasConSuEstado() {
        Long usuario = crearUsuario();
        ReservaResponse reserva = reservar(usuario, crearEscenario("ACTIVO"), franjaDeLas(10), 23);
        http.exchange("/api/reservas/" + reserva.id() + "/cancelar", HttpMethod.PATCH,
                new HttpEntity<>(new CancelarReservaRequest(usuario)), ReservaResponse.class);

        ResponseEntity<ReservaResponse[]> respuesta = http.getForEntity(
                "/api/reservas?usuarioId=" + usuario, ReservaResponse[].class);

        assertThat(respuesta.getBody()).extracting(ReservaResponse::estado)
                .containsExactly(EstadoReserva.CANCELADA);
    }

    @Test
    void hu6_usuarioSinReservasRecibeListaVacia() {
        ResponseEntity<ReservaResponse[]> respuesta = http.getForEntity(
                "/api/reservas?usuarioId=" + crearUsuario(), ReservaResponse[].class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEmpty();
    }

    @Test
    void hu6_usuarioInexistenteResponde404() {
        ResponseEntity<String> respuesta = http.getForEntity("/api/reservas?usuarioId=999999", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ReservaResponse reservar(Long usuario, Long escenario, Long franja, int dentroDeDias) {
        ResponseEntity<ReservaResponse> respuesta = http.postForEntity("/api/reservas",
                new CrearReservaRequest(usuario, escenario, enDias(dentroDeDias), franja), ReservaResponse.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return respuesta.getBody();
    }
}
