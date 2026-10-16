package canchapro.persistencia;

import java.util.List;

/**
 * Contenido leído de un archivo CSV.
 *
 * @param encabezado primera línea del archivo (vacía si el archivo estaba vacío)
 * @param lineas     líneas de datos, sin el encabezado ni líneas en blanco
 */
public record ContenidoCsv(String encabezado, List<LineaCsv> lineas) {
}
