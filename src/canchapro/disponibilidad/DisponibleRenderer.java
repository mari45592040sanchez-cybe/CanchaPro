package canchapro.disponibilidad;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Muestra "Disponible" en verde o "No disponible" en rojo.
 */
// POO - Herencia / Polimorfismo: extiende DefaultTableCellRenderer y sobrescribe cómo se dibuja la celda.
public class DisponibleRenderer extends DefaultTableCellRenderer {

    private static final Color VERDE = new Color(22, 101, 52);
    private static final Color ROJO = new Color(185, 28, 28);

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
            boolean conFoco, int fila, int columna) {
        boolean disponible = Boolean.TRUE.equals(valor);
        super.getTableCellRendererComponent(tabla, disponible ? "Disponible" : "No disponible",
                seleccionado, conFoco, fila, columna);
        setFont(getFont().deriveFont(Font.BOLD));
        if (!seleccionado) {
            setForeground(disponible ? VERDE : ROJO);
        }
        return this;
    }
}
