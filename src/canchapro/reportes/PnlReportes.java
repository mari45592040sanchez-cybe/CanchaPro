package canchapro.reportes;

import canchapro.canchas.Cancha;
import canchapro.canchas.CanchaServicio;
import canchapro.principal.Refrescable;
import canchapro.reportes.Reporte.Filtro;
import canchapro.reservas.EstadoReserva;
import canchapro.util.OpcionFiltro;
import canchapro.util.SelectorFecha;
import canchapro.util.excepciones.CanchaProException;
import java.awt.Font;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.TitledBorder;

/**
 * Pantalla de reportes. Solo conoce la interfaz {@link Reporte}: cualquier
 * reporte registrado en {@link ReporteServicio} aparece en la lista.
 */
// POO - Herencia: extiende JPanel. POO - Polimorfismo: implementa Refrescable y trabaja solo con la
// interfaz Reporte; no sabe qué reporte concreto está generando.
public class PnlReportes extends javax.swing.JPanel implements Refrescable {

    // POO - Relaciones: usa el servicio de reportes para generar y exportar.
    private final ReporteServicio servicio;
    private final ReporteTableModel modeloTabla = new ReporteTableModel();
    private ResultadoReporte ultimoResultado;
    private boolean cargando = true;

    public PnlReportes(ReporteServicio servicio, CanchaServicio canchaServicio) {
        this.servicio = servicio;
        initComponents();

        cboReporte.setModel(new DefaultComboBoxModel<>(servicio.listar()));
        cboCancha.setModel(new DefaultComboBoxModel<>(OpcionFiltro.arreglo("Todas",
                canchaServicio.listar(), c -> c.getCodigo() + " " + c.getNombre())));
        cboEstado.setModel(new DefaultComboBoxModel<>(OpcionFiltro.arreglo("Todos",
                EstadoReserva.values(), EstadoReserva::toString)));
        SelectorFecha.configurar(spnDesde, LocalDate.now().withDayOfMonth(1));
        SelectorFecha.configurar(spnHasta, LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        tblResultado.setModel(modeloTabla);
        txtCliente.addActionListener(e -> generar());

        cargando = false;
        reporteSeleccionado();
    }

    @Override
    public void refrescar() {
        generar();
    }

    /** Al elegir otro reporte se habilitan solo sus filtros y se genera. */
    private void reporteSeleccionado() {
        Reporte reporte = (Reporte) cboReporte.getSelectedItem();
        if (cargando || reporte == null) {
            return;
        }
        lblDescripcion.setText("<html><body style='width:230px'>" + reporte.getDescripcion() + "</body></html>");
        boolean fechas = reporte.usaFiltro(Filtro.FECHAS);
        spnDesde.setEnabled(fechas);
        spnHasta.setEnabled(fechas);
        btnHoy.setEnabled(fechas);
        btnEsteMes.setEnabled(fechas);
        btnMesAnterior.setEnabled(fechas);
        cboCancha.setEnabled(reporte.usaFiltro(Filtro.CANCHA));
        cboEstado.setEnabled(reporte.usaFiltro(Filtro.ESTADO));
        txtCliente.setEnabled(reporte.usaFiltro(Filtro.CLIENTE));
        generar();
    }

    @SuppressWarnings("unchecked")
    private void generar() {
        Reporte reporte = (Reporte) cboReporte.getSelectedItem();
        if (cargando || reporte == null) {
            return;
        }
        try {
            Cancha cancha = ((OpcionFiltro<Cancha>) cboCancha.getSelectedItem()).valor();
            EstadoReserva estado = ((OpcionFiltro<EstadoReserva>) cboEstado.getSelectedItem()).valor();
            FiltroReporte filtro = new FiltroReporte(SelectorFecha.obtener(spnDesde), SelectorFecha.obtener(spnHasta),
                    cancha == null ? null : cancha.getCodigo(), estado, txtCliente.getText());
            mostrar(servicio.generar(reporte, filtro));
        } catch (CanchaProException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Reportes", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void mostrar(ResultadoReporte resultado) {
        ultimoResultado = resultado;
        modeloTabla.setResultado(resultado);
        ((TitledBorder) pnlResultado.getBorder()).setTitle(resultado.getTitulo());
        lblSubtitulo.setText(resultado.getSubtitulo() + "  ·  " + resultado.getCantidadFilas() + " fila(s)");
        pnlTotales.removeAll();
        for (int i = 0; i < resultado.getCantidadTotales(); i++) {
            JLabel etiqueta = new JLabel(resultado.getConceptoTotal(i) + ": " + resultado.getValorTotal(i));
            etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD));
            pnlTotales.add(etiqueta);
        }
        pnlTotales.revalidate();
        pnlResultado.repaint();
    }

    private void exportar() {
        if (ultimoResultado == null) {
            return;
        }
        try {
            Path archivo = servicio.exportarCsv(ultimoResultado);
            JOptionPane.showMessageDialog(this, "Reporte exportado en:\n" + archivo,
                    "Exportar", JOptionPane.INFORMATION_MESSAGE);
        } catch (CanchaProException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Exportar", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void usarPeriodo(LocalDate desde, LocalDate hasta) {
        cargando = true;
        SelectorFecha.establecer(spnDesde, desde);
        SelectorFecha.establecer(spnHasta, hasta);
        cargando = false;
        generar();
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

        lblTitulo = new javax.swing.JLabel();
        pnlFiltros = new javax.swing.JPanel();
        lblReporte = new javax.swing.JLabel();
        cboReporte = new javax.swing.JComboBox<>();
        lblDescripcion = new javax.swing.JLabel();
        lblDesde = new javax.swing.JLabel();
        spnDesde = new javax.swing.JSpinner();
        lblHasta = new javax.swing.JLabel();
        spnHasta = new javax.swing.JSpinner();
        pnlRapidos = new javax.swing.JPanel();
        btnHoy = new javax.swing.JButton();
        btnEsteMes = new javax.swing.JButton();
        btnMesAnterior = new javax.swing.JButton();
        lblCancha = new javax.swing.JLabel();
        cboCancha = new javax.swing.JComboBox<>();
        lblEstado = new javax.swing.JLabel();
        cboEstado = new javax.swing.JComboBox<>();
        lblCliente = new javax.swing.JLabel();
        txtCliente = new javax.swing.JTextField();
        pnlAcciones = new javax.swing.JPanel();
        btnGenerar = new javax.swing.JButton();
        btnExportar = new javax.swing.JButton();
        pnlResultado = new javax.swing.JPanel();
        lblSubtitulo = new javax.swing.JLabel();
        scrResultado = new javax.swing.JScrollPane();
        tblResultado = new javax.swing.JTable();
        pnlTotales = new javax.swing.JPanel();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        lblTitulo.setText("Reportes");
        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        add(lblTitulo, java.awt.BorderLayout.NORTH);

        pnlFiltros.setBorder(javax.swing.BorderFactory.createTitledBorder("Reporte y filtros"));
        pnlFiltros.setLayout(new java.awt.GridBagLayout());
        lblReporte.setText("Reporte:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFiltros.add(lblReporte, gridBagConstraints);

        cboReporte.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboReporteActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFiltros.add(cboReporte, gridBagConstraints);

        lblDescripcion.setText(" ");
        lblDescripcion.setForeground(new java.awt.Color(128, 128, 128));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 8, 10, 8);
        pnlFiltros.add(lblDescripcion, gridBagConstraints);

        lblDesde.setText("Desde:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFiltros.add(lblDesde, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFiltros.add(spnDesde, gridBagConstraints);

        lblHasta.setText("Hasta:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFiltros.add(lblHasta, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFiltros.add(spnHasta, gridBagConstraints);

        pnlRapidos.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        btnHoy.setText("Hoy");
        btnHoy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHoyActionPerformed(evt);
            }
        });
        pnlRapidos.add(btnHoy);

        btnEsteMes.setText("Este mes");
        btnEsteMes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEsteMesActionPerformed(evt);
            }
        });
        pnlRapidos.add(btnEsteMes);

        btnMesAnterior.setText("Mes anterior");
        btnMesAnterior.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMesAnteriorActionPerformed(evt);
            }
        });
        pnlRapidos.add(btnMesAnterior);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 4, 6, 4);
        pnlFiltros.add(pnlRapidos, gridBagConstraints);

        lblCancha.setText("Cancha:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFiltros.add(lblCancha, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFiltros.add(cboCancha, gridBagConstraints);

        lblEstado.setText("Estado:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFiltros.add(lblEstado, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFiltros.add(cboEstado, gridBagConstraints);

        lblCliente.setText("Cliente:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFiltros.add(lblCliente, gridBagConstraints);

        txtCliente.setColumns(14);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFiltros.add(txtCliente, gridBagConstraints);

        pnlAcciones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        btnGenerar.setText("Generar reporte");
        btnGenerar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGenerarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnGenerar);

        btnExportar.setText("Exportar a CSV…");
        btnExportar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnExportar);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 4, 4, 4);
        pnlFiltros.add(pnlAcciones, gridBagConstraints);

        add(pnlFiltros, java.awt.BorderLayout.WEST);

        pnlResultado.setBorder(javax.swing.BorderFactory.createTitledBorder("Resultado"));
        pnlResultado.setLayout(new java.awt.BorderLayout(0, 8));
        lblSubtitulo.setText(" ");
        lblSubtitulo.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pnlResultado.add(lblSubtitulo, java.awt.BorderLayout.NORTH);

        tblResultado.setRowHeight(24);
        scrResultado.setViewportView(tblResultado);

        pnlResultado.add(scrResultado, java.awt.BorderLayout.CENTER);

        pnlTotales.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 18, 4));
        pnlResultado.add(pnlTotales, java.awt.BorderLayout.SOUTH);

        add(pnlResultado, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void cboReporteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboReporteActionPerformed
        reporteSeleccionado();
    }//GEN-LAST:event_cboReporteActionPerformed

    private void btnHoyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHoyActionPerformed
        usarPeriodo(LocalDate.now(), LocalDate.now());
    }//GEN-LAST:event_btnHoyActionPerformed

    private void btnEsteMesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEsteMesActionPerformed
        LocalDate hoy = LocalDate.now();
        usarPeriodo(hoy.withDayOfMonth(1), hoy.withDayOfMonth(hoy.lengthOfMonth()));
    }//GEN-LAST:event_btnEsteMesActionPerformed

    private void btnMesAnteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMesAnteriorActionPerformed
        LocalDate mes = LocalDate.now().minusMonths(1);
        usarPeriodo(mes.withDayOfMonth(1), mes.withDayOfMonth(mes.lengthOfMonth()));
    }//GEN-LAST:event_btnMesAnteriorActionPerformed

    private void btnGenerarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGenerarActionPerformed
        generar();
    }//GEN-LAST:event_btnGenerarActionPerformed

    private void btnExportarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportarActionPerformed
        exportar();
    }//GEN-LAST:event_btnExportarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEsteMes;
    private javax.swing.JButton btnExportar;
    private javax.swing.JButton btnGenerar;
    private javax.swing.JButton btnHoy;
    private javax.swing.JButton btnMesAnterior;
    private javax.swing.JComboBox<canchapro.util.OpcionFiltro<canchapro.canchas.Cancha>> cboCancha;
    private javax.swing.JComboBox<canchapro.util.OpcionFiltro<canchapro.reservas.EstadoReserva>> cboEstado;
    private javax.swing.JComboBox<Reporte> cboReporte;
    private javax.swing.JLabel lblCancha;
    private javax.swing.JLabel lblCliente;
    private javax.swing.JLabel lblDescripcion;
    private javax.swing.JLabel lblDesde;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblHasta;
    private javax.swing.JLabel lblReporte;
    private javax.swing.JLabel lblSubtitulo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel pnlAcciones;
    private javax.swing.JPanel pnlFiltros;
    private javax.swing.JPanel pnlRapidos;
    private javax.swing.JPanel pnlResultado;
    private javax.swing.JPanel pnlTotales;
    private javax.swing.JScrollPane scrResultado;
    private javax.swing.JSpinner spnDesde;
    private javax.swing.JSpinner spnHasta;
    private javax.swing.JTable tblResultado;
    private javax.swing.JTextField txtCliente;
    // End of variables declaration//GEN-END:variables
}
