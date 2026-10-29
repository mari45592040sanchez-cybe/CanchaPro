package canchapro.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Indicadores de la pantalla de inicio.
 *
 * @param fecha                fecha del resumen (hoy)
 * @param reservasHoy          reservas de hoy que no están canceladas
 * @param enCurso              reservas que se están jugando en este momento
 * @param pendientesConfirmar  reservas PENDIENTES desde hoy (sin anticipo suficiente)
 * @param ingresosHoy          suma de los pagos recibidos hoy
 * @param saldoPorCobrar       suma de los saldos de las reservas no canceladas
 * @param porcentajeOcupacion  horas reservadas hoy / horas de atención de las canchas disponibles
 */
// POO - Encapsulamiento: record inmutable con los indicadores calculados.
public record ResumenDashboard(LocalDate fecha, int reservasHoy, int enCurso, int pendientesConfirmar,
        BigDecimal ingresosHoy, BigDecimal saldoPorCobrar, int porcentajeOcupacion) {
}
