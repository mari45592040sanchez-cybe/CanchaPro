package canchapro.tienda;

import canchapro.util.FormatoMoneda;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de productos.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin conocer la clase concreta.
public class ProductoTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Código", "Nombre", "Categoría", "Precio", "Existencia", "Mínimo", "Estado"};

    // POO - Colecciones: arreglo de objetos Producto que se muestran en la tabla.
    private Producto[] productos = new Producto[0];

    public void setProductos(Producto[] nuevos) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        productos = new Producto[nuevos.length];
        for (int i = 0; i < nuevos.length; i++) {
            productos[i] = nuevos[i];
        }
        fireTableDataChanged();
    }

    public Producto getProducto(int fila) {
        return productos[fila];
    }

    public int indiceDe(String codigo) {
        for (int i = 0; i < productos.length; i++) {
            if (productos[i].getCodigo().equals(codigo)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return productos.length;
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    @Override
    public Class<?> getColumnClass(int columna) {
        return columna == 4 || columna == 5 ? Integer.class : String.class;
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        Producto p = productos[fila];
        return switch (columna) {
            case 0 -> p.getCodigo();
            case 1 -> p.getNombre();
            case 2 -> p.getCategoria().toString();
            case 3 -> FormatoMoneda.formatear(p.getPrecio());
            case 4 -> p.getExistencia();
            case 5 -> p.getStockMinimo();
            case 6 -> !p.isActivo() ? "Inactivo" : p.stockBajo() ? "Stock bajo" : "Activo";
            default -> "";
        };
    }
}
