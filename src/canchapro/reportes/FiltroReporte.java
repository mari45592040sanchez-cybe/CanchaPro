package canchapro.reportes;

import canchapro.reservas.EstadoReserva;
import java.time.LocalDate;

/**
 * Criterios para generar un reporte. Un valor null no filtra.
 *
 * @param desde        fecha inicial (incluida)
 * @param hasta        fecha final (incluida)
 * @param codigoCancha cancha
 * @param estado       estado de la reserva
 * @param textoCliente nombre, teléfono o código del cliente
 */
public record FiltroReporte(LocalDate desde, LocalDate hasta, String codigoCancha, EstadoReserva estado,
        String textoCliente) {
}
