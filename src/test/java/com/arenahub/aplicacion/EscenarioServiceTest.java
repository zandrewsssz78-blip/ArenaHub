package com.arenahub.aplicacion;

import com.arenahub.dominio.EscenarioDeportivo;
import com.arenahub.dominio.EstadoEscenario;
import com.arenahub.dominio.TipoEscenario;
import com.arenahub.persistencia.EscenarioDeportivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscenarioServiceTest {

    @Mock
    private EscenarioDeportivoRepository escenarios;

    @InjectMocks
    private EscenarioService service;

    private EscenarioDeportivo escenario;

    @BeforeEach
    void setUp() {
        escenario = new EscenarioDeportivo(1L, "Cancha Sintética Central", TipoEscenario.FUTBOL_5,
                "Sede Principal", 10, "Cancha techada con iluminación", EstadoEscenario.ACTIVO);
    }

    @Test
    void hu7_registrarEscenarioExitosoConEstadoActivo() {
        when(escenarios.existsByNombreIgnoreCase("Cancha Central")).thenReturn(false);
        when(escenarios.saveAndFlush(any(EscenarioDeportivo.class))).thenAnswer(inv -> inv.getArgument(0));

        EscenarioDeportivo creado = service.registrar("Cancha Central", TipoEscenario.FUTBOL_5, "Sede Norte", 10, "Cancha nueva");

        assertThat(creado).isNotNull();
        assertThat(creado.getNombre()).isEqualTo("Cancha Central");
        assertThat(creado.getEstado()).isEqualTo(EstadoEscenario.ACTIVO);
        assertThat(creado.admiteReservas()).isTrue();
        verify(escenarios).saveAndFlush(any(EscenarioDeportivo.class));
    }

    @Test
    void hu7_registrarEscenarioConNombreDuplicadoLanzaConflicto() {
        when(escenarios.existsByNombreIgnoreCase("Cancha Central")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar("Cancha Central", TipoEscenario.FUTBOL_5, "Sede Norte", 10, "Cancha"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Ya existe un escenario con el nombre");

        verify(escenarios, never()).saveAndFlush(any());
    }

    @Test
    void hu8_desactivarEscenarioPasaAEstadoInactivo() {
        when(escenarios.findById(1L)).thenReturn(Optional.of(escenario));
        when(escenarios.save(any(EscenarioDeportivo.class))).thenAnswer(inv -> inv.getArgument(0));

        EscenarioDeportivo desactivado = service.desactivar(1L);

        assertThat(desactivado.getEstado()).isEqualTo(EstadoEscenario.INACTIVO);
        assertThat(desactivado.admiteReservas()).isFalse();
        verify(escenarios).save(escenario);
    }

    @Test
    void hu8_cambiarEstadoAEnMantenimiento() {
        when(escenarios.findById(1L)).thenReturn(Optional.of(escenario));
        when(escenarios.save(any(EscenarioDeportivo.class))).thenAnswer(inv -> inv.getArgument(0));

        EscenarioDeportivo actualizado = service.cambiarEstado(1L, EstadoEscenario.EN_MANTENIMIENTO);

        assertThat(actualizado.getEstado()).isEqualTo(EstadoEscenario.EN_MANTENIMIENTO);
        assertThat(actualizado.admiteReservas()).isFalse();
        verify(escenarios).save(escenario);
    }

    @Test
    void hu8_desactivarEscenarioInexistenteLanzaExcepcion() {
        when(escenarios.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no existe");

        verify(escenarios, never()).save(any());
    }
}
