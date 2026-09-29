package com.arenahub.web;

import com.arenahub.aplicacion.DisponibilidadService;
import com.arenahub.dominio.TipoEscenario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** HU3 — Consultar escenarios disponibles por fecha. */
@RestController
@RequestMapping("/api/escenarios")
public class EscenarioController {

    private final DisponibilidadService disponibilidad;

    public EscenarioController(DisponibilidadService disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    public record FranjaResponse(Long franjaId, LocalTime horaInicio, LocalTime horaFin, boolean ocupada) {
    }

    public record EscenarioDisponibilidadResponse(Long id, String nombre, TipoEscenario tipo, String ubicacion,
                                                  Integer capacidad, List<FranjaResponse> franjas) {
    }

    /** Ejemplo: GET /api/escenarios/disponibilidad?fecha=2026-10-15 */
    @GetMapping("/disponibilidad")
    public List<EscenarioDisponibilidadResponse> disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return disponibilidad.consultar(fecha).stream()
                .map(d -> new EscenarioDisponibilidadResponse(
                        d.escenario().getId(), d.escenario().getNombre(), d.escenario().getTipo(),
                        d.escenario().getUbicacion(), d.escenario().getCapacidad(),
                        d.franjas().stream()
                                .map(f -> new FranjaResponse(f.franja().getId(), f.franja().getHoraInicio(),
                                        f.franja().getHoraFin(), f.ocupada()))
                                .toList()))
                .toList();
    }
}
