package canchapro.util;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Las horas del sistema se manejan en bloques de 30 minutos (08:00, 08:30…)
 * y las reservas duran de 1 a 4 horas. Esta clase centraliza esas reglas
 * para que todas las pantallas ofrezcan las mismas opciones.
 */
// POO - Reutilización: las pantallas de Canchas, Tarifas, Disponibilidad y Reservas usan las mismas horas.
public final class BloquesHorario {

    public static final int MINUTOS_BLOQUE = 30;
    public static final Duration DURACION_MINIMA = Duration.ofMinutes(60);
    public static final Duration DURACION_MAXIMA = Duration.ofMinutes(240);
    private static final LocalTime ULTIMO_BLOQUE = LocalTime.of(23, 30);

    private BloquesHorario() {
    }

    /** 00:00, 00:30, …, 23:30 */
    public static LocalTime[] horasDelDia() {
        return horasEntre(LocalTime.MIDNIGHT, ULTIMO_BLOQUE);
    }

    /** Horas de 30 en 30 minutos entre desde y hasta (ambas incluidas). */
    // POO - Colecciones: se calcula el tamaño exacto y se llena un arreglo de LocalTime.
    public static LocalTime[] horasEntre(LocalTime desde, LocalTime hasta) {
        if (desde.isAfter(hasta)) {
            return new LocalTime[0];
        }
        int cantidad = (int) (Duration.between(desde, hasta).toMinutes() / MINUTOS_BLOQUE) + 1;
        LocalTime[] horas = new LocalTime[cantidad];
        LocalTime hora = desde;
        for (int i = 0; i < cantidad; i++) {
            horas[i] = hora;
            hora = hora.plusMinutes(MINUTOS_BLOQUE);
        }
        return horas;
    }

    /** Duraciones permitidas para una reserva: 1 h, 1 h 30 min, …, 4 h. */
    public static OpcionDuracion[] duracionesPermitidas() {
        int cantidad = (int) ((DURACION_MAXIMA.toMinutes() - DURACION_MINIMA.toMinutes()) / MINUTOS_BLOQUE) + 1;
        OpcionDuracion[] opciones = new OpcionDuracion[cantidad];
        Duration d = DURACION_MINIMA;
        for (int i = 0; i < cantidad; i++) {
            opciones[i] = new OpcionDuracion(d);
            d = d.plusMinutes(MINUTOS_BLOQUE);
        }
        return opciones;
    }

    /** "1 h", "1 h 30 min", "45 min" */
    public static String describir(Duration duracion) {
        long horas = duracion.toHours();
        int minutos = duracion.toMinutesPart();
        if (horas == 0) {
            return minutos + " min";
        }
        return minutos == 0 ? horas + " h" : horas + " h " + minutos + " min";
    }

    /**
     * Opción de duración para listas desplegables.
     *
     * @param duracion duración representada
     */
    public record OpcionDuracion(Duration duracion) {

        @Override
        public String toString() {
            return describir(duracion);
        }
    }
}
