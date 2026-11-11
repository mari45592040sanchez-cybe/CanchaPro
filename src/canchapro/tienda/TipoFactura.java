package canchapro.tienda;

// POO - Encapsulamiento: enumeración con los dos tipos de factura de una venta de la tienda.
// INDIVIDUAL: la venta se cobra al momento. UNIFICADA: la venta se carga a la cuenta de una reserva.
public enum TipoFactura {

    INDIVIDUAL("Factura individual"),
    UNIFICADA("Factura unificada");

    // POO - Atributos: texto que se muestra en pantalla.
    private final String nombreVisible;

    // POO - Constructor de la enumeración.
    TipoFactura(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    // POO - Polimorfismo: sobrescribe toString() de Object.
    @Override
    public String toString() {
        return nombreVisible;
    }
}
