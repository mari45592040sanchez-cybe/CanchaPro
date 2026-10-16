package canchapro.persistencia;

import canchapro.util.FormatoFecha;
import canchapro.util.excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Conversión de campos de texto del CSV a tipos Java, con mensajes claros
 * cuando el valor no es válido.
 */
public final class CamposCsv {

    private CamposCsv() {
    }

    public static boolean booleano(String valor, String campo) {
        String limpio = valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
        return switch (limpio) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new DatosInvalidosException(campo + ": valor booleano inválido '" + valor + "'");
        };
    }

    public static <E extends Enum<E>> E enumeracion(Class<E> tipo, String valor, String campo) {
        try {
            return Enum.valueOf(tipo, valor.trim());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new DatosInvalidosException(campo + ": valor desconocido '" + valor + "'");
        }
    }

    /** Fecha en formato yyyy-MM-dd. */
    public static LocalDate fecha(String valor, String campo) {
        try {
            return LocalDate.parse(valor.trim(), FormatoFecha.ARCHIVO_FECHA);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DatosInvalidosException(campo + ": fecha inválida '" + valor + "' (formato yyyy-MM-dd)");
        }
    }

    /** Hora en formato HH:mm. */
    public static LocalTime hora(String valor, String campo) {
        try {
            return LocalTime.parse(valor.trim(), FormatoFecha.ARCHIVO_HORA);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DatosInvalidosException(campo + ": hora inválida '" + valor + "' (formato HH:mm)");
        }
    }

    /** Fecha y hora en formato yyyy-MM-ddTHH:mm:ss. */
    public static LocalDateTime fechaHora(String valor, String campo) {
        try {
            return LocalDateTime.parse(valor.trim(), FormatoFecha.ARCHIVO_FECHA_HORA);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DatosInvalidosException(campo + ": fecha y hora inválida '" + valor
                    + "' (formato yyyy-MM-ddTHH:mm:ss)");
        }
    }

    public static int entero(String valor, String campo) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new DatosInvalidosException(campo + ": número entero inválido '" + valor + "'");
        }
    }

    /** Monto con punto decimal, por ejemplo 150.00 */
    public static BigDecimal decimal(String valor, String campo) {
        try {
            return new BigDecimal(valor.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new DatosInvalidosException(campo + ": monto inválido '" + valor + "'");
        }
    }
}
