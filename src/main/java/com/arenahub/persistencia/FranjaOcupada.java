package com.arenahub.persistencia;

/** Par (escenario, franja) que ya tiene una reserva confirmada en una fecha. */
public record FranjaOcupada(Long escenarioId, Long franjaId) {
}
