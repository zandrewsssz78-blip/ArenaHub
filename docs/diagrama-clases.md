# TAREA 3 — Diagrama de clases (derivado)

## Rebanada seleccionada

Crear una reserva.

## Diagrama de clases

```mermaid
classDiagram

    class Reserva {
        +Long id
        +String codigoReserva
        +EstadoReserva estado
        +LocalDateTime fechaCreacion
        +String observacion
    }