package canchapro.util;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

// POO - Reutilización: mensajes y diálogos comunes para todas las pantallas.
public final class Dialogos {

    private Dialogos() {
    }

    public static void mostrarTexto(Component padre, String titulo, String encabezado, String texto) {
        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setOpaque(false);
        JOptionPane.showMessageDialog(padre, new Object[]{new JLabel(encabezado), area}, titulo,
                JOptionPane.INFORMATION_MESSAGE);
    }
}
