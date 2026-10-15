package canchapro.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Formatos de fecha y hora usados en archivos (ISO-8601) y en pantalla.
 */
public final class FormatoFecha {

    /** Formato para archivos: 2026-10-08 */
    public static final DateTimeFormatter ARCHIVO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    /** Formato para archivos: 18:30 */
    public static final DateTimeFormatter ARCHIVO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    /** Formato para archivos: 2026-10-08T18:30:00 */
    public static final DateTimeFormatter ARCHIVO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Formato para pantalla: 08/10/2026 */
    public static final DateTimeFormatter VISTA_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Formato para pantalla: 08/10/2026 18:30 */
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
