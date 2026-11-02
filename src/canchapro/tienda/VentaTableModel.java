package canchapro.tienda;

import canchapro.util.FormatoFecha;
import canchapro.util.FormatoMoneda;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de ventas.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin conocer la clase concreta.
public class VentaTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Código", "Fecha y hora", "Cliente", "Artículos", "Total", "Método", "Estado"};

    // POO - Colecciones: arreglo de objetos Venta que se muestran en la tabla.
    private Venta[] ventas = new Venta[0];

    public void setVentas(Venta[] nuevas) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        ventas = new Venta[nuevas.length];
        for (int i = 0; i < nuevas.length; i++) {
            ventas[i] = nuevas[i];
        }
        fireTableDataChanged();
    }

    public Venta getVenta(int fila) {
        return ventas[fila];
    }

    @Override
    public int getRowCount() {
        return ventas.length;
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
        Venta v = ventas[fila];
        return switch (columna) {
            case 0 -> v.getCodigo();
            case 1 -> FormatoFecha.vista(v.getFechaHora());
            case 2 -> v.getCliente() == null ? "(mostrador)" : v.getCliente().getNombre();
            case 3 -> v.getCantidadArticulos();
            case 4 -> FormatoMoneda.formatear(v.getTotal());
            case 5 -> v.getMetodoPago().toString();
            case 6 -> v.getEstado().toString();
            default -> "";
        };
    }
}
