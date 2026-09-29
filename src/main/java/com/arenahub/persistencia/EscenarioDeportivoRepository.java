package com.arenahub.persistencia;

import com.arenahub.dominio.EscenarioDeportivo;
import com.arenahub.dominio.EstadoEscenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscenarioDeportivoRepository extends JpaRepository<EscenarioDeportivo, Long> {
    List<EscenarioDeportivo> findByEstadoOrderByNombreAsc(EstadoEscenario estado);
}
