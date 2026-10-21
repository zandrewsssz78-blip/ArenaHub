package com.arenahub.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba unitaria: reglas del dominio de Reserva, sin Spring ni base de datos. */
class ReservaTest {

    private final Usuario usuario = new Usuario("Ana", "ana@example.com", "hash");
    private final FranjaHoraria franja = new FranjaHoraria(LocalTime.of(18, 0), LocalTime.of(19, 0));
    private final LocalDate fecha = LocalDate.of(2026, 10, 15);

    @Test
    void reservaEnEscenarioActivoQuedaConfirmada() {
        EscenarioDeportivo cancha = new EscenarioDeportivo("Cancha 1", TipoEscenario.FUTBOL_5, "Sede Norte",
                EstadoEscenario.ACTIVO);

        Reserva reserva = Reserva.confirmar(usuario, cancha, franja, fecha);

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(reserva.getCodigoReserva()).startsWith("RES-");
        assertThat(reserva.getFecha()).isEqualTo(fecha);
        assertThat(reserva.getFechaCreacion()).isNotNull();
    }

    @Test
    void escenarioEnMantenimientoNoAdmiteReservas() {
        EscenarioDeportivo cancha = new EscenarioDeportivo("Cancha 2", TipoEscenario.FUTBOL, "Sede Campestre",
                EstadoEscenario.EN_MANTENIMIENTO);

        assertThatThrownBy(() -> Reserva.confirmar(usuario, cancha, franja, fecha))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no está disponible");
    }

    @Test
    void franjaConHoraInicioPosteriorAHoraFinEsInvalida() {
        assertThatThrownBy(() -> new FranjaHoraria(LocalTime.of(19, 0), LocalTime.of(18, 0)))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void usuarioPuedeCancelarSuPropiaReserva() {
        Usuario usuarioConId = new Usuario(1L, "Ana", "ana@example.com", "hash");
        EscenarioDeportivo cancha = new EscenarioDeportivo("Cancha 1", TipoEscenario.FUTBOL_5, "Sede Norte",
                EstadoEscenario.ACTIVO);
        Reserva reserva = Reserva.confirmar(usuarioConId, cancha, franja, fecha);

        reserva.cancelar(1L);

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void usuarioNoPuedeCancelarReservaDeOtroUsuario() {
        Usuario usuarioConId = new Usuario(1L, "Ana", "ana@example.com", "hash");
        EscenarioDeportivo cancha = new EscenarioDeportivo("Cancha 1", TipoEscenario.FUTBOL_5, "Sede Norte",
                EstadoEscenario.ACTIVO);
        Reserva reserva = Reserva.confirmar(usuarioConId, cancha, franja, fecha);

        assertThatThrownBy(() -> reserva.cancelar(999L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No tienes permiso");
    }

    @Test
    void reservaYaCanceladaNoSePuedeVolverACancelar() {
        Usuario usuarioConId = new Usuario(1L, "Ana", "ana@example.com", "hash");
        EscenarioDeportivo cancha = new EscenarioDeportivo("Cancha 1", TipoEscenario.FUTBOL_5, "Sede Norte",
                EstadoEscenario.ACTIVO);
        Reserva reserva = Reserva.confirmar(usuarioConId, cancha, franja, fecha);

        reserva.cancelar(1L);

        assertThatThrownBy(() -> reserva.cancelar(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya se encuentra cancelada");
    }
}
