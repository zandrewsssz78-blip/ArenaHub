package com.arenahub.aplicacion;

import com.arenahub.dominio.*;
import com.arenahub.persistencia.EscenarioDeportivoRepository;
import com.arenahub.persistencia.FranjaHorariaRepository;
import com.arenahub.persistencia.ReservaRepository;
import com.arenahub.persistencia.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * HU4 — Crear una reserva (problema duro: concurrencia).
 *
 * Mecanismo: la verificación previa (existsBy...) solo da un mensaje rápido en el
 * caso secuencial. La garantía real bajo concurrencia es el índice único parcial
 * uq_reserva_confirmada de PostgreSQL: si 20 transacciones insertan a la vez la
 * misma (escenario, fecha, franja) CONFIRMADA, la base de datos acepta una y
 * rechaza las demás con violación de unicidad, que aquí se traduce a 409.
 */
@Service
public class ReservaService {

    static final String INDICE_RESERVA_UNICA = "uq_reserva_confirmada";

    private final ReservaRepository reservas;
    private final UsuarioRepository usuarios;
    private final EscenarioDeportivoRepository escenarios;
    private final FranjaHorariaRepository franjas;

    public ReservaService(ReservaRepository reservas, UsuarioRepository usuarios,
                          EscenarioDeportivoRepository escenarios, FranjaHorariaRepository franjas) {
        this.reservas = reservas;
        this.usuarios = usuarios;
        this.escenarios = escenarios;
        this.franjas = franjas;
    }

    @Transactional
    public Reserva crear(Long usuarioId, Long escenarioId, LocalDate fecha, Long franjaId) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + usuarioId + " no existe"));
        EscenarioDeportivo escenario = escenarios.findById(escenarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Escenario " + escenarioId + " no existe"));
        FranjaHoraria franja = franjas.findById(franjaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Franja " + franjaId + " no existe"));

        Reserva reserva = Reserva.confirmar(usuario, escenario, franja, fecha);

        if (reservas.existsByEscenarioIdAndFechaAndFranjaIdAndEstado(
                escenarioId, fecha, franjaId, EstadoReserva.CONFIRMADA)) {
            throw horarioOcupado();
        }

        try {
            return reservas.saveAndFlush(reserva);
        } catch (DataIntegrityViolationException e) {
            if (esViolacionDeReservaUnica(e)) {
                throw horarioOcupado();
            }
            throw e;
        }
    }

    private static ConflictoException horarioOcupado() {
        return new ConflictoException("El escenario ya está reservado en esa fecha y franja horaria");
    }

    private static boolean esViolacionDeReservaUnica(DataIntegrityViolationException e) {
        Throwable causa = e;
        while (causa != null) {
            if (causa instanceof ConstraintViolationException cve
                    && INDICE_RESERVA_UNICA.equalsIgnoreCase(cve.getConstraintName())) {
                return true;
            }
            if (causa.getMessage() != null && causa.getMessage().contains(INDICE_RESERVA_UNICA)) {
                return true;
            }
            causa = causa.getCause();
        }
        return false;
    }
}
