package com.arenahub.aplicacion;

import com.arenahub.dominio.*;
import com.arenahub.persistencia.EscenarioDeportivoRepository;
import com.arenahub.persistencia.FranjaHorariaRepository;
import com.arenahub.persistencia.ReservaRepository;
import com.arenahub.persistencia.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservas;

    @Mock
    private UsuarioRepository usuarios;

    @Mock
    private EscenarioDeportivoRepository escenarios;

    @Mock
    private FranjaHorariaRepository franjas;

    @InjectMocks
    private ReservaService service;

    private Usuario usuario;
    private EscenarioDeportivo escenario;
    private FranjaHoraria franja;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        usuario = new Usuario(1L, "Carlos", "carlos@example.com", "hash");
        escenario = new EscenarioDeportivo("Cancha 1", TipoEscenario.FUTBOL_5, "Sede Norte", EstadoEscenario.ACTIVO);
        franja = new FranjaHoraria(LocalTime.of(10, 0), LocalTime.of(11, 0));
        reserva = Reserva.confirmar(usuario, escenario, franja, LocalDate.now().plusDays(5));
    }

    @Test
    void hu5_cancelarReservaPropiaExitosamente() {
        when(reservas.findByIdConDetalle(10L)).thenReturn(Optional.of(reserva));
        when(reservas.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva cancelada = service.cancelar(10L, 1L);

        assertThat(cancelada.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        verify(reservas).save(reserva);
    }

    @Test
    void hu5_cancelarReservaInexistenteLanzaExcepcion() {
        when(reservas.findByIdConDetalle(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(99L, 1L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");

        verify(reservas, never()).save(any());
    }

    @Test
    void hu5_cancelarReservaDeOtroUsuarioLanzaExcepcion() {
        when(reservas.findByIdConDetalle(10L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> service.cancelar(10L, 2L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No tienes permiso");

        verify(reservas, never()).save(any());
    }

    @Test
    void hu6_listarReservasPorUsuarioExitosamente() {
        when(usuarios.existsById(1L)).thenReturn(true);
        when(reservas.buscarPorUsuarioConDetalle(1L)).thenReturn(List.of(reserva));

        List<Reserva> lista = service.listarPorUsuario(1L);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getUsuario().getId()).isEqualTo(1L);
        assertThat(lista.get(0).getCodigoReserva()).startsWith("RES-");
    }

    @Test
    void hu6_listarReservasUsuarioInexistenteLanzaExcepcion() {
        when(usuarios.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listarPorUsuario(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");

        verify(reservas, never()).buscarPorUsuarioConDetalle(any());
    }
}
