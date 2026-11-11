package canchapro.reportes;

import canchapro.reservas.EstadoReserva;
import java.time.LocalDate;

public record FiltroReporte(LocalDate desde, LocalDate hasta, String codigoCancha, EstadoReserva estado,
        String textoCliente) {
}
