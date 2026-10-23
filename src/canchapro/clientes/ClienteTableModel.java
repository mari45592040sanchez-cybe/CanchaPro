package canchapro.clientes;

import canchapro.util.FormatoFecha;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de clientes.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin saber que es un modelo de clientes.
public class ClienteTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Código", "Nombre", "Teléfono", "Correo", "Registro", "Estado"};

    // POO - Colecciones: arreglo de objetos Cliente que se muestran en la tabla.
    private Cliente[] clientes = new Cliente[0];

    public void setClientes(Cliente[] nuevos) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        clientes = new Cliente[nuevos.length];
        for (int i = 0; i < nuevos.length; i++) {
            clientes[i] = nuevos[i];
        }
        fireTableDataChanged();
    }

    public Cliente getCliente(int fila) {
        return clientes[fila];
    }

    public int indiceDe(String codigo) {
        for (int i = 0; i < clientes.length; i++) {
            if (clientes[i].getCodigo().equals(codigo)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return clientes.length;
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
        Cliente c = clientes[fila];
        return switch (columna) {
            case 0 -> c.getCodigo();
            case 1 -> c.getNombre();
            case 2 -> c.getTelefono();
            case 3 -> c.getCorreo();
            case 4 -> FormatoFecha.vista(c.getFechaRegistro());
            case 5 -> c.isActivo() ? "Activo" : "Inactivo";
            default -> "";
        };
    }
}
