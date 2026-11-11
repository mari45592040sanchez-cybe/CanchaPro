package canchapro.servicios;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

// POO - Polimorfismo: cada constante calcula el importe a su manera (por reserva o por hora).
public enum TipoCobro {

    POR_RESERVA("Por reserva") {
        @Override
        public BigDecimal calcular(BigDecimal precio, int cantidad, Duration duracion) {
            return precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
        }
    },

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

    public abstract BigDecimal calcular(BigDecimal precio, int cantidad, Duration duracion);

    @Override
    public String toString() {
        return nombreVisible;
    }
}
