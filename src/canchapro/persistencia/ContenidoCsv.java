package canchapro.persistencia;

/**
 * Contenido leído de un archivo CSV.
 *
 * @param encabezado primera línea del archivo (vacía si el archivo estaba vacío)
 * @param lineas     líneas de datos, sin el encabezado ni líneas en blanco
 */
// POO - Encapsulamiento: un record es inmutable; sus datos no se pueden cambiar después de creado.
// POO - Colecciones: las líneas se entregan en un arreglo de objetos LineaCsv.
public record ContenidoCsv(String encabezado, LineaCsv[] lineas) {
}
