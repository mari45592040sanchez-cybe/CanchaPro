package canchapro.util;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Las horas del sistema se manejan en bloques de 30 minutos (08:00, 08:30…)
 * y las reservas duran de 1 a 4 horas. Esta clase centraliza esas reglas
 * para que todas las pantallas ofrezcan las mismas opciones.
 */
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
    public static LocalTime[] horasEntre(LocalTime desde, LocalTime hasta) {
        List<LocalTime> horas = new ArrayList<>();
        for (LocalTime hora = desde; !hora.isAfter(hasta); hora = hora.plusMinutes(MINUTOS_BLOQUE)) {
            horas.add(hora);
            if (hora.equals(ULTIMO_BLOQUE)) {
                break;
            }
        }
        return horas.toArray(LocalTime[]::new);
    }

    /** Duraciones permitidas para una reserva: 1 h, 1 h 30 min, …, 4 h. */
    public static OpcionDuracion[] duracionesPermitidas() {
        List<OpcionDuracion> opciones = new ArrayList<>();
        for (Duration d = DURACION_MINIMA; d.compareTo(DURACION_MAXIMA) <= 0; d = d.plusMinutes(MINUTOS_BLOQUE)) {
            opciones.add(new OpcionDuracion(d));
        }
        return opciones.toArray(OpcionDuracion[]::new);
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
