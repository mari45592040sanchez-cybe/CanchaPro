package canchapro.pagos;

import canchapro.util.FormatoFecha;
import canchapro.util.FormatoMoneda;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de pagos.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin conocer la clase concreta.
public class PagoTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Código", "Fecha y hora", "Monto", "Método", "Observación", "Recibió"};

    // POO - Colecciones: arreglo de objetos Pago que se muestran en la tabla.
    private Pago[] pagos = new Pago[0];

    public void setPagos(Pago[] nuevos) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        pagos = new Pago[nuevos.length];
        for (int i = 0; i < nuevos.length; i++) {
            pagos[i] = nuevos[i];
        }
        fireTableDataChanged();
    }

    public Pago getPago(int fila) {
        return pagos[fila];
    }

    @Override
    public int getRowCount() {
        return pagos.length;
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
        Pago p = pagos[fila];
        return switch (columna) {
            case 0 -> p.getCodigo();
            case 1 -> FormatoFecha.vista(p.getFechaHora());
            case 2 -> FormatoMoneda.formatear(p.getMonto());
            case 3 -> p.getMetodo().toString();
            case 4 -> p.getObservacion();
            case 5 -> p.getUsuario().getNombre();
            default -> "";
        };
    }
}
