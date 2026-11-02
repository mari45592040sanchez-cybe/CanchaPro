package canchapro.tienda;

import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Líneas de venta guardadas en detalle_ventas.csv:
 * codigoVenta;linea;codigoProducto;cantidad;precioUnitario
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv; aquí solo se define el formato de detalle_ventas.csv.
// POO - Polimorfismo: los métodos con @Override implementan los métodos abstractos de RepositorioCsv.
public class DetalleVentaRepositorio extends RepositorioCsv<DetalleVenta> {

    public static final String ARCHIVO = "detalle_ventas.csv";

    private final Predicate<String> existeVenta;
    private final ProductoRepositorio productos;
    private Runnable alCargar = () -> { };

    public DetalleVentaRepositorio(Path carpetaDatos, Predicate<String> existeVenta, ProductoRepositorio productos) {
        super(carpetaDatos.resolve(ARCHIVO));
        this.existeVenta = existeVenta;
        this.productos = productos;
    }

    /** Acción a ejecutar tras cada lectura (asociar las líneas a sus ventas). */
    public void setAlCargar(Runnable alCargar) {
        this.alCargar = alCargar;
    }

    @Override
    protected void despuesDeCargar() {
        alCargar.run();
    }

    // --- Formato del archivo ---

    @Override
    protected String encabezado() {
        return "codigoVenta;linea;codigoProducto;cantidad;precioUnitario";
    }

    @Override
    protected int cantidadCampos() {
        return 5;
    }

    @Override
    protected DetalleVenta desdeCsv(String[] c) {
        if (!existeVenta.test(c[0].trim())) {
            throw new EntidadNoEncontradaException("la línea corresponde a la venta inexistente " + c[0]);
        }
        Producto producto = productos.buscar(c[2].trim())
                .orElseThrow(() -> new EntidadNoEncontradaException("producto inexistente " + c[2]));
        return new DetalleVenta(c[0].trim(), CamposCsv.entero(c[1], "linea"), producto,
                CamposCsv.entero(c[3], "cantidad"), CamposCsv.decimal(c[4], "precioUnitario"));
    }

    @Override
    protected String aCsv(DetalleVenta d) {
        return String.join(";", d.getCodigoVenta(), String.valueOf(d.getLinea()), d.getProducto().getCodigo(),
                String.valueOf(d.getCantidad()), d.getPrecioUnitario().toPlainString());
    }

    @Override
    protected DetalleVenta[] crearArreglo(int tamanio) {
        return new DetalleVenta[tamanio];
    }
}
