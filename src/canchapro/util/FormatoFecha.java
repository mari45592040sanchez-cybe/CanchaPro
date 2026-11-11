package canchapro.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class FormatoFecha {

    public static final DateTimeFormatter ARCHIVO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter ARCHIVO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter ARCHIVO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static final DateTimeFormatter VISTA_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter VISTA_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private FormatoFecha() {
    }

    public static String vista(LocalDate fecha) {
        return fecha == null ? "" : fecha.format(VISTA_FECHA);
    }

    public static String vista(LocalDateTime fechaHora) {
        return fechaHora == null ? "" : fechaHora.format(VISTA_FECHA_HORA);
    }

    public static String vista(LocalTime hora) {
        return hora == null ? "" : hora.format(ARCHIVO_HORA);
    }
}
