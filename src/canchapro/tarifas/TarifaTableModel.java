package canchapro.tarifas;

import canchapro.util.FormatoMoneda;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de tarifas.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin conocer la clase concreta.
public class TarifaTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Código", "Días", "Desde", "Hasta", "Tipo de cancha", "Precio / hora", "Estado"};

    // POO - Colecciones: arreglo de objetos Tarifa que se muestran en la tabla.
    private Tarifa[] tarifas = new Tarifa[0];

    public void setTarifas(Tarifa[] nuevas) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        tarifas = new Tarifa[nuevas.length];
        for (int i = 0; i < nuevas.length; i++) {
            tarifas[i] = nuevas[i];
        }
        fireTableDataChanged();
    }

    public Tarifa getTarifa(int fila) {
        return tarifas[fila];
    }

    public int indiceDe(String codigo) {
        for (int i = 0; i < tarifas.length; i++) {
            if (tarifas[i].getCodigo().equals(codigo)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return tarifas.length;
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
        Tarifa t = tarifas[fila];
        return switch (columna) {
            case 0 -> t.getCodigo();
            case 1 -> t.getTipoDia().toString();
            case 2 -> t.getHoraDesde().toString();
            case 3 -> t.getHoraHasta().toString();
            case 4 -> t.getAlcance();
            case 5 -> FormatoMoneda.formatear(t.getPrecioHora());
            case 6 -> t.isActiva() ? "Activa" : "Inactiva";
            default -> "";
        };
    }
}
