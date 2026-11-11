package canchapro.util;

import java.text.Normalizer;
import java.util.Locale;

// POO - Reutilización: utilidades de texto (búsqueda sin tildes) que usan varios módulos.
public final class Texto {

    private Texto() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

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
