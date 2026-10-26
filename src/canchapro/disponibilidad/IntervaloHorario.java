package canchapro.disponibilidad;

import canchapro.util.excepciones.DatosInvalidosException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Período de uso de una cancha, de {@code inicio} (incluido) a {@code fin}
 * (excluido). Es inmutable.
 *
 * @param inicio fecha y hora de inicio
 * @param fin    fecha y hora de fin
 */
// POO - Encapsulamiento / Responsabilidad: record inmutable que implementa la regla de traslape.
public record IntervaloHorario(LocalDateTime inicio, LocalDateTime fin) {

    public IntervaloHorario {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new DatosInvalidosException("El inicio del horario debe ser anterior al fin.");
        }
    }

    public static IntervaloHorario de(LocalDate fecha, LocalTime inicio, Duration duracion) {
        LocalDateTime desde = fecha.atTime(inicio);
        return new IntervaloHorario(desde, desde.plus(duracion));
    }

    /**
     * Regla de traslape del enunciado. Hay conflicto cuando:
     * <pre>inicioNuevo &lt; finExistente  AND  finNuevo &gt; inicioExistente</pre>
     * Una reserva que inicia exactamente cuando termina otra NO choca.
     */
    public boolean seTraslapaCon(IntervaloHorario otro) {
        return inicio.isBefore(otro.fin) && fin.isAfter(otro.inicio);
    }

    /** Indica si el bloque que inicia en {@code momento} está dentro del intervalo. */
    public boolean contiene(LocalDateTime momento) {
        return !momento.isBefore(inicio) && momento.isBefore(fin);
    }

    public Duration duracion() {
        return Duration.between(inicio, fin);
    }

    /** "18:00 a 19:30" */
    public String describirHoras() {
        return inicio.toLocalTime() + " a " + fin.toLocalTime();
    }
}
