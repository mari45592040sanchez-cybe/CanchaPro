package canchapro.tienda;

import canchapro.util.FormatoMoneda;
import canchapro.util.Identificable;
import canchapro.util.Validador;
import canchapro.util.excepciones.DatosInvalidosException;
import canchapro.util.excepciones.StockInsuficienteException;
import java.math.BigDecimal;

/**
 * Producto de la tienda/cafetería con su inventario.
 * La existencia solo cambia al vender (descontar) o al reponer.
 */
// POO - Polimorfismo (interfaz): implementa Identificable para poder guardarse en un RepositorioCsv.
// POO - Encapsulamiento: la existencia solo cambia con descontar() y reponer(), que validan.
public class Producto implements Identificable {

    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("5000");
    private static final int STOCK_MAXIMO = 100_000;

    private final String codigo;
    private String nombre;
    private CategoriaProducto categoria;
    private BigDecimal precio;
    private int existencia;
    private int stockMinimo;
    private boolean activo;

    public Producto(String codigo, String nombre, CategoriaProducto categoria, BigDecimal precio,
            int existencia, int stockMinimo, boolean activo) {
        this.codigo = Validador.requerido(codigo, "El código");
        actualizarDatos(nombre, categoria, precio, stockMinimo);
        if (existencia < 0 || existencia > STOCK_MAXIMO) {
            throw new DatosInvalidosException("La existencia debe estar entre 0 y " + STOCK_MAXIMO + ".");
        }
        this.existencia = existencia;
        this.activo = activo;
    }

    /** Valida todos los datos y solo entonces los asigna. La existencia no se edita aquí. */
    public final void actualizarDatos(String nombre, CategoriaProducto categoria, BigDecimal precio, int stockMinimo) {
        String nombreValido = Validador.texto(nombre, 3, 40, "El nombre del producto");
        if (categoria == null) {
            throw new DatosInvalidosException("Seleccione la categoría.");
        }
        BigDecimal precioValido = Validador.precio(precio, PRECIO_MAXIMO, "El precio");
        if (stockMinimo < 0 || stockMinimo > STOCK_MAXIMO) {
            throw new DatosInvalidosException("El stock mínimo debe estar entre 0 y " + STOCK_MAXIMO + ".");
        }
        this.nombre = nombreValido;
        this.categoria = categoria;
        this.precio = precioValido;
        this.stockMinimo = stockMinimo;
    }

    /** Hay pocas unidades: la existencia llegó al stock mínimo. */
    public boolean stockBajo() {
        return existencia <= stockMinimo;
    }

    /**
     * Verifica que haya suficientes unidades, sin modificar la existencia.
     *
     * @throws StockInsuficienteException si no alcanza
     */
    public void verificarStock(int cantidad) {
        if (cantidad > existencia) {
            throw new StockInsuficienteException("Stock insuficiente de " + nombre + ": hay " + existencia
                    + " y se solicitan " + cantidad + ".");
        }
    }

    public void descontar(int cantidad) {
        if (cantidad <= 0) {
            throw new DatosInvalidosException("La cantidad a vender debe ser mayor que cero.");
        }
        verificarStock(cantidad);
        existencia -= cantidad;
    }

    public void reponer(int cantidad) {
        if (cantidad <= 0) {
            throw new DatosInvalidosException("La cantidad a reponer debe ser mayor que cero.");
        }
        if (existencia + cantidad > STOCK_MAXIMO) {
            throw new DatosInvalidosException("La existencia no puede superar " + STOCK_MAXIMO + " unidades.");
        }
        existencia += cantidad;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public CategoriaProducto getCategoria() {
        return categoria;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public int getExistencia() {
        return existencia;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        return nombre + " (" + FormatoMoneda.formatear(precio) + ")";
    }
}
