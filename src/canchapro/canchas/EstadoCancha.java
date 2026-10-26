package canchapro.canchas;

/**
 * Estado operativo de una cancha. Solo una cancha DISPONIBLE puede
 * aparecer en la consulta de disponibilidad y recibir reservas.
 */
// POO - Encapsulamiento: enumeración con atributo y comportamiento (permiteReservas).
public enum EstadoCancha {

    DISPONIBLE("Disponible"),
    MANTENIMIENTO("En mantenimiento"),
    INACTIVA("Inactiva");

    private final String nombreVisible;

    EstadoCancha(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public boolean permiteReservas() {
        return this == DISPONIBLE;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
