package canchapro.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

/**
 * Forma de cobrar un servicio adicional. Cada constante sabe calcular su
 * propio importe (comportamiento polimórfico en el enum).
 */
// POO - Polimorfismo: cada constante calcula el importe a su manera (por reserva o por hora).
public enum TipoCobro {

    /** Se cobra una vez por unidad, sin importar la duración (balón, agua). */
    POR_RESERVA("Por reserva") {
        @Override
        public BigDecimal calcular(BigDecimal precio, int cantidad, Duration duracion) {
            return precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
        }
    },

    /** Se cobra por cada hora de la reserva (árbitro, iluminación). */
    POR_HORA("Por hora") {
        @Override
        public BigDecimal calcular(BigDecimal precio, int cantidad, Duration duracion) {
            BigDecimal horas = BigDecimal.valueOf(duracion.toMinutes())
                    .divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
            return precio.multiply(BigDecimal.valueOf(cantidad)).multiply(horas).setScale(2, RoundingMode.HALF_UP);
        }
    };

    private final String nombreVisible;

    TipoCobro(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    /**
     * Importe del servicio.
     *
     * @param precio   precio unitario (por reserva o por hora)
     * @param cantidad unidades solicitadas
     * @param duracion duración de la reserva
     */
    public abstract BigDecimal calcular(BigDecimal precio, int cantidad, Duration duracion);

    @Override
    public String toString() {
        return nombreVisible;
    }
}
