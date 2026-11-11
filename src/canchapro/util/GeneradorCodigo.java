package canchapro.util;

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
                }
            }
        }
        return prefijo + String.format("%0" + digitos + "d", mayor + 1);
    }
}
