package canchapro.tarifas;

import canchapro.util.FormatoMoneda;
import javax.swing.table.AbstractTableModel;

/**
 * Muestra los tramos de un cálculo de precio. Se reutilizará en Nueva Reserva.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin conocer la clase concreta.
public class TramoTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Desde", "Hasta", "Minutos", "Precio / hora", "Subtotal"};

    // POO - Colecciones: arreglo de objetos TramoTarifa que se muestran en la tabla.
    private TramoTarifa[] tramos = new TramoTarifa[0];

    public void setTramos(TramoTarifa[] nuevos) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        tramos = new TramoTarifa[nuevos.length];
        for (int i = 0; i < nuevos.length; i++) {
            tramos[i] = nuevos[i];
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return tramos.length;
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
        TramoTarifa t = tramos[fila];
        return switch (columna) {
            case 0 -> t.horaDesde().toString();
            case 1 -> t.horaHasta().toString();
            case 2 -> t.minutos();
            case 3 -> FormatoMoneda.formatear(t.precioHora());
            case 4 -> FormatoMoneda.formatear(t.subtotal());
            default -> "";
        };
    }
}
