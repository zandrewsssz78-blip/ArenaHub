package com.arenahub.persistencia;

import com.arenahub.dominio.FranjaHoraria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FranjaHorariaRepository extends JpaRepository<FranjaHoraria, Long> {
    List<FranjaHoraria> findAllByOrderByHoraInicioAsc();
}
