package canchapro.reservas;

import canchapro.servicios.ServicioAdicional;
import canchapro.util.FormatoMoneda;
import javax.swing.table.AbstractTableModel;

/**
 * Tabla para elegir servicios adicionales: casilla "Agregar" y cantidad
 * editables.
 */
// POO - Herencia: extiende AbstractTableModel de Swing.
// POO - Polimorfismo: sobrescribe isCellEditable() y setValueAt() para que la tabla sea editable.
public class ServicioSeleccionTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Agregar", "Servicio", "Precio", "Cobro", "Cantidad"};
    private static final int COL_AGREGAR = 0;
    private static final int COL_CANTIDAD = 4;

    /** Fila editable de la tabla. */
    private static final class Fila {

        private final ServicioAdicional servicio;
        private boolean seleccionado;
        private int cantidad = 1;

        Fila(ServicioAdicional servicio) {
            this.servicio = servicio;
        }
    }

    // POO - Colecciones: arreglo de filas (cada fila guarda un servicio, si está marcado y su cantidad).
    private Fila[] filas = new Fila[0];

    public void setServicios(ServicioAdicional[] servicios) {
        filas = new Fila[servicios.length];
        for (int i = 0; i < servicios.length; i++) {
            filas[i] = new Fila(servicios[i]);
        }
        fireTableDataChanged();
    }

    public SolicitudServicio[] getSeleccionados() {
        int cantidad = 0;
        for (Fila f : filas) {
            if (f.seleccionado) {
                cantidad++;
            }
        }
        SolicitudServicio[] seleccionados = new SolicitudServicio[cantidad];
        int posicion = 0;
        for (Fila f : filas) {
            if (f.seleccionado) {
                seleccionados[posicion] = new SolicitudServicio(f.servicio, f.cantidad);
                posicion++;
            }
        }
        return seleccionados;
    }

    public boolean haySeleccionados() {
        for (Fila f : filas) {
            if (f.seleccionado) {
                return true;
            }
        }
        return false;
    }

    public void limpiarSeleccion() {
        for (Fila f : filas) {
            f.seleccionado = false;
            f.cantidad = 1;
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return filas.length;
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
            case COL_AGREGAR -> Boolean.class;
            case COL_CANTIDAD -> Integer.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return columna == COL_AGREGAR || columna == COL_CANTIDAD;
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        Fila f = filas[fila];
        return switch (columna) {
            case COL_AGREGAR -> f.seleccionado;
            case 1 -> f.servicio.getNombre();
            case 2 -> FormatoMoneda.formatear(f.servicio.getPrecio());
            case 3 -> f.servicio.getTipoCobro().toString();
            case COL_CANTIDAD -> f.cantidad;
            default -> "";
        };
    }

    @Override
    public void setValueAt(Object valor, int fila, int columna) {
        Fila f = filas[fila];
        if (columna == COL_AGREGAR) {
            f.seleccionado = Boolean.TRUE.equals(valor);
        } else if (columna == COL_CANTIDAD && valor instanceof Integer cantidad) {
            // Valores fuera de rango se ajustan al mínimo/máximo permitido.
            f.cantidad = Math.max(1, Math.min(ServicioAdicional.CANTIDAD_MAXIMA, cantidad));
            f.seleccionado = true;
        }
        fireTableRowsUpdated(fila, fila);
    }
}
