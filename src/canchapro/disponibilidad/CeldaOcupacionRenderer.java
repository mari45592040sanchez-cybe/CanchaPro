package canchapro.disponibilidad;

import canchapro.disponibilidad.GrillaOcupacion.Celda;
import canchapro.disponibilidad.GrillaOcupacion.TipoCelda;
import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Dibuja cada bloque de la grilla de ocupación con el color de su estado.
 * Los bloques del horario consultado se enmarcan en azul.
 */
// POO - Herencia / Polimorfismo: extiende DefaultTableCellRenderer y sobrescribe cómo se dibuja cada celda.
public class CeldaOcupacionRenderer extends DefaultTableCellRenderer {

    /** Color de fondo de cada tipo de celda (también se usa en la leyenda). */
    public static Color colorDe(TipoCelda tipo) {
        return switch (tipo) {
            case LIBRE -> new Color(220, 252, 231);
            case OCUPADA -> new Color(254, 202, 202);
            case CERRADA -> new Color(229, 231, 235);
            case NO_DISPONIBLE -> new Color(253, 230, 138);
            case PASADA -> new Color(243, 244, 246);
        };
    }

    private static final Border MARCO_SOLICITADO = BorderFactory.createLineBorder(new Color(37, 99, 235), 2);

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
            boolean conFoco, int fila, int columna) {
        super.getTableCellRendererComponent(tabla, "", false, false, fila, columna);
        if (valor instanceof Celda celda) {
            setText(celda.texto());
            setBackground(colorDe(celda.tipo()));
            setForeground(celda.tipo() == TipoCelda.OCUPADA ? new Color(127, 29, 29) : Color.DARK_GRAY);
            setToolTipText(celda.tipo() + (celda.texto().isBlank() ? "" : ": " + celda.texto()));
            if (celda.solicitada()) {
                setBorder(MARCO_SOLICITADO);
            }
        }
        return this;
    }
}
