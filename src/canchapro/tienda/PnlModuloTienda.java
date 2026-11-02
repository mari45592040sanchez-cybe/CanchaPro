package canchapro.tienda;

import canchapro.principal.Refrescable;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Módulo Tienda / Cafetería: pestañas "Punto de venta" y "Productos".
 * Solo contiene las dos pantallas diseñadas, por eso no tiene formulario propio.
 */
// POO - Herencia: extiende JPanel. POO - Relaciones (composición): TIENE las pestañas Punto de venta y Productos.
public class PnlModuloTienda extends JPanel implements Refrescable {

    private final PnlVentas pnlVentas;
    private final PnlProductos pnlProductos;

    public PnlModuloTienda(PnlVentas pnlVentas, PnlProductos pnlProductos) {
        super(new BorderLayout());
        this.pnlVentas = pnlVentas;
        this.pnlProductos = pnlProductos;
        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Punto de venta", pnlVentas);
        pestanas.addTab("Productos", pnlProductos);
        // Al cambiar de pestaña se actualiza la existencia mostrada.
        pestanas.addChangeListener(e -> refrescar());
        add(pestanas, BorderLayout.CENTER);
    }

    @Override
    public void refrescar() {
        pnlVentas.refrescar();
        pnlProductos.refrescar();
    }
}
