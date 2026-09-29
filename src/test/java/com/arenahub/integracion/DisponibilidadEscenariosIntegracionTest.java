package com.arenahub.integracion;

import com.arenahub.web.EscenarioController.EscenarioDisponibilidadResponse;
import com.arenahub.web.EscenarioController.FranjaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** HU3 — Consultar escenarios disponibles por fecha. */
class DisponibilidadEscenariosIntegracionTest extends PruebaIntegracionBase {

    @Test
    void soloMuestraEscenariosActivosConSusHorariosOcupadosMarcados() {
        LocalDate fecha = LocalDate.of(2026, 11, 20);
        Long activo = crearEscenario("ACTIVO");
        Long enMantenimiento = crearEscenario("EN_MANTENIMIENTO");
        Long franja18 = franjaDeLas(18);
        jdbc.update("INSERT INTO reserva (codigo_reserva, usuario_id, escenario_id, franja_id, fecha) "
                + "VALUES (?, ?, ?, ?, ?)", unico("RES"), crearUsuario(), activo, franja18, fecha);

        ResponseEntity<EscenarioDisponibilidadResponse[]> respuesta = http.getForEntity(
                "/api/escenarios/disponibilidad?fecha=" + fecha, EscenarioDisponibilidadResponse[].class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<EscenarioDisponibilidadResponse> escenarios = Arrays.asList(respuesta.getBody());
        assertThat(escenarios).extracting(EscenarioDisponibilidadResponse::id)
                .contains(activo)
                .doesNotContain(enMantenimiento);

        EscenarioDisponibilidadResponse cancha = escenarios.stream()
                .filter(e -> e.id().equals(activo)).findFirst().orElseThrow();
        assertThat(cancha.franjas()).filteredOn(FranjaResponse::ocupada)
                .extracting(FranjaResponse::franjaId)
                .containsExactly(franja18);
    }

    @Test
    void fechaInvalidaResponde400() {
        ResponseEntity<String> respuesta = http.getForEntity(
                "/api/escenarios/disponibilidad?fecha=no-es-fecha", String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
