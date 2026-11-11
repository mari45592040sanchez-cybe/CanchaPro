package canchapro.disponibilidad;

import canchapro.canchas.TipoCancha;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

// POO - Encapsulamiento: record inmutable con los datos de la consulta (fecha, hora, duración, tipo).
public record SolicitudDisponibilidad(LocalDate fecha, LocalTime horaInicio, Duration duracion, TipoCancha tipoCancha) {

    public LocalTime horaFin() {
        return horaInicio.plus(duracion);
    }

    public IntervaloHorario intervalo() {
        return IntervaloHorario.de(fecha, horaInicio, duracion);
    }
}
