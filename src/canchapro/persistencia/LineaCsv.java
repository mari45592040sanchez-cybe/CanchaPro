package canchapro.persistencia;

/**
 * Una línea leída de un archivo CSV, con su número para reportar errores.
 *
 * @param numero número de línea dentro del archivo (el encabezado es la 1)
 * @param texto  contenido original de la línea
 * @param campos valores separados por ';'
 */
// POO - Encapsulamiento: record inmutable; los campos de la línea se guardan en un arreglo String[].
public record LineaCsv(int numero, String texto, String[] campos) {
}
