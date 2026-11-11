package canchapro.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

// POO - Encapsulamiento: record inmutable con los indicadores calculados.
public record ResumenDashboard(LocalDate fecha, int reservasHoy, int enCurso, int pendientesConfirmar,
        BigDecimal ingresosHoy, BigDecimal saldoPorCobrar, int porcentajeOcupacion) {
}
