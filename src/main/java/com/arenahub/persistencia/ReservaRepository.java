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

    /** HU6 — Listar reservas de un usuario con los datos de escenario y franja. */
    @Query("""
           select r from Reserva r
           join fetch r.escenario
           join fetch r.franja
           join fetch r.usuario
           where r.usuario.id = :usuarioId
           order by r.fecha desc, r.franja.horaInicio asc
           """)
    List<Reserva> buscarPorUsuarioConDetalle(@Param("usuarioId") Long usuarioId);

    /** Buscar reserva por ID cargando escenario, franja y usuario en un solo query. */
    @Query("""
           select r from Reserva r
           join fetch r.escenario
           join fetch r.franja
           join fetch r.usuario
           where r.id = :id
           """)
    java.util.Optional<Reserva> findByIdConDetalle(@Param("id") Long id);
}
