package com.arenahub.integracion;

import com.arenahub.dominio.EstadoReserva;
import com.arenahub.web.ReservaController.CrearReservaRequest;
import com.arenahub.web.ReservaController.ReservaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/** HU4 — Crear una reserva, incluida la evidencia del problema duro. */
class CrearReservaIntegracionTest extends PruebaIntegracionBase {

    private static final int SOLICITUDES = 20;

    @Test
    void horarioLibreQuedaConfirmado() {
        Long escenario = crearEscenario("ACTIVO");
        LocalDate fecha = LocalDate.of(2026, 12, 1);

        ResponseEntity<ReservaResponse> respuesta = http.postForEntity("/api/reservas",
                new CrearReservaRequest(crearUsuario(), escenario, fecha, franjaDeLas(10)), ReservaResponse.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().estado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(respuesta.getBody().codigoReserva()).startsWith("RES-");
    }

    @Test
    void horarioYaReservadoSeRechazaConConflicto() {
        Long escenario = crearEscenario("ACTIVO");
        LocalDate fecha = LocalDate.of(2026, 12, 2);
        Long franja = franjaDeLas(11);
        http.postForEntity("/api/reservas",
                new CrearReservaRequest(crearUsuario(), escenario, fecha, franja), String.class);

        ResponseEntity<String> segunda = http.postForEntity("/api/reservas",
                new CrearReservaRequest(crearUsuario(), escenario, fecha, franja), String.class);

        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(segunda.getBody()).contains("ya está reservado");
    }

    @Test
    void escenarioEnMantenimientoSeRechaza() {
        Long escenario = crearEscenario("EN_MANTENIMIENTO");

        ResponseEntity<String> respuesta = http.postForEntity("/api/reservas",
                new CrearReservaRequest(crearUsuario(), escenario, LocalDate.of(2026, 12, 3), franjaDeLas(9)),
                String.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    /**
     * PROBLEMA DURO — Concurrencia (docs/problema-duro.md).
     * 20 hilos esperan en una barrera (CountDownLatch) y se liberan al mismo
     * tiempo para reservar el mismo escenario, fecha y franja por HTTP.
     * Esperado: 1 confirmada, 19 rechazadas, 1 sola fila persistida.
     */
    @Test
    void veinteSolicitudesSimultaneasSoloConfirmanUna() throws Exception {
        Long escenario = crearEscenario("ACTIVO");
        LocalDate fecha = LocalDate.of(2026, 12, 24);
        Long franja = franjaDeLas(18);
        List<Long> usuarios = new ArrayList<>();
        for (int i = 0; i < SOLICITUDES; i++) {
            usuarios.add(crearUsuario());
        }

        ExecutorService hilos = Executors.newFixedThreadPool(SOLICITUDES);
        CountDownLatch listos = new CountDownLatch(SOLICITUDES);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<HttpStatusCode>> resultados = new ArrayList<>();

        for (Long usuario : usuarios) {
            resultados.add(hilos.submit(() -> {
                listos.countDown();
                salida.await();           // barrera: todos arrancan a la vez
                return http.postForEntity("/api/reservas",
                        new CrearReservaRequest(usuario, escenario, fecha, franja), String.class)
                        .getStatusCode();
            }));
        }

        assertThat(listos.await(10, TimeUnit.SECONDS)).isTrue();
        salida.countDown();

        int confirmadas = 0;
        int rechazadas = 0;
        for (Future<HttpStatusCode> resultado : resultados) {
            HttpStatusCode status = resultado.get(30, TimeUnit.SECONDS);
            if (status.value() == HttpStatus.CREATED.value()) {
                confirmadas++;
            } else if (status.value() == HttpStatus.CONFLICT.value()) {
                rechazadas++;
            }
        }
        hilos.shutdown();

        assertThat(confirmadas).as("solicitudes confirmadas").isEqualTo(1);
        assertThat(rechazadas).as("solicitudes rechazadas").isEqualTo(SOLICITUDES - 1);

        Integer persistidas = jdbc.queryForObject(
                "SELECT count(*) FROM reserva WHERE escenario_id = ? AND fecha = ? AND franja_id = ?",
                Integer.class, escenario, fecha, franja);
        assertThat(persistidas).as("reservas persistidas (sin duplicados)").isEqualTo(1);
    }
}
