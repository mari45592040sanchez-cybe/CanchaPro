package canchapro.disponibilidad;

// POO - Encapsulamiento: record inmutable. POO - Relaciones: TIENE un IntervaloHorario.
public record Ocupacion(String codigoCancha, IntervaloHorario intervalo, String referencia, String descripcion) {
}
