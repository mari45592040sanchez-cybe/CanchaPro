package canchapro.tienda;

import java.awt.Color;
import java.awt.Component;
import java.util.function.IntFunction;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Resalta en rojo claro las filas de productos con stock bajo.
 */
// POO - Herencia / Polimorfismo: extiende DefaultTableCellRenderer y sobrescribe cómo se dibuja la fila.
public class StockBajoRenderer extends DefaultTableCellRenderer {

    private static final Color FONDO_ALERTA = new Color(254, 226, 226);

    /** Devuelve el producto de una fila (en índice de la vista). */
    private final IntFunction<Producto> productoDeFila;

    public StockBajoRenderer(IntFunction<Producto> productoDeFila) {
        this.productoDeFila = productoDeFila;
    }

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionado,
            boolean conFoco, int fila, int columna) {
        super.getTableCellRendererComponent(tabla, valor, seleccionado, conFoco, fila, columna);
        if (!seleccionado) {
            Producto p = productoDeFila.apply(fila);
            setBackground(p.isActivo() && p.stockBajo() ? FONDO_ALERTA : tabla.getBackground());
        }
        setHorizontalAlignment(valor instanceof Integer ? RIGHT : LEFT);
        return this;
    }
}
