package canchapro.tarifas;

import java.math.BigDecimal;
import java.time.LocalTime;

// POO - Encapsulamiento: record inmutable con un tramo del cálculo de precio.
public record TramoTarifa(LocalTime horaDesde, LocalTime horaHasta, int minutos,
        BigDecimal precioHora, BigDecimal subtotal) {
}
