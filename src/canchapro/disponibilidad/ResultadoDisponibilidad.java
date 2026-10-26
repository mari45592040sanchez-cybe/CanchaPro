package canchapro.disponibilidad;

import canchapro.canchas.Cancha;
import canchapro.tarifas.DetallePrecio;

/**
 * Resultado de evaluar una cancha para una solicitud.
 *
 * @param cancha      cancha evaluada
 * @param disponible  true si se puede reservar
 * @param motivo      por qué no está disponible (vacío si lo está)
 * @param precio      precio calculado (null si no está disponible)
 */
// POO - Encapsulamiento: record inmutable. POO - Relaciones: TIENE una cancha y su DetallePrecio.
public record ResultadoDisponibilidad(Cancha cancha, boolean disponible, String motivo, DetallePrecio precio) {

    public static ResultadoDisponibilidad disponible(Cancha cancha, DetallePrecio precio) {
        return new ResultadoDisponibilidad(cancha, true, "", precio);
    }

    public static ResultadoDisponibilidad noDisponible(Cancha cancha, String motivo) {
        return new ResultadoDisponibilidad(cancha, false, motivo, null);
    }
}
