package canchapro.usuarios;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de la tabla de usuarios.
 */
public class UsuarioTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Código", "Nombre", "Usuario", "Rol", "Teléfono", "Estado", "Módulos"};

    private final List<Usuario> usuarios = new ArrayList<>();

    public void setUsuarios(List<Usuario> nuevos) {
        usuarios.clear();
        usuarios.addAll(nuevos);
        fireTableDataChanged();
    }

    public Usuario getUsuario(int fila) {
        return usuarios.get(fila);
    }

    public int indiceDe(String codigo) {
        for (int i = 0; i < usuarios.size(); i++) {
            if (usuarios.get(i).getCodigo().equals(codigo)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return usuarios.size();
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
        Usuario u = usuarios.get(fila);
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
