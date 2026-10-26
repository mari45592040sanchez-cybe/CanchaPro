package canchapro.servicios;

import canchapro.util.FormatoMoneda;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de servicios adicionales.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin conocer la clase concreta.
public class ServicioAdicionalTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Código", "Nombre", "Descripción", "Precio", "Cobro", "Estado"};

    // POO - Colecciones: arreglo de objetos ServicioAdicional que se muestran en la tabla.
    private ServicioAdicional[] servicios = new ServicioAdicional[0];

    public void setServicios(ServicioAdicional[] nuevos) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        servicios = new ServicioAdicional[nuevos.length];
        for (int i = 0; i < nuevos.length; i++) {
            servicios[i] = nuevos[i];
        }
        fireTableDataChanged();
    }

    public ServicioAdicional getServicio(int fila) {
        return servicios[fila];
    }

    public int indiceDe(String codigo) {
        for (int i = 0; i < servicios.length; i++) {
            if (servicios[i].getCodigo().equals(codigo)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return servicios.length;
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
        ServicioAdicional s = servicios[fila];
        return switch (columna) {
            case 0 -> s.getCodigo();
            case 1 -> s.getNombre();
            case 2 -> s.getDescripcion();
            case 3 -> FormatoMoneda.formatear(s.getPrecio());
            case 4 -> s.getTipoCobro().toString();
            case 5 -> s.isActivo() ? "Activo" : "Inactivo";
            default -> "";
        };
    }
}
