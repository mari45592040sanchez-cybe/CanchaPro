package canchapro.util;

import canchapro.util.excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Validaciones reutilizables para los datos de entrada.
 * Cada método lanza {@link DatosInvalidosException} con un mensaje
 * listo para mostrarse al usuario.
 */
public final class Validador {

    private static final Pattern TELEFONO = Pattern.compile("\\d{8}");
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validador() {
    }

    /**
     * Verifica que el valor no esté vacío y lo devuelve sin espacios al inicio/fin.
     */
    public static String requerido(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException(campo + " es obligatorio.");
        }
        return valor.trim();
    }

    /**
     * Devuelve el valor sin espacios, o cadena vacía si es nulo.
     */
    public static String opcional(String valor) {
        return valor == null ? "" : valor.trim();
    }

    public static void longitud(String valor, int minimo, int maximo, String campo) {
        int largo = valor == null ? 0 : valor.length();
        if (largo < minimo || largo > maximo) {
            throw new DatosInvalidosException(
                    campo + " debe tener entre " + minimo + " y " + maximo + " caracteres.");
        }
    }

    public static boolean telefonoValido(String telefono) {
        return telefono != null && TELEFONO.matcher(telefono).matches();
    }

    public static boolean correoValido(String correo) {
        return correo != null && CORREO.matcher(correo).matches();
    }

    /**
     * Los archivos usan ';' como separador, por lo que ningún texto puede
     * contener ese carácter ni saltos de línea.
     */
    public static void sinSeparadorCsv(String valor, String campo) {
        if (valor != null && (valor.contains(";") || valor.contains("\n") || valor.contains("\r"))) {
            throw new DatosInvalidosException(campo + " no puede contener ';' ni saltos de línea.");
        }
    }

    /**
     * Convierte un monto escrito por el usuario ("150", "1,250.50", "Q 75")
     * a BigDecimal.
     */
    public static BigDecimal monto(String texto, String campo) {
        String limpio = requerido(texto, campo)
                .replace("Q", "").replace("q", "").replace(",", "").replace(" ", "");
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException e) {
            throw new DatosInvalidosException(campo + " debe ser un número (ejemplo: 150.00).");
        }
    }

    /**
     * Valida un precio: mayor que cero, no mayor que el máximo y con dos
     * decimales como máximo. Devuelve el precio con escala 2.
     */
    public static BigDecimal precio(BigDecimal precio, BigDecimal maximo, String campo) {
        if (precio == null || precio.signum() <= 0) {
            throw new DatosInvalidosException(campo + " debe ser mayor que cero.");
        }
        if (precio.compareTo(maximo) > 0) {
            throw new DatosInvalidosException(campo + " no puede ser mayor a Q" + maximo + ".");
        }
        if (precio.stripTrailingZeros().scale() > 2) {
            throw new DatosInvalidosException(campo + " admite como máximo dos decimales.");
        }
        return precio.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Valida un texto obligatorio completo: requerido, longitud y sin separador.
     */
    public static String texto(String valor, int minimo, int maximo, String campo) {
        String limpio = requerido(valor, campo);
        longitud(limpio, minimo, maximo, campo);
        sinSeparadorCsv(limpio, campo);
        return limpio;
    }
}
