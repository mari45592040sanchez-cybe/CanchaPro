package canchapro.reservas;

import canchapro.canchas.Cancha;
import canchapro.canchas.CanchaServicio;
import canchapro.configuracion.Configuracion;
import canchapro.login.Sesion;
import canchapro.pagos.DlgRegistrarPago;
import canchapro.principal.Refrescable;
import canchapro.util.FormatoMoneda;
import canchapro.util.OpcionFiltro;
import canchapro.util.SelectorFecha;
import canchapro.util.excepciones.CanchaProException;
import canchapro.util.excepciones.DatosInvalidosException;
import java.time.LocalDate;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * Listado de reservas con filtros, detalle, cancelación y registro de pagos.
 */
// POO - Herencia: extiende JPanel. POO - Polimorfismo: implementa Refrescable.
public class PnlReservas extends javax.swing.JPanel implements Refrescable {

    private final ReservaServicio servicio;
    private final CanchaServicio canchaServicio;
    private final Sesion sesion;
    private final Configuracion configuracion;
    private final ReservaTableModel modeloTabla = new ReservaTableModel();

    public PnlReservas(ReservaServicio servicio, CanchaServicio canchaServicio, Configuracion configuracion,
            Sesion sesion) {
        this.servicio = servicio;
        this.canchaServicio = canchaServicio;
        this.configuracion = configuracion;
        this.sesion = sesion;
        initComponents();

        SelectorFecha.configurar(spnDesde, LocalDate.now());
        SelectorFecha.configurar(spnHasta, LocalDate.now().plusDays(7));
        cargarCanchas();
        cboEstado.setModel(new DefaultComboBoxModel<>(
                OpcionFiltro.arreglo("Todos", EstadoReserva.values(), EstadoReserva::toString)));

        tblReservas.setModel(modeloTabla);
        tblReservas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblReservas.setDefaultRenderer(EstadoReserva.class, new EstadoReservaRenderer());
        tblReservas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarDetalle();
            }
        });
        txtBuscar.addActionListener(e -> buscar());

        buscar();
    }

    private void cargarCanchas() {
        Cancha[] canchas = canchaServicio.listar();
        cboCancha.setModel(new DefaultComboBoxModel<>(
                OpcionFiltro.arreglo("Todas", canchas, c -> c.getCodigo() + " " + c.getNombre())));
    }

    @Override
    public void refrescar() {
        buscar();
    }

    /** Muestra una reserva concreta (por ejemplo, la recién creada). */
    public void mostrarReserva(Reserva reserva) {
        SelectorFecha.establecer(spnDesde, reserva.getFecha());
        SelectorFecha.establecer(spnHasta, reserva.getFecha());
        cboCancha.setSelectedIndex(0);
        cboEstado.setSelectedIndex(0);
        txtBuscar.setText("");
        buscar();
        seleccionar(reserva.getCodigo());
    }

    @SuppressWarnings("unchecked")
    private void buscar() {
        try {
            LocalDate desde = SelectorFecha.obtener(spnDesde);
            LocalDate hasta = SelectorFecha.obtener(spnHasta);
            if (hasta.isBefore(desde)) {
                throw new DatosInvalidosException("La fecha final no puede ser anterior a la inicial.");
            }
            Cancha cancha = ((OpcionFiltro<Cancha>) cboCancha.getSelectedItem()).valor();
            EstadoReserva estado = ((OpcionFiltro<EstadoReserva>) cboEstado.getSelectedItem()).valor();
            String seleccionada = codigoSeleccionado();
            Reserva[] reservas = servicio.buscar(new FiltroReservas(desde, hasta,
                    cancha == null ? null : cancha.getCodigo(), estado, txtBuscar.getText()));
            modeloTabla.setReservas(reservas);
            lblConteo.setText(reservas.length + (reservas.length == 1 ? " reserva" : " reservas"));
            if (seleccionada != null) {
                seleccionar(seleccionada);
            }
            mostrarDetalle();
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private String codigoSeleccionado() {
        int fila = tblReservas.getSelectedRow();
        return fila < 0 ? null : modeloTabla.getReserva(fila).getCodigo();
    }

    private void seleccionar(String codigo) {
        int fila = modeloTabla.indiceDe(codigo);
        if (fila >= 0) {
            tblReservas.setRowSelectionInterval(fila, fila);
            tblReservas.scrollRectToVisible(tblReservas.getCellRect(fila, 0, true));
        }
    }

    private void mostrarDetalle() {
        String codigo = codigoSeleccionado();
        if (codigo == null) {
            txtDetalle.setText("Seleccione una reserva para ver su detalle.");
            btnRegistrarPago.setEnabled(false);
            btnCancelarReserva.setEnabled(false);
            return;
        }
        Reserva r = servicio.obtener(codigo);
        txtDetalle.setText(FormatoReserva.detalle(r));
        txtDetalle.setCaretPosition(0);
        btnRegistrarPago.setEnabled(r.getEstado().permitePagos() && r.getSaldo().signum() > 0);
        btnCancelarReserva.setEnabled(r.getEstado().permiteCancelar());
    }

    private void cancelarReserva() {
        String codigo = codigoSeleccionado();
        if (codigo == null) {
            return;
        }
        Reserva r = servicio.obtener(codigo);
        String motivo = JOptionPane.showInputDialog(this,
                "Motivo de la cancelación de " + r.getCodigo() + " (mínimo 10 caracteres).\n"
                + "La reserva se conserva en el historial y el horario queda libre.",
                "Cancelar reserva", JOptionPane.QUESTION_MESSAGE);
        if (motivo == null) {
            return;
        }
        try {
            servicio.cancelar(codigo, motivo, sesion.getUsuario());
            buscar();
            if (r.getTotalPagado().signum() > 0) {
                JOptionPane.showMessageDialog(this, "Reserva cancelada. Tenía pagos registrados por "
                        + FormatoMoneda.formatear(r.getTotalPagado())
                        + "; la devolución, si corresponde, se gestiona fuera del sistema.",
                        "Cancelar reserva", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void registrarPago() {
        String codigo = codigoSeleccionado();
        if (codigo == null) {
            return;
        }
        DlgRegistrarPago.registrar(SwingUtilities.getWindowAncestor(this), servicio, servicio.obtener(codigo),
                sesion.getUsuario(), configuracion.getNombreNegocio()).ifPresent(p -> buscar());
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Reservas", JOptionPane.WARNING_MESSAGE);
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
        pnlFiltros = new javax.swing.JPanel();
        lblDesde = new javax.swing.JLabel();
        spnDesde = new javax.swing.JSpinner();
        lblHasta = new javax.swing.JLabel();
        spnHasta = new javax.swing.JSpinner();
        lblCancha = new javax.swing.JLabel();
        cboCancha = new javax.swing.JComboBox<>();
        lblEstado = new javax.swing.JLabel();
        cboEstado = new javax.swing.JComboBox<>();
        lblBuscar = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        btnBuscar = new javax.swing.JButton();
        btnHoy = new javax.swing.JButton();
        btnProximos = new javax.swing.JButton();
        scrReservas = new javax.swing.JScrollPane();
        tblReservas = new javax.swing.JTable();
        pnlDetalle = new javax.swing.JPanel();
        scrDetalle = new javax.swing.JScrollPane();
        txtDetalle = new javax.swing.JTextArea();
        pnlBotones = new javax.swing.JPanel();
        lblConteo = new javax.swing.JLabel();
        pnlAcciones = new javax.swing.JPanel();
        btnRegistrarPago = new javax.swing.JButton();
        btnCancelarReserva = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        pnlNorte.setLayout(new java.awt.BorderLayout(0, 8));
        lblTitulo.setText("Reservas");
        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        pnlNorte.add(lblTitulo, java.awt.BorderLayout.NORTH);

        pnlFiltros.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 2));
        lblDesde.setText("Desde:");
        pnlFiltros.add(lblDesde);

        pnlFiltros.add(spnDesde);

        lblHasta.setText("Hasta:");
        pnlFiltros.add(lblHasta);

        pnlFiltros.add(spnHasta);

        lblCancha.setText("Cancha:");
        pnlFiltros.add(lblCancha);

        pnlFiltros.add(cboCancha);

        lblEstado.setText("Estado:");
        pnlFiltros.add(lblEstado);

        pnlFiltros.add(cboEstado);

        lblBuscar.setText("Cliente / código:");
        pnlFiltros.add(lblBuscar);

        txtBuscar.setColumns(14);
        pnlFiltros.add(txtBuscar);

        btnBuscar.setText("Buscar");
        btnBuscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarActionPerformed(evt);
            }
        });
        pnlFiltros.add(btnBuscar);

        btnHoy.setText("Hoy");
        btnHoy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHoyActionPerformed(evt);
            }
        });
        pnlFiltros.add(btnHoy);

        btnProximos.setText("Próximos 7 días");
        btnProximos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProximosActionPerformed(evt);
            }
        });
        pnlFiltros.add(btnProximos);

        pnlNorte.add(pnlFiltros, java.awt.BorderLayout.CENTER);

        add(pnlNorte, java.awt.BorderLayout.NORTH);

        tblReservas.setRowHeight(24);
        scrReservas.setViewportView(tblReservas);

        add(scrReservas, java.awt.BorderLayout.CENTER);

        pnlDetalle.setBorder(javax.swing.BorderFactory.createTitledBorder("Detalle de la reserva"));
        pnlDetalle.setLayout(new java.awt.BorderLayout());
        txtDetalle.setEditable(false);
        txtDetalle.setColumns(54);
        txtDetalle.setRows(20);
        txtDetalle.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        scrDetalle.setViewportView(txtDetalle);

        pnlDetalle.add(scrDetalle, java.awt.BorderLayout.CENTER);

        add(pnlDetalle, java.awt.BorderLayout.EAST);

        pnlBotones.setLayout(new java.awt.BorderLayout());
        lblConteo.setText("0 reservas");
        lblConteo.setForeground(new java.awt.Color(128, 128, 128));
        pnlBotones.add(lblConteo, java.awt.BorderLayout.WEST);

        pnlAcciones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        btnRegistrarPago.setText("Registrar pago…");
        btnRegistrarPago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarPagoActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnRegistrarPago);

        btnCancelarReserva.setText("Cancelar reserva…");
        btnCancelarReserva.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelarReservaActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnCancelarReserva);

        pnlBotones.add(pnlAcciones, java.awt.BorderLayout.EAST);

        add(pnlBotones, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        buscar();
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnHoyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHoyActionPerformed
        SelectorFecha.establecer(spnDesde, LocalDate.now());
        SelectorFecha.establecer(spnHasta, LocalDate.now());
        buscar();
    }//GEN-LAST:event_btnHoyActionPerformed

    private void btnProximosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProximosActionPerformed
        SelectorFecha.establecer(spnDesde, LocalDate.now());
        SelectorFecha.establecer(spnHasta, LocalDate.now().plusDays(7));
        buscar();
    }//GEN-LAST:event_btnProximosActionPerformed

    private void btnRegistrarPagoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarPagoActionPerformed
        registrarPago();
    }//GEN-LAST:event_btnRegistrarPagoActionPerformed

    private void btnCancelarReservaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelarReservaActionPerformed
        cancelarReserva();
    }//GEN-LAST:event_btnCancelarReservaActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnCancelarReserva;
    private javax.swing.JButton btnHoy;
    private javax.swing.JButton btnProximos;
    private javax.swing.JButton btnRegistrarPago;
    private javax.swing.JComboBox<canchapro.util.OpcionFiltro<canchapro.canchas.Cancha>> cboCancha;
    private javax.swing.JComboBox<canchapro.util.OpcionFiltro<EstadoReserva>> cboEstado;
    private javax.swing.JLabel lblBuscar;
    private javax.swing.JLabel lblCancha;
    private javax.swing.JLabel lblConteo;
    private javax.swing.JLabel lblDesde;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblHasta;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel pnlAcciones;
    private javax.swing.JPanel pnlBotones;
    private javax.swing.JPanel pnlDetalle;
    private javax.swing.JPanel pnlFiltros;
    private javax.swing.JPanel pnlNorte;
    private javax.swing.JScrollPane scrDetalle;
    private javax.swing.JScrollPane scrReservas;
    private javax.swing.JSpinner spnDesde;
    private javax.swing.JSpinner spnHasta;
    private javax.swing.JTable tblReservas;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextArea txtDetalle;
    // End of variables declaration//GEN-END:variables
}
