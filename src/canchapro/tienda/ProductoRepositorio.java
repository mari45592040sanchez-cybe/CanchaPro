package canchapro.tienda;

import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import java.nio.file.Path;

/**
 * Productos guardados en productos.csv:
 * codigo;nombre;categoria;precio;existencia;stockMinimo;activo
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv; aquí solo se define el formato de productos.csv.
// POO - Polimorfismo: los métodos con @Override implementan los métodos abstractos de RepositorioCsv.
public class ProductoRepositorio extends RepositorioCsv<Producto> {

    public static final String ARCHIVO = "productos.csv";

    public ProductoRepositorio(Path carpetaDatos) {
        super(carpetaDatos.resolve(ARCHIVO));
    }

    // --- Formato del archivo ---

    @Override
    protected String encabezado() {
        return "codigo;nombre;categoria;precio;existencia;stockMinimo;activo";
    }

    @Override
    protected int cantidadCampos() {
        return 7;
    }

    @Override
    protected Producto desdeCsv(String[] c) {
        return new Producto(c[0], c[1],
                CamposCsv.enumeracion(CategoriaProducto.class, c[2], "categoria"),
                CamposCsv.decimal(c[3], "precio"),
                CamposCsv.entero(c[4], "existencia"),
                CamposCsv.entero(c[5], "stockMinimo"),
                CamposCsv.booleano(c[6], "activo"));
    }

    @Override
    protected String aCsv(Producto p) {
        return String.join(";",
                p.getCodigo(), p.getNombre(), p.getCategoria().name(), p.getPrecio().toPlainString(),
                String.valueOf(p.getExistencia()), String.valueOf(p.getStockMinimo()), String.valueOf(p.isActivo()));
    }

    @Override
    protected Producto[] crearArreglo(int tamanio) {
        return new Producto[tamanio];
    }
}
