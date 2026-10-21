package com.arenahub.aplicacion;

import com.arenahub.dominio.EscenarioDeportivo;
import com.arenahub.dominio.EstadoEscenario;
import com.arenahub.dominio.TipoEscenario;
import com.arenahub.persistencia.EscenarioDeportivoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * HU7 — Registrar un nuevo escenario deportivo (administración).
 * HU8 — Desactivar un escenario deportivo (administración).
 */
@Service
public class EscenarioService {

    private final EscenarioDeportivoRepository escenarios;

    public EscenarioService(EscenarioDeportivoRepository escenarios) {
        this.escenarios = escenarios;
    }

    @Transactional
    public EscenarioDeportivo registrar(String nombre, TipoEscenario tipo, String ubicacion,
                                        Integer capacidad, String descripcion) {
        String nombreLimpio = nombre.trim();
        if (escenarios.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new ConflictoException("Ya existe un escenario con el nombre: " + nombreLimpio);
        }

        EscenarioDeportivo escenario = new EscenarioDeportivo(
                nombreLimpio, tipo, ubicacion.trim(), capacidad, descripcion, EstadoEscenario.ACTIVO
        );

        try {
            return escenarios.saveAndFlush(escenario);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictoException("Ya existe un escenario con el nombre: " + nombreLimpio);
        }
    }

    @Transactional
    public EscenarioDeportivo desactivar(Long id) {
        EscenarioDeportivo escenario = escenarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Escenario " + id + " no existe"));
        escenario.desactivar();
        return escenarios.save(escenario);
    }

    @Transactional
    public EscenarioDeportivo cambiarEstado(Long id, EstadoEscenario nuevoEstado) {
        EscenarioDeportivo escenario = escenarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Escenario " + id + " no existe"));
        escenario.cambiarEstado(nuevoEstado);
        return escenarios.save(escenario);
    }

    @Transactional(readOnly = true)
    public EscenarioDeportivo obtenerPorId(Long id) {
        return escenarios.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Escenario " + id + " no existe"));
    }

    @Transactional(readOnly = true)
    public List<EscenarioDeportivo> listarTodos() {
        return escenarios.findAll();
    }
}
