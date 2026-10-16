package canchapro.persistencia;

/**
 * Una línea leída de un archivo CSV, con su número para reportar errores.
 *
 * @param numero número de línea dentro del archivo (el encabezado es la 1)
 * @param texto  contenido original de la línea
 * @param campos valores separados por ';'
 */
public record LineaCsv(int numero, String texto, String[] campos) {
}
