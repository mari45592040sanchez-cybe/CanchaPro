package canchapro.persistencia;

/**
 * Un problema encontrado al leer un archivo de datos o de configuración.
 *
 * @param archivo   nombre del archivo
 * @param linea     número de línea (0 cuando el error no corresponde a una línea concreta)
 * @param contenido texto original de la línea (vacío si no aplica)
 * @param motivo    explicación del problema
 */
public record ErrorCarga(String archivo, int linea, String contenido, String motivo) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(archivo);
        if (linea > 0) {
            sb.append(" línea ").append(linea);
        }
        sb.append(": ").append(motivo);
        if (!contenido.isEmpty()) {
            sb.append(" → [").append(contenido).append(']');
        }
        return sb.toString();
    }
}
