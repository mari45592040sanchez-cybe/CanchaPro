package canchapro.canchas;

import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de canchas.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin saber que es un modelo de canchas.
public class CanchaTableModel extends AbstractTableModel {

    public static final int COLUMNA_ESTADO = 5;
    private static final String[] COLUMNAS = {"Código", "Nombre", "Tipo", "Capacidad", "Horario", "Estado", "Descripción"};

    // POO - Colecciones: arreglo de objetos Cancha que se muestran en la tabla.
    private Cancha[] canchas = new Cancha[0];

    public void setCanchas(Cancha[] nuevas) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        canchas = new Cancha[nuevas.length];
        for (int i = 0; i < nuevas.length; i++) {
            canchas[i] = nuevas[i];
        }
        fireTableDataChanged();
    }

    public Cancha getCancha(int fila) {
        return canchas[fila];
    }

    public int indiceDe(String codigo) {
        for (int i = 0; i < canchas.length; i++) {
            if (canchas[i].getCodigo().equals(codigo)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return canchas.length;
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
        return switch (columna) {
            case 3 -> Integer.class;
            case COLUMNA_ESTADO -> EstadoCancha.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        Cancha c = canchas[fila];
        return switch (columna) {
            case 0 -> c.getCodigo();
            case 1 -> c.getNombre();
            case 2 -> c.getTipo().toString();
            case 3 -> c.getCapacidad();
            case 4 -> c.getHorario();
            case COLUMNA_ESTADO -> c.getEstado();
            case 6 -> c.getDescripcion();
            default -> "";
        };
    }
}
