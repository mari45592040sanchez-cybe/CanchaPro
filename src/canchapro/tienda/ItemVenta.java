package canchapro.tienda;

/**
 * Producto y cantidad que se quieren vender (una línea del carrito).
 *
 * @param producto producto
 * @param cantidad unidades
 */
// POO - Encapsulamiento: record inmutable (producto + cantidad del carrito).
public record ItemVenta(Producto producto, int cantidad) {
}
