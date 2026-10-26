package canchapro.disponibilidad;

/**
 * Un horario en el que una cancha ya está ocupada (por ejemplo, una reserva).
 *
 * @param codigoCancha cancha ocupada
 * @param intervalo    horario ocupado
 * @param referencia   código de lo que la ocupa (por ejemplo RES-00012)
 * @param descripcion  texto breve para mostrar (por ejemplo el nombre del cliente)
 */
// POO - Encapsulamiento: record inmutable. POO - Relaciones: TIENE un IntervaloHorario.
public record Ocupacion(String codigoCancha, IntervaloHorario intervalo, String referencia, String descripcion) {
}
