package com.arenahub.dominio;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "franja_horaria")
public class FranjaHoraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    protected FranjaHoraria() {
        // requerido por JPA
    }

    public FranjaHoraria(LocalTime horaInicio, LocalTime horaFin) {
        if (!horaInicio.isBefore(horaFin)) {
            throw new ReglaNegocioException("La hora de inicio debe ser anterior a la hora de fin");
        }
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public Long getId() { return id; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
}
