package canchapro.disponibilidad;

import canchapro.util.excepciones.DatosInvalidosException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    public boolean seTraslapaCon(IntervaloHorario otro) {
        return inicio.isBefore(otro.fin) && fin.isAfter(otro.inicio);
    }

    public boolean contiene(LocalDateTime momento) {
        return !momento.isBefore(inicio) && momento.isBefore(fin);
    }

    public Duration duracion() {
        return Duration.between(inicio, fin);
    }

    public String describirHoras() {
        return inicio.toLocalTime() + " a " + fin.toLocalTime();
    }
}
