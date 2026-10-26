package canchapro.disponibilidad;

import canchapro.canchas.TipoCancha;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Lo que el usuario quiere reservar: fecha, hora de inicio, duración y
 * (opcionalmente) tipo de cancha.
 *
 * @param fecha      fecha solicitada
 * @param horaInicio hora de inicio
 * @param duracion   duración de la reserva
 * @param tipoCancha tipo de cancha, o null para consultar todos los tipos
 */
// POO - Encapsulamiento: record inmutable con los datos de la consulta (fecha, hora, duración, tipo).
public record SolicitudDisponibilidad(LocalDate fecha, LocalTime horaInicio, Duration duracion, TipoCancha tipoCancha) {

    /** Hora final calculada (inicio + duración). */
    public LocalTime horaFin() {
        return horaInicio.plus(duracion);
    }

    public IntervaloHorario intervalo() {
        return IntervaloHorario.de(fecha, horaInicio, duracion);
    }
}
