package canchapro.reservas;

import canchapro.canchas.Cancha;
import canchapro.disponibilidad.SolicitudDisponibilidad;
import canchapro.principal.Refrescable;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

// POO - Herencia: extiende JPanel. POO - Polimorfismo: implementa Refrescable.
// POO - Relaciones (composición): TIENE las pestañas Nueva reserva y Reservas.
public class PnlModuloReservas extends JPanel implements Refrescable {

    private final JTabbedPane pestanas = new JTabbedPane();
    private final PnlNuevaReserva pnlNuevaReserva;
    private final PnlReservas pnlReservas;

    public PnlModuloReservas(PnlNuevaReserva pnlNuevaReserva, PnlReservas pnlReservas) {
        super(new BorderLayout());
        this.pnlNuevaReserva = pnlNuevaReserva;
        this.pnlReservas = pnlReservas;
        pestanas.addTab("Nueva reserva", pnlNuevaReserva);
        pestanas.addTab("Reservas", pnlReservas);
        add(pestanas, BorderLayout.CENTER);

        pnlNuevaReserva.setAlCrearReserva(reserva -> {
            pnlReservas.mostrarReserva(reserva);
            pestanas.setSelectedComponent(pnlReservas);
        });
    }

    public void mostrarNuevaReserva() {
        pestanas.setSelectedComponent(pnlNuevaReserva);
    }

    public void nuevaReserva(SolicitudDisponibilidad solicitud, Cancha cancha) {
        pestanas.setSelectedComponent(pnlNuevaReserva);
        pnlNuevaReserva.precargar(solicitud, cancha);
    }

    @Override
    public void refrescar() {
        pnlNuevaReserva.refrescar();
        pnlReservas.refrescar();
    }
}
