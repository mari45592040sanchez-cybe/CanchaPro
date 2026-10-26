package canchapro.reservas;

import canchapro.util.FormatoFecha;
import canchapro.util.FormatoMoneda;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de reservas.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin conocer la clase concreta.
public class ReservaTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Código", "Fecha", "Horario", "Cancha", "Cliente", "Total", "Pagado", "Saldo", "Estado"};

    // POO - Colecciones: arreglo de objetos Reserva que se muestran en la tabla.
    private Reserva[] reservas = new Reserva[0];

    public void setReservas(Reserva[] nuevas) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        reservas = new Reserva[nuevas.length];
        for (int i = 0; i < nuevas.length; i++) {
            reservas[i] = nuevas[i];
        }
        fireTableDataChanged();
    }

    public Reserva getReserva(int fila) {
        return reservas[fila];
    }

    public int indiceDe(String codigo) {
        for (int i = 0; i < reservas.length; i++) {
            if (reservas[i].getCodigo().equals(codigo)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return reservas.length;
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
        return columna == 8 ? EstadoReserva.class : String.class;
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        Reserva r = reservas[fila];
        return switch (columna) {
            case 0 -> r.getCodigo();
            case 1 -> FormatoFecha.vista(r.getFecha());
            case 2 -> r.getHoraInicio() + " - " + r.getHoraFin();
            case 3 -> r.getCancha().getCodigo() + " " + r.getCancha().getNombre();
            case 4 -> r.getCliente().getNombre();
            case 5 -> FormatoMoneda.formatear(r.getTotal());
            case 6 -> FormatoMoneda.formatear(r.getTotalPagado());
            case 7 -> FormatoMoneda.formatear(r.getSaldo());
            case 8 -> r.getEstado();
            default -> "";
        };
    }
}
