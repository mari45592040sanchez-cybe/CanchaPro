package canchapro.reservas;

/**
 * Estados de una reserva y transiciones permitidas.
 *
 * <pre>
 * PENDIENTE  → CONFIRMADA, EN_CURSO, CANCELADA
 * CONFIRMADA → EN_CURSO, CANCELADA
 * EN_CURSO   → FINALIZADA
 * FINALIZADA → (ninguno)
 * CANCELADA  → (ninguno)
 * </pre>
 */
// POO - Encapsulamiento: enumeración con comportamiento; ella misma decide a qué estados puede pasar.
public enum EstadoReserva {

    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    EN_CURSO("En curso"),
    FINALIZADA("Finalizada"),
    CANCELADA("Cancelada");

    private final String nombreVisible;

    EstadoReserva(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    /** Estados a los que se puede pasar desde este. */
    // POO - Colecciones: cada estado devuelve un arreglo con los estados siguientes permitidos.
    public EstadoReserva[] siguientesPermitidos() {
        return switch (this) {
            case PENDIENTE -> new EstadoReserva[]{CONFIRMADA, EN_CURSO, CANCELADA};
            case CONFIRMADA -> new EstadoReserva[]{EN_CURSO, CANCELADA};
            case EN_CURSO -> new EstadoReserva[]{FINALIZADA};
            case FINALIZADA, CANCELADA -> new EstadoReserva[0];
        };
    }

    public boolean puedeTransicionarA(EstadoReserva destino) {
        for (EstadoReserva siguiente : siguientesPermitidos()) {
            if (siguiente == destino) {
                return true;
            }
        }
        return false;
    }

    /** Un estado final ya no cambia. */
    public boolean esFinal() {
        return siguientesPermitidos().length == 0;
    }

    /** Toda reserva no cancelada ocupa la cancha en su horario. */
    public boolean ocupaCancha() {
        return this != CANCELADA;
    }

    public boolean permiteCancelar() {
        return puedeTransicionarA(CANCELADA);
    }

    /** Se pueden recibir pagos en cualquier estado excepto CANCELADA. */
    public boolean permitePagos() {
        return this != CANCELADA;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
