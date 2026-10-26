package canchapro.canchas;

/**
 * Tipos de cancha. La capacidad sugerida se propone al registrar una cancha.
 */
// POO - Encapsulamiento: enumeración con sus propios atributos (nombre y capacidad sugerida).
public enum TipoCancha {

    FUTBOL_5("Fútbol 5", 10),
    FUTBOL_7("Fútbol 7", 14),
    FUTBOL_11("Fútbol 11", 22);

    private final String nombreVisible;
    private final int capacidadSugerida;

    TipoCancha(String nombreVisible, int capacidadSugerida) {
        this.nombreVisible = nombreVisible;
        this.capacidadSugerida = capacidadSugerida;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public int getCapacidadSugerida() {
        return capacidadSugerida;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
