package canchapro.disponibilidad;

import canchapro.canchas.Cancha;
import javax.swing.table.AbstractTableModel;

/**
 * Adapta una {@link GrillaOcupacion} a una tabla: la primera columna es la
 * hora y cada columna siguiente es una cancha.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Relaciones: el modelo TIENE una grilla de ocupación y la adapta para mostrarla en la tabla.
public class GrillaTableModel extends AbstractTableModel {

    private GrillaOcupacion grilla;

    public void setGrilla(GrillaOcupacion grilla) {
        this.grilla = grilla;
        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return grilla == null ? 0 : grilla.cantidadHoras();
    }

    @Override
    public int getColumnCount() {
        return grilla == null ? 1 : grilla.cantidadCanchas() + 1;
    }

    @Override
    public String getColumnName(int columna) {
        if (columna == 0) {
            return "Hora";
        }
        Cancha c = grilla.getCancha(columna - 1);
        return c.getCodigo() + " " + c.getNombre();
    }

    @Override
    public Class<?> getColumnClass(int columna) {
        return columna == 0 ? String.class : GrillaOcupacion.Celda.class;
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        if (columna == 0) {
            return grilla.getHora(fila).toString();
        }
        return grilla.getCelda(fila, columna - 1);
    }
}
