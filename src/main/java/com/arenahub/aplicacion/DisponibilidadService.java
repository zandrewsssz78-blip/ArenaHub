package com.arenahub.aplicacion;

import com.arenahub.dominio.EscenarioDeportivo;
import com.arenahub.dominio.EstadoEscenario;
import com.arenahub.dominio.FranjaHoraria;
import com.arenahub.persistencia.EscenarioDeportivoRepository;
import com.arenahub.persistencia.FranjaHorariaRepository;
import com.arenahub.persistencia.FranjaOcupada;
import com.arenahub.persistencia.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** HU3 — Consultar escenarios disponibles por fecha. */
@Service
public class DisponibilidadService {

    private final EscenarioDeportivoRepository escenarios;
    private final FranjaHorariaRepository franjas;
    private final ReservaRepository reservas;

    public DisponibilidadService(EscenarioDeportivoRepository escenarios,
                                 FranjaHorariaRepository franjas,
                                 ReservaRepository reservas) {
        this.escenarios = escenarios;
        this.franjas = franjas;
        this.reservas = reservas;
    }

    public record FranjaConEstado(FranjaHoraria franja, boolean ocupada) {
    }

    public record EscenarioConDisponibilidad(EscenarioDeportivo escenario, List<FranjaConEstado> franjas) {
    }

    @Transactional(readOnly = true)
    public List<EscenarioConDisponibilidad> consultar(LocalDate fecha) {
        List<FranjaHoraria> todasLasFranjas = franjas.findAllByOrderByHoraInicioAsc();
        Set<FranjaOcupada> ocupadas = Set.copyOf(reservas.buscarFranjasOcupadas(fecha));

        return escenarios.findByEstadoOrderByNombreAsc(EstadoEscenario.ACTIVO).stream()
                .map(escenario -> new EscenarioConDisponibilidad(
                        escenario,
                        todasLasFranjas.stream()
                                .map(f -> new FranjaConEstado(f,
                                        ocupadas.contains(new FranjaOcupada(escenario.getId(), f.getId()))))
                                .collect(Collectors.toList())))
                .toList();
    }
}
