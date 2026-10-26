package canchapro.tarifas;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Parte de una reserva cobrada con una misma tarifa.
 * Ejemplo: 16:00-17:00, 60 min, Q150.00/h → Q150.00
 *
 * @param horaDesde  inicio del tramo
 * @param horaHasta  fin del tramo
 * @param minutos    duración del tramo
 * @param precioHora precio por hora aplicado
 * @param subtotal   precioHora × minutos / 60
 */
// POO - Encapsulamiento: record inmutable con un tramo del cálculo de precio.
public record TramoTarifa(LocalTime horaDesde, LocalTime horaHasta, int minutos,
        BigDecimal precioHora, BigDecimal subtotal) {
}
