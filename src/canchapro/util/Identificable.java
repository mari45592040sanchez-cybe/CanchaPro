package canchapro.util;

/**
 * Toda entidad que se guarda en archivo se identifica por un código único.
 */
// POO - Polimorfismo (interfaz): cualquier entidad con código (Cliente, Usuario...) se puede guardar
// en un RepositorioCsv, sin que el repositorio conozca su clase concreta.
public interface Identificable {

    String getCodigo();
}
