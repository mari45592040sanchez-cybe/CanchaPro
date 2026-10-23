package canchapro.util;

/**
 * Genera el siguiente código correlativo a partir de los códigos existentes.
 * Ejemplo: prefijo "USR-", 3 dígitos, existentes [USR-001, USR-004] → USR-005
 */
// POO - Reutilización: la usan Usuarios, Clientes y los demás módulos para generar sus códigos.
public final class GeneradorCodigo {

    private GeneradorCodigo() {
    }

    public static String siguiente(String prefijo, int digitos, String[] existentes) {
        int mayor = 0;
        for (String codigo : existentes) {
            if (codigo != null && codigo.startsWith(prefijo)) {
                try {
                    mayor = Math.max(mayor, Integer.parseInt(codigo.substring(prefijo.length())));
                } catch (NumberFormatException e) {
                    // Código con otro formato: no participa en el correlativo.
                }
            }
        }
        return prefijo + String.format("%0" + digitos + "d", mayor + 1);
    }
}
