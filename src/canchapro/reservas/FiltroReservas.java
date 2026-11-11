package canchapro.reservas;

import java.time.LocalDate;

// POO - Encapsulamiento: record inmutable con los filtros de búsqueda.
public record FiltroReservas(LocalDate desde, LocalDate hasta, String codigoCancha, EstadoReserva estado, String texto) {
}
