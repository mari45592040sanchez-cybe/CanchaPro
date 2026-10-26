package canchapro.pagos;

// POO - Encapsulamiento: enumeración con su propio nombre visible.
public enum MetodoPago {

    EFECTIVO("Efectivo"),
    TARJETA("Tarjeta"),
    TRANSFERENCIA("Transferencia"),
    DEPOSITO("Depósito");

    private final String nombreVisible;

    MetodoPago(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
