package com.arenahub.web;

import com.arenahub.aplicacion.DisponibilidadService;
import com.arenahub.aplicacion.DisponibilidadService.EscenarioConDisponibilidad;
import com.arenahub.aplicacion.DisponibilidadService.FranjaConEstado;
import com.arenahub.aplicacion.EscenarioService;
import com.arenahub.dominio.EscenarioDeportivo;
import com.arenahub.dominio.EstadoEscenario;
import com.arenahub.dominio.TipoEscenario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * HU3 — Consultar escenarios disponibles por fecha.
 * HU7 — Registrar nuevo escenario deportivo (administración).
 * HU8 — Desactivar / actualizar estado de escenario deportivo (administración).
 */
@RestController
@RequestMapping("/api/escenarios")
public class EscenarioController {

    private final DisponibilidadService disponibilidad;
    private final EscenarioService escenarioService;

    public EscenarioController(DisponibilidadService disponibilidad, EscenarioService escenarioService) {
        this.disponibilidad = disponibilidad;
        this.escenarioService = escenarioService;
    }

    public record FranjaResponse(Long franjaId, LocalTime horaInicio, LocalTime horaFin, boolean ocupada) {
        static FranjaResponse de(FranjaConEstado f) {
            return new FranjaResponse(f.franja().getId(), f.franja().getHoraInicio(),
                    f.franja().getHoraFin(), f.ocupada());
        }
    }

    public record EscenarioDisponibilidadResponse(Long id, String nombre, TipoEscenario tipo, String ubicacion,
                                                  Integer capacidad, List<FranjaResponse> franjas) {
        static EscenarioDisponibilidadResponse de(EscenarioConDisponibilidad d) {
            EscenarioDeportivo e = d.escenario();
            return new EscenarioDisponibilidadResponse(e.getId(), e.getNombre(), e.getTipo(), e.getUbicacion(),
                    e.getCapacidad(), d.franjas().stream().map(FranjaResponse::de).toList());
        }
    }

    public record CrearEscenarioRequest(
            @NotBlank @Size(max = 100) String nombre,
            @NotNull TipoEscenario tipo,
            @NotBlank @Size(max = 200) String ubicacion,
            @Positive Integer capacidad,
            @Size(max = 500) String descripcion) {
    }

    public record CambiarEstadoRequest(
            @NotNull EstadoEscenario estado) {
    }

    public record EscenarioDetalleResponse(
            Long id,
            String nombre,
            TipoEscenario tipo,
            String ubicacion,
            Integer capacidad,
            String descripcion,
            EstadoEscenario estado) {
        public static EscenarioDetalleResponse de(EscenarioDeportivo e) {
            return new EscenarioDetalleResponse(
                    e.getId(), e.getNombre(), e.getTipo(), e.getUbicacion(),
                    e.getCapacidad(), e.getDescripcion(), e.getEstado());
        }
    }

    /** HU3: Ejemplo: GET /api/escenarios/disponibilidad?fecha=2026-10-15 */
    @GetMapping("/disponibilidad")
    public List<EscenarioDisponibilidadResponse> disponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return disponibilidad.consultar(fecha).stream()
                .map(EscenarioDisponibilidadResponse::de)
                .toList();
    }

    /** HU7: Registrar un nuevo escenario deportivo */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EscenarioDetalleResponse registrar(@Valid @RequestBody CrearEscenarioRequest request) {
        EscenarioDeportivo creado = escenarioService.registrar(
                request.nombre(), request.tipo(), request.ubicacion(),
                request.capacidad(), request.descripcion()
        );
        return EscenarioDetalleResponse.de(creado);
    }

    /** HU8: Desactivar un escenario deportivo */
    @PatchMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.OK)
    public EscenarioDetalleResponse desactivar(@PathVariable Long id) {
        return EscenarioDetalleResponse.de(escenarioService.desactivar(id));
    }

    /** HU8: Cambiar estado (INACTIVO, EN_MANTENIMIENTO, ACTIVO) */
    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.OK)
    public EscenarioDetalleResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return EscenarioDetalleResponse.de(escenarioService.cambiarEstado(id, request.estado()));
    }

    @GetMapping("/{id}")
    public EscenarioDetalleResponse obtenerPorId(@PathVariable Long id) {
        return EscenarioDetalleResponse.de(escenarioService.obtenerPorId(id));
    }

    @GetMapping
    public List<EscenarioDetalleResponse> listarTodos() {
        return escenarioService.listarTodos().stream()
                .map(EscenarioDetalleResponse::de)
                .toList();
    }
}
