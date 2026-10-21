package com.arenahub.web;

import com.arenahub.aplicacion.ReservaService;
import com.arenahub.dominio.EstadoReserva;
import com.arenahub.dominio.Reserva;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controladores de reservas:
 * - HU4: Crear una reserva (problema duro de concurrencia)
 * - HU5: Cancelar una reserva propia
 * - HU6: Ver historial de reservas de un usuario
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservas;

    public ReservaController(ReservaService reservas) {
        this.reservas = reservas;
    }

    /**
     * usuarioId va en el cuerpo porque el inicio de sesión (HU2) aún no está
     * implementado; cuando exista, saldrá del token/sesión.
     */
    public record CrearReservaRequest(
            @NotNull Long usuarioId,
            @NotNull Long escenarioId,
            @NotNull LocalDate fecha,
            @NotNull Long franjaId) {
    }

    public record CancelarReservaRequest(@NotNull Long usuarioId) {
    }

    public record ReservaResponse(
            Long id,
            String codigoReserva,
            Long usuarioId,
            Long escenarioId,
            String escenarioNombre,
            Long franjaId,
            LocalTime horaInicio,
            LocalTime horaFin,
            LocalDate fecha,
            EstadoReserva estado) {

        static ReservaResponse de(Reserva r) {
            return new ReservaResponse(
                    r.getId(),
                    r.getCodigoReserva(),
                    r.getUsuario() != null ? r.getUsuario().getId() : null,
                    r.getEscenario() != null ? r.getEscenario().getId() : null,
                    r.getEscenario() != null ? r.getEscenario().getNombre() : null,
                    r.getFranja() != null ? r.getFranja().getId() : null,
                    r.getFranja() != null ? r.getFranja().getHoraInicio() : null,
                    r.getFranja() != null ? r.getFranja().getHoraFin() : null,
                    r.getFecha(),
                    r.getEstado());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody CrearReservaRequest request) {
        Reserva reserva = reservas.crear(request.usuarioId(), request.escenarioId(),
                request.fecha(), request.franjaId());
        return ReservaResponse.de(reserva);
    }

    /** HU5 — Cancelar una reserva propia. */
    @PatchMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@PathVariable Long id, @Valid @RequestBody CancelarReservaRequest request) {
        return ReservaResponse.de(reservas.cancelar(id, request.usuarioId()));
    }

    /** HU5 — Cancelar una reserva vía DELETE con usuarioId en query param. */
    @DeleteMapping("/{id}")
    public ReservaResponse cancelarPorDelete(@PathVariable Long id, @RequestParam Long usuarioId) {
        return ReservaResponse.de(reservas.cancelar(id, usuarioId));
    }

    /** HU6 — Ver historial de reservas de un usuario. */
    @GetMapping
    public List<ReservaResponse> listarPorUsuario(@RequestParam Long usuarioId) {
        return reservas.listarPorUsuario(usuarioId).stream()
                .map(ReservaResponse::de)
                .toList();
    }

    @GetMapping("/{id}")
    public ReservaResponse obtenerPorId(@PathVariable Long id) {
        return ReservaResponse.de(reservas.obtenerPorId(id));
    }
}
