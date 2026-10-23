package canchapro.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Utilidades de texto para búsquedas.
 */
// POO - Reutilización: utilidades de texto (búsqueda sin tildes) que usan varios módulos.
public final class Texto {

    private Texto() {
    }

    /**
     * Convierte a minúsculas y quita tildes, para que "Pérez", "PEREZ" y
     * "perez" se consideren iguales al buscar.
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /**
     * Indica si todas las palabras de la búsqueda aparecen en el texto,
     * en cualquier orden. "juan perez" encuentra "Juan Carlos Pérez".
     */
    public static boolean contieneTodas(String texto, String busqueda) {
        String base = normalizar(texto);
        for (String palabra : normalizar(busqueda).split("\\s+")) {
            if (!base.contains(palabra)) {
                return false;
            }
        }
        return true;
    }
}
