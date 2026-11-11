package canchapro.canchas;

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
