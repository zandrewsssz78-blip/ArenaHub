package com.arenahub.persistencia;

import com.arenahub.dominio.EstadoReserva;
import com.arenahub.dominio.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    boolean existsByEscenarioIdAndFechaAndFranjaIdAndEstado(
            Long escenarioId, LocalDate fecha, Long franjaId, EstadoReserva estado);

    @Query("""
           select new com.arenahub.persistencia.FranjaOcupada(r.escenario.id, r.franja.id)
           from Reserva r
           where r.fecha = :fecha and r.estado = com.arenahub.dominio.EstadoReserva.CONFIRMADA
           """)
    List<FranjaOcupada> buscarFranjasOcupadas(@Param("fecha") LocalDate fecha);
}
