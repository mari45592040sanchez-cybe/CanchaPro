package canchapro.persistencia;

// POO - Encapsulamiento: un record es inmutable; sus datos no se pueden cambiar después de creado.
// POO - Colecciones: las líneas se entregan en un arreglo de objetos LineaCsv.
public record ContenidoCsv(String encabezado, LineaCsv[] lineas) {
}
