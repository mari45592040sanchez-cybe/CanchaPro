package canchapro.tienda;

import canchapro.util.FormatoMoneda;
import java.math.BigDecimal;
import java.util.Arrays;
import javax.swing.table.AbstractTableModel;

/**
 * Carrito de la venta en curso. Si se agrega un producto que ya está, se
 * suma la cantidad.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin conocer la clase concreta.
public class CarritoTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Producto", "Cant.", "Precio", "Subtotal"};

    // POO - Colecciones: arreglo de items del carrito + contador; si se llena, se agranda al doble.
    private ItemVenta[] items = new ItemVenta[10];
    private int cantidad;

    /** Cantidad que ya hay de un producto en el carrito. */
    public int cantidadDe(Producto producto) {
        int total = 0;
        for (int i = 0; i < cantidad; i++) {
            if (items[i].producto().getCodigo().equals(producto.getCodigo())) {
                total += items[i].cantidad();
            }
        }
        return total;
    }

    public void agregar(Producto producto, int unidades) {
        for (int i = 0; i < cantidad; i++) {
            if (items[i].producto().getCodigo().equals(producto.getCodigo())) {
                items[i] = new ItemVenta(producto, items[i].cantidad() + unidades);
                fireTableRowsUpdated(i, i);
                return;
            }
        }
        if (cantidad == items.length) {
            items = Arrays.copyOf(items, items.length * 2);
        }
        items[cantidad] = new ItemVenta(producto, unidades);
        cantidad++;
        fireTableRowsInserted(cantidad - 1, cantidad - 1);
    }

    /** Quita la fila y corre una posición hacia arriba los items siguientes. */
    public void quitar(int fila) {
        for (int i = fila; i < cantidad - 1; i++) {
            items[i] = items[i + 1];
        }
        cantidad--;
        items[cantidad] = null;
        fireTableRowsDeleted(fila, fila);
    }

    public void vaciar() {
        items = new ItemVenta[10];
        cantidad = 0;
        fireTableDataChanged();
    }

    /** Copia de los items, del tamaño exacto. */
    public ItemVenta[] getItems() {
        return Arrays.copyOf(items, cantidad);
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cantidad; i++) {
            total = total.add(items[i].producto().getPrecio().multiply(BigDecimal.valueOf(items[i].cantidad())));
        }
        return total;
    }

    @Override
    public int getRowCount() {
        return cantidad;
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
    public Object getValueAt(int fila, int columna) {
        ItemVenta i = items[fila];
        return switch (columna) {
            case 0 -> i.producto().getNombre();
            case 1 -> i.cantidad();
            case 2 -> FormatoMoneda.formatear(i.producto().getPrecio());
            case 3 -> FormatoMoneda.formatear(i.producto().getPrecio().multiply(BigDecimal.valueOf(i.cantidad())));
            default -> "";
        };
    }
}
