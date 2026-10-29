package canchapro.dashboard;

import canchapro.principal.Refrescable;
import canchapro.reservas.EstadoReserva;
import canchapro.reservas.EstadoReservaRenderer;
import canchapro.reservas.FormatoReserva;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaTableModel;
import canchapro.usuarios.Modulo;
import canchapro.util.FormatoMoneda;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

/**
 * Pantalla de inicio: indicadores del día y reservas de hoy.
 */
// POO - Herencia: extiende JPanel. POO - Polimorfismo: implementa Refrescable (se actualiza cada minuto).
// POO - Relaciones: usa DashboardServicio para los cálculos; la pantalla solo los muestra.
public class PnlDashboard extends javax.swing.JPanel implements Refrescable {

    private static final Color VERDE = new Color(22, 101, 52);
    private static final Color AZUL = new Color(29, 78, 216);
    private static final Color AMBAR = new Color(180, 83, 9);
    private static final Color GRIS_OSCURO = new Color(55, 65, 81);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final DashboardServicio servicio;
    private final Consumer<Modulo> navegar;
    private final ReservaTableModel modeloReservas = new ReservaTableModel();
    private final Tarjeta tarjetaReservas = new Tarjeta("Reservas de hoy", GRIS_OSCURO);
    private final Tarjeta tarjetaEnCurso = new Tarjeta("En curso ahora", AZUL);
    private final Tarjeta tarjetaPendientes = new Tarjeta("Pendientes de confirmar", AMBAR);
    private final Tarjeta tarjetaIngresos = new Tarjeta("Ingresos de hoy", VERDE);
    private final Tarjeta tarjetaSaldo = new Tarjeta("Saldo por cobrar", AMBAR);
    private final Tarjeta tarjetaOcupacion = new Tarjeta("Ocupación de hoy", AZUL);
    /**
     * Tarjetas aportadas por módulos opcionales y su indicador. Son dos
     * arreglos paralelos: la tarjeta de la posición i muestra el indicador i.
     */
    // POO - Colecciones: arreglos paralelos de tarjetas e indicadores.
    private final Tarjeta[] tarjetasExtra;
    private final Indicador[] indicadoresExtra;

    /**
     * @param navegar abre un módulo de la ventana principal
     * @param puedeVer indica si el usuario tiene acceso a un módulo
     * @param extras indicadores de módulos opcionales (puede estar vacío)
     */
    public PnlDashboard(DashboardServicio servicio, Consumer<Modulo> navegar, Predicate<Modulo> puedeVer,
            Indicador[] extras) {
        this.servicio = servicio;
        this.navegar = navegar;
        initComponents();

        Tarjeta[] fijas = {tarjetaReservas, tarjetaEnCurso, tarjetaPendientes,
            tarjetaIngresos, tarjetaSaldo, tarjetaOcupacion};
        tarjetasExtra = new Tarjeta[extras.length];
        indicadoresExtra = new Indicador[extras.length];
        Tarjeta[] tarjetas = new Tarjeta[fijas.length + extras.length];
        for (int i = 0; i < fijas.length; i++) {
            tarjetas[i] = fijas[i];
        }
        for (int i = 0; i < extras.length; i++) {
            Tarjeta tarjeta = new Tarjeta(extras[i].titulo(), GRIS_OSCURO);
            tarjetasExtra[i] = tarjeta;
            indicadoresExtra[i] = extras[i];
            tarjetas[fijas.length + i] = tarjeta;
        }
        // Cuatro tarjetas por fila.
        for (int i = 0; i < tarjetas.length; i++) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = i % 4;
            gbc.gridy = i / 4;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(i / 4 == 0 ? 0 : 6, i % 4 == 0 ? 0 : 6, 0, 0);
            pnlTarjetas.add(tarjetas[i], gbc);
        }

        tblReservasHoy.setModel(modeloReservas);
        tblReservasHoy.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblReservasHoy.setDefaultRenderer(EstadoReserva.class, new EstadoReservaRenderer());

        // Los accesos rápidos solo aparecen si el usuario tiene el módulo.
        btnNuevaReserva.setVisible(puedeVer.test(Modulo.RESERVAS));
        btnDisponibilidad.setVisible(puedeVer.test(Modulo.DISPONIBILIDAD));
        btnPagos.setVisible(puedeVer.test(Modulo.PAGOS));
        pnlAccesos.setVisible(btnNuevaReserva.isVisible() || btnDisponibilidad.isVisible() || btnPagos.isVisible());

        refrescar();
    }

    @Override
    public void refrescar() {
        ResumenDashboard r = servicio.resumen();
        tarjetaReservas.setValor(String.valueOf(r.reservasHoy()));
        tarjetaEnCurso.setValor(String.valueOf(r.enCurso()));
        tarjetaPendientes.setValor(String.valueOf(r.pendientesConfirmar()));
        tarjetaIngresos.setValor(FormatoMoneda.formatear(r.ingresosHoy()));
        tarjetaSaldo.setValor(FormatoMoneda.formatear(r.saldoPorCobrar()));
        tarjetaOcupacion.setValor(r.porcentajeOcupacion() + " %");
        for (int i = 0; i < tarjetasExtra.length; i++) {
            tarjetasExtra[i].setValor(indicadoresExtra[i].valor().get());
        }

        Reserva[] deHoy = servicio.reservasDeHoy();
        modeloReservas.setReservas(deHoy);
        ((TitledBorder) pnlReservasHoy.getBorder()).setTitle(deHoy.length == 0
                ? "Reservas de hoy (no hay reservas)" : "Reservas de hoy (" + deHoy.length + ")");
        pnlReservasHoy.repaint();
        lblFecha.setText(FormatoReserva.fechaLarga(r.fecha()) + "  ·  actualizado a las " + LocalTime.now().format(HORA));
    }

    /** Recuadro con un indicador: título pequeño y valor grande. */
    private static final class Tarjeta extends JPanel {

        private final JLabel lblValor = new JLabel("-");

        Tarjeta(String titulo, Color color) {
            super(new BorderLayout(0, 4));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, color),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            JLabel lblTitulo = new JLabel(titulo);
            lblTitulo.setForeground(Color.GRAY);
            lblValor.setFont(lblValor.getFont().deriveFont(Font.BOLD, 24f));
            lblValor.setForeground(color);
            add(lblTitulo, BorderLayout.NORTH);
            add(lblValor, BorderLayout.CENTER);
        }

        void setValor(String valor) {
            lblValor.setText(valor);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        pnlNorte = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        pnlFecha = new javax.swing.JPanel();
        lblFecha = new javax.swing.JLabel();
        btnActualizar = new javax.swing.JButton();
        pnlCentro = new javax.swing.JPanel();
        pnlTarjetas = new javax.swing.JPanel();
        pnlReservasHoy = new javax.swing.JPanel();
        scrReservasHoy = new javax.swing.JScrollPane();
        tblReservasHoy = new javax.swing.JTable();
        pnlAccesos = new javax.swing.JPanel();
        lblAccesos = new javax.swing.JLabel();
        btnNuevaReserva = new javax.swing.JButton();
        btnDisponibilidad = new javax.swing.JButton();
        btnPagos = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        pnlNorte.setLayout(new java.awt.BorderLayout(12, 0));
        lblTitulo.setText("Inicio");
        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        pnlNorte.add(lblTitulo, java.awt.BorderLayout.WEST);

        pnlFecha.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        lblFecha.setText("-");
        lblFecha.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblFecha.setForeground(new java.awt.Color(128, 128, 128));
        pnlFecha.add(lblFecha);

        btnActualizar.setText("Actualizar");
        btnActualizar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActualizarActionPerformed(evt);
            }
        });
        pnlFecha.add(btnActualizar);

        pnlNorte.add(pnlFecha, java.awt.BorderLayout.EAST);

        add(pnlNorte, java.awt.BorderLayout.NORTH);

        pnlCentro.setLayout(new java.awt.BorderLayout(0, 12));
        pnlTarjetas.setLayout(new java.awt.GridBagLayout());
        pnlCentro.add(pnlTarjetas, java.awt.BorderLayout.NORTH);

        pnlReservasHoy.setBorder(javax.swing.BorderFactory.createTitledBorder("Reservas de hoy"));
        pnlReservasHoy.setLayout(new java.awt.BorderLayout());
        tblReservasHoy.setRowHeight(24);
        scrReservasHoy.setViewportView(tblReservasHoy);

        pnlReservasHoy.add(scrReservasHoy, java.awt.BorderLayout.CENTER);

        pnlCentro.add(pnlReservasHoy, java.awt.BorderLayout.CENTER);

        add(pnlCentro, java.awt.BorderLayout.CENTER);

        pnlAccesos.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        lblAccesos.setText("Accesos rápidos:");
        lblAccesos.setForeground(new java.awt.Color(128, 128, 128));
        pnlAccesos.add(lblAccesos);

        btnNuevaReserva.setText("Nueva reserva");
        btnNuevaReserva.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevaReservaActionPerformed(evt);
            }
        });
        pnlAccesos.add(btnNuevaReserva);

        btnDisponibilidad.setText("Consultar disponibilidad");
        btnDisponibilidad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDisponibilidadActionPerformed(evt);
            }
        });
        pnlAccesos.add(btnDisponibilidad);

        btnPagos.setText("Registrar pago");
        btnPagos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPagosActionPerformed(evt);
            }
        });
        pnlAccesos.add(btnPagos);

        add(pnlAccesos, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnActualizarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActualizarActionPerformed
        refrescar();
    }//GEN-LAST:event_btnActualizarActionPerformed

    private void btnNuevaReservaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevaReservaActionPerformed
        navegar.accept(Modulo.RESERVAS);
    }//GEN-LAST:event_btnNuevaReservaActionPerformed

    private void btnDisponibilidadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDisponibilidadActionPerformed
        navegar.accept(Modulo.DISPONIBILIDAD);
    }//GEN-LAST:event_btnDisponibilidadActionPerformed

    private void btnPagosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPagosActionPerformed
        navegar.accept(Modulo.PAGOS);
    }//GEN-LAST:event_btnPagosActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnActualizar;
    private javax.swing.JButton btnDisponibilidad;
    private javax.swing.JButton btnNuevaReserva;
    private javax.swing.JButton btnPagos;
    private javax.swing.JLabel lblAccesos;
    private javax.swing.JLabel lblFecha;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel pnlAccesos;
    private javax.swing.JPanel pnlCentro;
    private javax.swing.JPanel pnlFecha;
    private javax.swing.JPanel pnlNorte;
    private javax.swing.JPanel pnlReservasHoy;
    private javax.swing.JPanel pnlTarjetas;
    private javax.swing.JScrollPane scrReservasHoy;
    private javax.swing.JTable tblReservasHoy;
    // End of variables declaration//GEN-END:variables
}
