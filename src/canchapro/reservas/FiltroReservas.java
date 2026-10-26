package canchapro.reservas;

import java.time.LocalDate;

/**
 * Criterios de búsqueda de reservas. Los valores null no filtran.
 *
 * @param desde        fecha inicial (incluida)
 * @param hasta        fecha final (incluida)
 * @param codigoCancha cancha
 * @param estado       estado
 * @param texto        código de reserva, nombre, teléfono o código del cliente
 */
// POO - Encapsulamiento: record inmutable con los filtros de búsqueda.
public record FiltroReservas(LocalDate desde, LocalDate hasta, String codigoCancha, EstadoReserva estado, String texto) {
}
