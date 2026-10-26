package canchapro.canchas;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Muestra el estado de la cancha con color: verde (disponible),
 * ámbar (mantenimiento) y gris (inactiva).
 */
// POO - Herencia / Polimorfismo: extiende DefaultTableCellRenderer y sobrescribe cómo se dibuja la celda.
public class EstadoCanchaRenderer extends DefaultTableCellRenderer {

    private static final Color VERDE = new Color(22, 101, 52);
    private static final Color AMBAR = new Color(180, 83, 9);
    private static final Color GRIS = new Color(107, 114, 128);

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
            boolean conFoco, int fila, int columna) {
        Component celda = super.getTableCellRendererComponent(tabla, valor, seleccionado, conFoco, fila, columna);
        setFont(getFont().deriveFont(Font.BOLD));
        if (!seleccionado && valor instanceof EstadoCancha estado) {
            setForeground(switch (estado) {
                case DISPONIBLE -> VERDE;
                case MANTENIMIENTO -> AMBAR;
                case INACTIVA -> GRIS;
            });
        }
        return celda;
    }
}
