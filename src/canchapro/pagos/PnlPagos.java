package canchapro.pagos;

import canchapro.configuracion.Configuracion;
import canchapro.login.Sesion;
import canchapro.principal.Refrescable;
import canchapro.reservas.EstadoReserva;
import canchapro.reservas.EstadoReservaRenderer;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import canchapro.reservas.ReservaTableModel;
import canchapro.util.FormatoMoneda;
import canchapro.util.excepciones.CanchaProException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * Pantalla de pagos: buscar una reserva, ver su total, lo pagado y el saldo,
 * registrar abonos y ver o imprimir el comprobante de cada pago.
 */
// POO - Herencia: la pantalla extiende JPanel de Swing.
public class PnlPagos extends javax.swing.JPanel implements Refrescable {

    private final PagoServicio pagoServicio;
    private final ReservaServicio reservaServicio;
    private final Configuracion configuracion;
    private final Sesion sesion;
    private final ReservaTableModel modeloReservas = new ReservaTableModel();
    private final PagoTableModel modeloPagos = new PagoTableModel();

    public PnlPagos(PagoServicio pagoServicio, ReservaServicio reservaServicio, Configuracion configuracion,
            Sesion sesion) {
        this.pagoServicio = pagoServicio;
        this.reservaServicio = reservaServicio;
        this.configuracion = configuracion;
        this.sesion = sesion;
        initComponents();

        tblReservas.setModel(modeloReservas);
        tblReservas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblReservas.setDefaultRenderer(EstadoReserva.class, new EstadoReservaRenderer());
        tblReservas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarPagos();
            }
        });
        tblPagos.setModel(modeloPagos);
        tblPagos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPagos.getSelectionModel().addListSelectionListener(e -> btnComprobante.setEnabled(tblPagos.getSelectedRow() >= 0));
        tblPagos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    verComprobante();
                }
            }
        });
        txtBuscar.addActionListener(e -> buscar());

        buscar();
    }

    @Override
    public void refrescar() {
        buscar();
    }

    private void buscar() {
        String seleccionada = codigoReservaSeleccionada();
        Reserva[] reservas = pagoServicio.buscarReservas(txtBuscar.getText(), chkSoloSaldo.isSelected());
        modeloReservas.setReservas(reservas);
        if (seleccionada != null) {
            int fila = modeloReservas.indiceDe(seleccionada);
            if (fila >= 0) {
                tblReservas.setRowSelectionInterval(fila, fila);
            }
        }
        mostrarPagos();
        lblIngresosHoy.setText("Ingresos de hoy: " + FormatoMoneda.formatear(pagoServicio.ingresosDeHoy()));
    }

    private String codigoReservaSeleccionada() {
        int fila = tblReservas.getSelectedRow();
        return fila < 0 ? null : modeloReservas.getReserva(fila).getCodigo();
    }

    private void mostrarPagos() {
        String codigo = codigoReservaSeleccionada();
        if (codigo == null) {
            modeloPagos.setPagos(new Pago[0]);
            lblReservaSel.setText("Seleccione una reserva");
            lblTotal.setText("Total: -");
            lblPagado.setText("Pagado: -");
            lblSaldo.setText("Saldo: -");
            btnRegistrarPago.setEnabled(false);
            btnComprobante.setEnabled(false);
            return;
        }
        Reserva r = reservaServicio.obtener(codigo);
        modeloPagos.setPagos(r.getPagos());
        lblReservaSel.setText(r.getCodigo() + " · " + r.getCliente().getNombre() + " · " + r.getEstado());
        lblTotal.setText("Total: " + FormatoMoneda.formatear(r.getTotal()));
        lblPagado.setText("Pagado: " + FormatoMoneda.formatear(r.getTotalPagado()));
        lblSaldo.setText("Saldo: " + FormatoMoneda.formatear(r.getSaldo()));
        btnRegistrarPago.setEnabled(r.getEstado().permitePagos() && r.getSaldo().signum() > 0);
        btnComprobante.setEnabled(false);
    }

    private void registrarPago() {
        String codigo = codigoReservaSeleccionada();
        if (codigo == null) {
            return;
        }
        DlgRegistrarPago.registrar(SwingUtilities.getWindowAncestor(this), reservaServicio,
                reservaServicio.obtener(codigo), sesion.getUsuario(), configuracion.getNombreNegocio())
                .ifPresent(p -> {
                    // Si quedó sin saldo y se filtran "solo con saldo", se muestra igual para ver el pago.
                    if (chkSoloSaldo.isSelected() && reservaServicio.obtener(codigo).getSaldo().signum() == 0) {
                        chkSoloSaldo.setSelected(false);
                    }
                    buscar();
                });
    }

    private void verComprobante() {
        int fila = tblPagos.getSelectedRow();
        if (fila < 0) {
            return;
        }
        try {
            DlgComprobante.mostrar(SwingUtilities.getWindowAncestor(this),
                    pagoServicio.comprobante(modeloPagos.getPago(fila).getCodigo()));
        } catch (CanchaProException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Pagos", JOptionPane.WARNING_MESSAGE);
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
        java.awt.GridBagConstraints gridBagConstraints;

        pnlNorte = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        pnlBusqueda = new javax.swing.JPanel();
        lblBuscar = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        chkSoloSaldo = new javax.swing.JCheckBox();
        btnBuscar = new javax.swing.JButton();
        lblIngresosHoy = new javax.swing.JLabel();
        pnlCentro = new javax.swing.JPanel();
        pnlReservas = new javax.swing.JPanel();
        scrReservas = new javax.swing.JScrollPane();
        tblReservas = new javax.swing.JTable();
        pnlPagosReserva = new javax.swing.JPanel();
        pnlTotales = new javax.swing.JPanel();
        lblReservaSel = new javax.swing.JLabel();
        lblTotal = new javax.swing.JLabel();
        lblPagado = new javax.swing.JLabel();
        lblSaldo = new javax.swing.JLabel();
        scrPagos = new javax.swing.JScrollPane();
        tblPagos = new javax.swing.JTable();
        pnlBotones = new javax.swing.JPanel();
        btnComprobante = new javax.swing.JButton();
        btnRegistrarPago = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        pnlNorte.setLayout(new java.awt.BorderLayout(0, 8));
        lblTitulo.setText("Pagos");
        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        pnlNorte.add(lblTitulo, java.awt.BorderLayout.NORTH);

        pnlBusqueda.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 2));
        lblBuscar.setText("Buscar reserva (código, cliente o teléfono):");
        pnlBusqueda.add(lblBuscar);

        txtBuscar.setColumns(22);
        pnlBusqueda.add(txtBuscar);

        chkSoloSaldo.setSelected(true);
        chkSoloSaldo.setText("Solo con saldo pendiente");
        chkSoloSaldo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkSoloSaldoActionPerformed(evt);
            }
        });
        pnlBusqueda.add(chkSoloSaldo);

        btnBuscar.setText("Buscar");
        btnBuscar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBuscarActionPerformed(evt);
            }
        });
        pnlBusqueda.add(btnBuscar);

        pnlNorte.add(pnlBusqueda, java.awt.BorderLayout.CENTER);

        lblIngresosHoy.setText("Ingresos de hoy: -");
        lblIngresosHoy.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblIngresosHoy.setForeground(new java.awt.Color(22, 101, 52));
        pnlNorte.add(lblIngresosHoy, java.awt.BorderLayout.EAST);

        add(pnlNorte, java.awt.BorderLayout.NORTH);

        pnlCentro.setLayout(new java.awt.GridBagLayout());
        pnlReservas.setBorder(javax.swing.BorderFactory.createTitledBorder("Reservas"));
        pnlReservas.setLayout(new java.awt.BorderLayout());
        tblReservas.setRowHeight(24);
        scrReservas.setViewportView(tblReservas);

        pnlReservas.add(scrReservas, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        pnlCentro.add(pnlReservas, gridBagConstraints);

        pnlPagosReserva.setBorder(javax.swing.BorderFactory.createTitledBorder("Pagos de la reserva seleccionada"));
        pnlPagosReserva.setLayout(new java.awt.BorderLayout(0, 4));
        pnlTotales.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 24, 4));
        lblReservaSel.setText("Seleccione una reserva");
        lblReservaSel.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pnlTotales.add(lblReservaSel);

        lblTotal.setText("Total: -");
        pnlTotales.add(lblTotal);

        lblPagado.setText("Pagado: -");
        pnlTotales.add(lblPagado);

        lblSaldo.setText("Saldo: -");
        lblSaldo.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        pnlTotales.add(lblSaldo);

        pnlPagosReserva.add(pnlTotales, java.awt.BorderLayout.NORTH);

        tblPagos.setRowHeight(24);
        scrPagos.setViewportView(tblPagos);

        pnlPagosReserva.add(scrPagos, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        pnlCentro.add(pnlPagosReserva, gridBagConstraints);

        add(pnlCentro, java.awt.BorderLayout.CENTER);

        pnlBotones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        btnComprobante.setText("Ver comprobante del pago");
        btnComprobante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnComprobanteActionPerformed(evt);
            }
        });
        pnlBotones.add(btnComprobante);

        btnRegistrarPago.setText("Registrar pago…");
        btnRegistrarPago.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarPagoActionPerformed(evt);
            }
        });
        pnlBotones.add(btnRegistrarPago);

        add(pnlBotones, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void chkSoloSaldoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkSoloSaldoActionPerformed
        buscar();
    }//GEN-LAST:event_chkSoloSaldoActionPerformed

    private void btnBuscarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBuscarActionPerformed
        buscar();
    }//GEN-LAST:event_btnBuscarActionPerformed

    private void btnComprobanteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnComprobanteActionPerformed
        verComprobante();
    }//GEN-LAST:event_btnComprobanteActionPerformed

    private void btnRegistrarPagoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarPagoActionPerformed
        registrarPago();
    }//GEN-LAST:event_btnRegistrarPagoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBuscar;
    private javax.swing.JButton btnComprobante;
    private javax.swing.JButton btnRegistrarPago;
    private javax.swing.JCheckBox chkSoloSaldo;
    private javax.swing.JLabel lblBuscar;
    private javax.swing.JLabel lblIngresosHoy;
    private javax.swing.JLabel lblPagado;
    private javax.swing.JLabel lblReservaSel;
    private javax.swing.JLabel lblSaldo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JPanel pnlBotones;
    private javax.swing.JPanel pnlBusqueda;
    private javax.swing.JPanel pnlCentro;
    private javax.swing.JPanel pnlNorte;
    private javax.swing.JPanel pnlPagosReserva;
    private javax.swing.JPanel pnlReservas;
    private javax.swing.JPanel pnlTotales;
    private javax.swing.JScrollPane scrPagos;
    private javax.swing.JScrollPane scrReservas;
    private javax.swing.JTable tblPagos;
    private javax.swing.JTable tblReservas;
    private javax.swing.JTextField txtBuscar;
    // End of variables declaration//GEN-END:variables
}
