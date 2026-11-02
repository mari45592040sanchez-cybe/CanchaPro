package canchapro.tienda;

import canchapro.util.Identificable;
import canchapro.util.excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una línea de una venta: producto, cantidad y precio al momento de vender.
 * Se identifica por el código de la venta y su número de línea (VTA-00001-1).
 */
// POO - Polimorfismo (interfaz): implementa Identificable. POO - Relaciones: TIENE el producto vendido.
// POO - Encapsulamiento: guarda el precio vigente al vender (no cambia si el producto sube de precio).
public final class DetalleVenta implements Identificable {

    private final String codigoVenta;
    private final int linea;
    private final Producto producto;
    private final int cantidad;
    private final BigDecimal precioUnitario;

    public DetalleVenta(String codigoVenta, int linea, Producto producto, int cantidad, BigDecimal precioUnitario) {
        this.codigoVenta = Objects.requireNonNull(codigoVenta);
        if (linea < 1) {
            throw new DatosInvalidosException("El número de línea debe ser mayor que cero.");
        }
        if (cantidad < 1) {
            throw new DatosInvalidosException("La cantidad debe ser mayor que cero.");
        }
        this.linea = linea;
        this.producto = Objects.requireNonNull(producto);
        this.cantidad = cantidad;
        this.precioUnitario = Objects.requireNonNull(precioUnitario);
    }

    @Override
    public String getCodigo() {
        return codigoVenta + "-" + linea;
    }

    public String getCodigoVenta() {
        return codigoVenta;
    }

    public int getLinea() {
        return linea;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }
}
