package canchapro.tienda;

// POO - Encapsulamiento: enumeración con su propio nombre visible.
public enum CategoriaProducto {

    BEBIDA("Bebida"),
    SNACK("Snack"),
    COMIDA("Comida"),
    ARTICULO_DEPORTIVO("Artículo deportivo"),
    OTRO("Otro");

    private final String nombreVisible;

    CategoriaProducto(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
