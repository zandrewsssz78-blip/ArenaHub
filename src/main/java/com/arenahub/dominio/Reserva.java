package com.arenahub.dominio;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_reserva", nullable = false, length = 50, unique = true)
    private String codigoReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "escenario_id", nullable = false)
    private EscenarioDeportivo escenario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "franja_id", nullable = false)
    private FranjaHoraria franja;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(length = 500)
    private String observacion;

    protected Reserva() {
        // requerido por JPA
    }

    /**
     * Crea una reserva CONFIRMADA aplicando las reglas del dominio.
     * La garantía de "una sola reserva confirmada por escenario/fecha/franja"
     * bajo concurrencia NO se decide aquí: la impone la base de datos
     * (índice único parcial uq_reserva_confirmada).
     */
    public static Reserva confirmar(Usuario usuario, EscenarioDeportivo escenario,
                                    FranjaHoraria franja, LocalDate fecha) {
        return confirmar(usuario, escenario, franja, fecha, LocalDate.now());
    }

    /**
     * Igual que {@link #confirmar(Usuario, EscenarioDeportivo, FranjaHoraria, LocalDate)},
     * recibiendo "hoy" de forma explícita para poder probar la regla de fechas pasadas
     * sin depender del reloj del sistema.
     */
    public static Reserva confirmar(Usuario usuario, EscenarioDeportivo escenario,
                                    FranjaHoraria franja, LocalDate fecha, LocalDate hoy) {
        if (!usuario.isActivo()) {
            throw new ReglaNegocioException("El usuario no está activo");
        }
        if (!escenario.admiteReservas()) {
            throw new ReglaNegocioException(
                    "El escenario '" + escenario.getNombre() + "' no está disponible para reservas");
        }
        if (fecha == null) {
            throw new ReglaNegocioException("La fecha de la reserva es obligatoria");
        }
        if (fecha.isBefore(hoy)) {
            throw new ReglaNegocioException("No se puede reservar en una fecha pasada");
        }
        Reserva reserva = new Reserva();
        reserva.codigoReserva = "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        reserva.usuario = usuario;
        reserva.escenario = escenario;
        reserva.franja = franja;
        reserva.fecha = fecha;
        reserva.estado = EstadoReserva.CONFIRMADA;
        reserva.fechaCreacion = LocalDateTime.now();
        return reserva;
    }

    /**
     * HU5 — Cancela una reserva aplicando las reglas de negocio:
     * - Solo el usuario dueño de la reserva puede cancelarla.
     * - No se puede cancelar una reserva que ya está cancelada.
     */
    public void cancelar(Long usuarioIdSolicitante) {
        if (this.usuario == null || !this.usuario.coincideConId(usuarioIdSolicitante)) {
            throw new ReglaNegocioException("No tienes permiso para cancelar una reserva de otro usuario");
        }
        if (this.estado == EstadoReserva.CANCELADA) {
            throw new ReglaNegocioException("La reserva ya se encuentra cancelada");
        }
        this.estado = EstadoReserva.CANCELADA;
    }

    public Long getId() { return id; }
    public String getCodigoReserva() { return codigoReserva; }
    public Usuario getUsuario() { return usuario; }
    public EscenarioDeportivo getEscenario() { return escenario; }
    public FranjaHoraria getFranja() { return franja; }
    public LocalDate getFecha() { return fecha; }
    public EstadoReserva getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public String getObservacion() { return observacion; }
}
