package canchapro.tarifas;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Agrupación de días para las tarifas.
 */
// POO - Encapsulamiento: enumeración con comportamiento (de(fecha) decide el tipo de día).
public enum TipoDia {

    LUNES_VIERNES("Lunes a viernes"),
    SABADO_DOMINGO("Sábado y domingo");

    private final String nombreVisible;

    TipoDia(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    /** Tipo de día que corresponde a una fecha. */
    public static TipoDia de(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY ? SABADO_DOMINGO : LUNES_VIERNES;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
