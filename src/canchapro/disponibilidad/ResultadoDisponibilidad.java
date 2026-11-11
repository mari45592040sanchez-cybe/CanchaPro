package canchapro.disponibilidad;

import canchapro.canchas.Cancha;
import canchapro.tarifas.DetallePrecio;

// POO - Encapsulamiento: record inmutable. POO - Relaciones: TIENE una cancha y su DetallePrecio.
public record ResultadoDisponibilidad(Cancha cancha, boolean disponible, String motivo, DetallePrecio precio) {

    public static ResultadoDisponibilidad disponible(Cancha cancha, DetallePrecio precio) {
        return new ResultadoDisponibilidad(cancha, true, "", precio);
    }

    public static ResultadoDisponibilidad noDisponible(Cancha cancha, String motivo) {
        return new ResultadoDisponibilidad(cancha, false, motivo, null);
    }
}
