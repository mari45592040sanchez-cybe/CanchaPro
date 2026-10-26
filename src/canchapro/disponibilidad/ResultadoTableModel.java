package canchapro.disponibilidad;

import canchapro.util.FormatoMoneda;
import javax.swing.table.AbstractTableModel;

/**
 * Resultado de una consulta de disponibilidad, una fila por cancha.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin conocer la clase concreta.
public class ResultadoTableModel extends AbstractTableModel {

    public static final int COLUMNA_DISPONIBLE = 3;
    private static final String[] COLUMNAS = {"Cancha", "Tipo", "Horario", "Disponibilidad", "Detalle", "Precio"};

    // POO - Colecciones: arreglo de objetos ResultadoDisponibilidad que se muestran en la tabla.
    private ResultadoDisponibilidad[] resultados = new ResultadoDisponibilidad[0];

    public void setResultados(ResultadoDisponibilidad[] nuevos) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        resultados = new ResultadoDisponibilidad[nuevos.length];
        for (int i = 0; i < nuevos.length; i++) {
            resultados[i] = nuevos[i];
        }
        fireTableDataChanged();
    }

    public ResultadoDisponibilidad getResultado(int fila) {
        return resultados[fila];
    }

    @Override
    public int getRowCount() {
        return resultados.length;
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
        return columna == COLUMNA_DISPONIBLE ? Boolean.class : String.class;
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        ResultadoDisponibilidad r = resultados[fila];
        return switch (columna) {
            case 0 -> r.cancha().getCodigo() + " - " + r.cancha().getNombre();
            case 1 -> r.cancha().getTipo().toString();
            case 2 -> r.cancha().getHorario();
            case COLUMNA_DISPONIBLE -> r.disponible();
            case 4 -> r.disponible() ? describirTramos(r) : r.motivo();
            case 5 -> r.disponible() ? FormatoMoneda.formatear(r.precio().total()) : "";
            default -> "";
        };
    }

    private static String describirTramos(ResultadoDisponibilidad r) {
        return r.precio().tieneVariosTramos()
                ? "Cruza " + r.precio().cantidadTramos() + " franjas tarifarias"
                : FormatoMoneda.formatear(r.precio().tramos()[0].precioHora()) + " por hora";
    }
}
