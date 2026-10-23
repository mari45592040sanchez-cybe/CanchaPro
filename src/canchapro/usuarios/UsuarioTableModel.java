package canchapro.usuarios;

import java.util.stream.Collectors;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de usuarios.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: la JTable llama a getRowCount(), getValueAt()... sin saber que es un modelo de usuarios.
public class UsuarioTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Código", "Nombre", "Usuario", "Rol", "Teléfono", "Estado", "Módulos"};

    // POO - Colecciones: arreglo de objetos Usuario que se muestran en la tabla.
    private Usuario[] usuarios = new Usuario[0];

    public void setUsuarios(Usuario[] nuevos) {
        // Se guarda una copia para que el arreglo de la tabla no cambie desde afuera.
        usuarios = new Usuario[nuevos.length];
        for (int i = 0; i < nuevos.length; i++) {
            usuarios[i] = nuevos[i];
        }
        fireTableDataChanged();
    }

    public Usuario getUsuario(int fila) {
        return usuarios[fila];
    }

    public int indiceDe(String codigo) {
        for (int i = 0; i < usuarios.length; i++) {
            if (usuarios[i].getCodigo().equals(codigo)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return usuarios.length;
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
        Usuario u = usuarios[fila];
        return switch (columna) {
            case 0 -> u.getCodigo();
            case 1 -> u.getNombre();
            case 2 -> u.getUsername();
            case 3 -> u.getRol().toString();
            case 4 -> u.getTelefono();
            case 5 -> u.isActivo() ? "Activo" : "Inactivo";
            case 6 -> describirModulos(u);
            default -> "";
        };
    }

    private static String describirModulos(Usuario u) {
        if (u instanceof Empleado empleado) {
            return empleado.getModulos().isEmpty()
                    ? "(ninguno)"
                    : empleado.getModulos().stream().map(Modulo::getNombreVisible).collect(Collectors.joining(", "));
        }
        return "Todos";
    }
}
