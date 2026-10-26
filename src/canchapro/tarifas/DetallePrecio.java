package canchapro.tarifas;

import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Resultado del cálculo del precio de uso de una cancha: los tramos con su
 * tarifa y el total.
 *
 * @param tramos tramos en orden cronológico
 * @param total  suma de los subtotales de los tramos
 */
// POO - Encapsulamiento: record inmutable; el arreglo se copia al crear y al consultar,
// para que nadie pueda cambiar los tramos desde afuera.
// POO - Colecciones: los tramos se guardan en un arreglo de objetos TramoTarifa.
public record DetallePrecio(TramoTarifa[] tramos, BigDecimal total) {

    public DetallePrecio {
        tramos = Arrays.copyOf(tramos, tramos.length);
    }

    @Override
    public TramoTarifa[] tramos() {
        return Arrays.copyOf(tramos, tramos.length);
    }

    public int cantidadTramos() {
        return tramos.length;
    }

    /** true si la reserva cruza más de una franja tarifaria. */
    public boolean tieneVariosTramos() {
        return tramos.length > 1;
    }
}
