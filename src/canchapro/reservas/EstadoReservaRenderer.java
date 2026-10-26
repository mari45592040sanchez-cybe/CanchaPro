package canchapro.reservas;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Muestra el estado de la reserva con color.
 */
// POO - Herencia / Polimorfismo: extiende DefaultTableCellRenderer y sobrescribe cómo se dibuja la celda.
public class EstadoReservaRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
            boolean conFoco, int fila, int columna) {
        super.getTableCellRendererComponent(tabla, valor, seleccionado, conFoco, fila, columna);
        setFont(getFont().deriveFont(Font.BOLD));
        if (!seleccionado && valor instanceof EstadoReserva estado) {
            setForeground(color(estado));
        }
        return this;
    }

    public static Color color(EstadoReserva estado) {
        return switch (estado) {
            case PENDIENTE -> new Color(180, 83, 9);
            case CONFIRMADA -> new Color(22, 101, 52);
            case EN_CURSO -> new Color(29, 78, 216);
            case FINALIZADA -> new Color(75, 85, 99);
            case CANCELADA -> new Color(185, 28, 28);
        };
    }
}
