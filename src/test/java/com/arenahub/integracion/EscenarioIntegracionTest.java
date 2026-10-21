package com.arenahub.integracion;

import com.arenahub.dominio.TipoEscenario;
import com.arenahub.web.EscenarioController.CrearEscenarioRequest;
import com.arenahub.web.EscenarioController.EscenarioDetalleResponse;
import com.arenahub.web.EscenarioController.EscenarioDisponibilidadResponse;
import com.arenahub.web.ReservaController.CrearReservaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU7 — Registrar un nuevo escenario deportivo (administración).
 * HU8 — Desactivar un escenario deportivo (administración).
 */
class EscenarioIntegracionTest extends PruebaIntegracionBase {

    @Test
    void hu7_registrarEscenarioExitosoConEstadoActivo() {
        String nombre = unico("Cancha Sintetica");
        CrearEscenarioRequest request = new CrearEscenarioRequest(
                nombre, TipoEscenario.FUTBOL_5, "Sede Norte", 10, "Cancha sintética techada"
        );

        ResponseEntity<EscenarioDetalleResponse> respuesta = http.postForEntity(
                "/api/escenarios", request, EscenarioDetalleResponse.class
        );

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().id()).isNotNull();
        assertThat(respuesta.getBody().nombre()).isEqualTo(nombre);
        assertThat(respuesta.getBody().estado().name()).isEqualTo("ACTIVO");
    }

    @Test
    void hu7_registrarEscenarioSinNombreRetorna400BadRequest() {
        CrearEscenarioRequest request = new CrearEscenarioRequest(
                "", TipoEscenario.BALONCESTO, "Sede Sur", 12, "Cancha múltiple"
        );

        ResponseEntity<String> respuesta = http.postForEntity("/api/escenarios", request, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void hu7_registrarEscenarioDuplicadoRetorna409Conflict() {
        String nombre = unico("Cancha Duplicada");
        CrearEscenarioRequest request = new CrearEscenarioRequest(
                nombre, TipoEscenario.TENIS, "Sede Oriente", 4, "Cancha de tenis"
        );

        http.postForEntity("/api/escenarios", request, EscenarioDetalleResponse.class);
        ResponseEntity<String> repetido = http.postForEntity("/api/escenarios", request, String.class);

        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void hu8_desactivarEscenarioPasaAInactivoYDesapareceDeDisponibilidad() {
        Long escenarioId = crearEscenario("ACTIVO");

        // 1. Desactivar escenario vía PATCH
        ResponseEntity<EscenarioDetalleResponse> respuestaDesactivar = http.exchange(
                "/api/escenarios/" + escenarioId + "/desactivar",
                HttpMethod.PATCH,
                HttpEntity.EMPTY,
                EscenarioDetalleResponse.class
        );

        assertThat(respuestaDesactivar.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuestaDesactivar.getBody()).isNotNull();
        assertThat(respuestaDesactivar.getBody().estado().name()).isEqualTo("INACTIVO");

        // 2. Verificar que no aparece en la disponibilidad
        LocalDate manana = LocalDate.now().plusDays(1);
        ResponseEntity<EscenarioDisponibilidadResponse[]> respuestaDisp = http.getForEntity(
                "/api/escenarios/disponibilidad?fecha=" + manana,
                EscenarioDisponibilidadResponse[].class
        );

        assertThat(respuestaDisp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Long> idsDisponibles = Arrays.stream(respuestaDisp.getBody())
                .map(EscenarioDisponibilidadResponse::id)
                .toList();

        assertThat(idsDisponibles).doesNotContain(escenarioId);
    }

    @Test
    void hu8_reservaDeEscenarioDesactivadoSeRechazaCon422() {
        Long usuarioId = crearUsuario();
        Long escenarioInactivoId = crearEscenario("INACTIVO");
        Long franjaId = franjaDeLas(18);
        LocalDate manana = LocalDate.now().plusDays(1);

        CrearReservaRequest request = new CrearReservaRequest(usuarioId, escenarioInactivoId, manana, franjaId);

        ResponseEntity<String> respuesta = http.postForEntity("/api/reservas", request, String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody()).contains("no está disponible para reservas");
    }
}
