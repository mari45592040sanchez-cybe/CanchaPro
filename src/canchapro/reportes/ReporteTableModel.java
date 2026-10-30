package canchapro.reportes;

import javax.swing.table.AbstractTableModel;

/**
 * Muestra cualquier {@link ResultadoReporte} en una tabla: las columnas
 * dependen del reporte generado.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Reutilización: un solo modelo de tabla sirve para los 8 reportes.
public class ReporteTableModel extends AbstractTableModel {

    private ResultadoReporte resultado;

    public void setResultado(ResultadoReporte resultado) {
        this.resultado = resultado;
        fireTableStructureChanged();
    }

    @Override
    public int getRowCount() {
        return resultado == null ? 0 : resultado.getCantidadFilas();
    }

    @Override
    public int getColumnCount() {
        return resultado == null ? 0 : resultado.getCantidadColumnas();
    }

    @Override
    public String getColumnName(int columna) {
        return resultado.getColumna(columna);
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        return resultado.getValor(fila, columna);
    }
}
