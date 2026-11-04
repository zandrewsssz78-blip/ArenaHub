package com.arenahub.aplicacion;

import com.arenahub.dominio.*;
import com.arenahub.persistencia.EscenarioDeportivoRepository;
import com.arenahub.persistencia.FranjaHorariaRepository;
import com.arenahub.persistencia.ReservaRepository;
import com.arenahub.persistencia.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * HU4 — Crear una reserva (problema duro: concurrencia).
 * HU5 — Cancelar reserva (SEG-06 / SEG-14: autorización estricta del propietario).
 * SEG-10 — Registro de auditoría de seguridad sin almacenar información sensible.
 *
 * Mecanismo: la verificación previa (existsBy...) solo da un mensaje rápido en el
 * caso secuencial. La garantía real bajo concurrencia es el índice único parcial
 * uq_reserva_confirmada de PostgreSQL: si 20 transacciones insertan a la vez la
 * misma (escenario, fecha, franja) CONFIRMADA, la base de datos acepta una y
 * rechaza las demás con violación de unicidad, que aquí se traduce a 409.
 */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

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
        log.info("Solicitud de reserva: usuarioId={}, escenarioId={}, fecha={}, franjaId={}",
                usuarioId, escenarioId, fecha, franjaId);

        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + usuarioId + " no existe"));
        EscenarioDeportivo escenario = escenarios.findById(escenarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Escenario " + escenarioId + " no existe"));
        FranjaHoraria franja = franjas.findById(franjaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Franja " + franjaId + " no existe"));

        Reserva reserva = Reserva.confirmar(usuario, escenario, franja, fecha);

        if (reservas.existsByEscenarioIdAndFechaAndFranjaIdAndEstado(
                escenarioId, fecha, franjaId, EstadoReserva.CONFIRMADA)) {
            log.warn("Reserva rechazada: horario ocupado (escenarioId={}, fecha={}, franjaId={})",
                    escenarioId, fecha, franjaId);
            throw horarioOcupado();
        }

        try {
            Reserva guardada = reservas.saveAndFlush(reserva);
            log.info("Reserva creada exitosamente: id={}, codigo={}", guardada.getId(), guardada.getCodigoReserva());
            return guardada;
        } catch (DataIntegrityViolationException e) {
            if (esViolacionDeReservaUnica(e)) {
                log.warn("Conflicto de concurrencia: índice único uq_reserva_confirmada activado para escenarioId={}, fecha={}, franjaId={}",
                        escenarioId, fecha, franjaId);
                throw horarioOcupado();
            }
            throw e;
        }
    }

    /**
     * HU5 — Cancelar una reserva.
     * SEG-06 / SEG-14: Aplica validación de autorización estricta verificando que
     * el usuarioId coincida con el dueño de la reserva.
     */
    @Transactional
    public Reserva cancelar(Long reservaId, Long usuarioId) {
        log.info("Solicitud de cancelación de reserva: reservaId={}, solicitanteId={}", reservaId, usuarioId);

        Reserva reserva = reservas.findByIdConDetalle(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva " + reservaId + " no existe"));

        try {
            reserva.cancelar(usuarioId);
        } catch (ReglaNegocioException rne) {
            log.warn("Cancelación denegada por regla de negocio/autorización: reservaId={}, solicitanteId={}, detalle={}",
                    reservaId, usuarioId, rne.getMessage());
            throw rne;
        }

        Reserva actualizada = reservas.save(reserva);
        log.info("Reserva cancelada exitosamente: reservaId={}", reservaId);
        return actualizada;
    }

    /**
     * HU6 — Ver historial de reservas de un usuario.
     */
    @Transactional(readOnly = true)
    public List<Reserva> listarPorUsuario(Long usuarioId) {
        if (!usuarios.existsById(usuarioId)) {
            throw new RecursoNoEncontradoException("Usuario " + usuarioId + " no existe");
        }
        return reservas.buscarPorUsuarioConDetalle(usuarioId);
    }

    @Transactional(readOnly = true)
    public Reserva obtenerPorId(Long reservaId) {
        return reservas.findByIdConDetalle(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva " + reservaId + " no existe"));
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
