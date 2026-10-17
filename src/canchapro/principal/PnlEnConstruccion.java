package canchapro.principal;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Panel informativo para módulos que aún no se han implementado o cuando
 * el usuario no tiene módulos asignados.
 */
public class PnlEnConstruccion extends JPanel {

    public PnlEnConstruccion(String titulo, String mensaje) {
        super(new GridBagLayout());

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(lblTitulo.getFont().deriveFont(Font.BOLD, 22f));

        JLabel lblMensaje = new JLabel(mensaje);
        lblMensaje.setForeground(Color.GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(lblTitulo, gbc);
        gbc.gridy = 1;
        add(lblMensaje, gbc);
    }
}
