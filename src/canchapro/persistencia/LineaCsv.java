package canchapro.persistencia;

// POO - Encapsulamiento: record inmutable; los campos de la línea se guardan en un arreglo String[].
public record LineaCsv(int numero, String texto, String[] campos) {
}
