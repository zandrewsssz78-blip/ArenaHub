package com.arenahub.web;

import com.arenahub.aplicacion.ReservaService;
import com.arenahub.dominio.EstadoReserva;
import com.arenahub.dominio.Reserva;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** HU4 — Crear una reserva. */
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

    public record ReservaResponse(Long id, String codigoReserva, Long usuarioId, Long escenarioId,
                                  Long franjaId, LocalDate fecha, EstadoReserva estado) {
        static ReservaResponse de(Reserva r) {
            return new ReservaResponse(r.getId(), r.getCodigoReserva(), r.getUsuario().getId(),
                    r.getEscenario().getId(), r.getFranja().getId(), r.getFecha(), r.getEstado());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse crear(@Valid @RequestBody CrearReservaRequest request) {
        Reserva reserva = reservas.crear(request.usuarioId(), request.escenarioId(),
                request.fecha(), request.franjaId());
        return ReservaResponse.de(reserva);
    }
}
