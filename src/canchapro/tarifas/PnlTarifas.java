package canchapro.tarifas;

import canchapro.canchas.OpcionTipoCancha;
import canchapro.canchas.TipoCancha;
import canchapro.util.BloquesHorario;
import canchapro.util.BloquesHorario.OpcionDuracion;
import canchapro.util.FormatoMoneda;
import canchapro.util.SelectorFecha;
import canchapro.util.Validador;
import canchapro.util.excepciones.CanchaProException;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;

/**
 * Configuración de tarifas por franja horaria y simulador de precio.
 */
// POO - Herencia: la pantalla extiende JPanel de Swing.
public class PnlTarifas extends javax.swing.JPanel {

    // POO - Relaciones / Responsabilidad: la pantalla usa el servicio; el precio lo calcula TarifaServicio.
    private final TarifaServicio servicio;
    private final TarifaTableModel modeloTabla = new TarifaTableModel();
    private final TramoTableModel modeloTramos = new TramoTableModel();
    /** Código de la tarifa en edición; null cuando se registra una nueva. */
    private String codigoSeleccionado;

    public PnlTarifas(TarifaServicio servicio) {
        this.servicio = servicio;
        initComponents();

        cboTipoDia.setModel(new DefaultComboBoxModel<>(TipoDia.values()));
        cboDesde.setModel(new DefaultComboBoxModel<>(BloquesHorario.horasDelDia()));
        cboHasta.setModel(new DefaultComboBoxModel<>(BloquesHorario.horasDelDia()));
        cboAlcance.setModel(new DefaultComboBoxModel<>(OpcionTipoCancha.opciones()));

        tblTarifas.setModel(modeloTabla);
        tblTarifas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblTarifas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarFila();
            }
        });

        SelectorFecha.configurar(spnFechaSim, LocalDate.now());
        cboHoraSim.setModel(new DefaultComboBoxModel<>(BloquesHorario.horasEntre(LocalTime.of(6, 0), LocalTime.of(23, 0))));
        cboHoraSim.setSelectedItem(LocalTime.of(16, 0));
        cboDuracionSim.setModel(new DefaultComboBoxModel<>(BloquesHorario.duracionesPermitidas()));
        cboDuracionSim.setSelectedIndex(2);
        cboTipoSim.setModel(new DefaultComboBoxModel<>(TipoCancha.values()));
        tblTramos.setModel(modeloTramos);
        scrTramos.setPreferredSize(new Dimension(340, 120));

        cargarTabla();
        limpiarFormulario();
    }

    private void cargarTabla() {
        modeloTabla.setTarifas(servicio.listar());
    }

    private void seleccionarFila() {
        int fila = tblTarifas.getSelectedRow();
        if (fila >= 0) {
            mostrarTarifa(modeloTabla.getTarifa(fila));
        }
    }

    private void mostrarTarifa(Tarifa tarifa) {
        codigoSeleccionado = tarifa.getCodigo();
        lblCodigoValor.setText(tarifa.getCodigo());
        cboTipoDia.setSelectedItem(tarifa.getTipoDia());
        cboDesde.setSelectedItem(tarifa.getHoraDesde());
        cboHasta.setSelectedItem(tarifa.getHoraHasta());
        cboAlcance.setSelectedItem(new OpcionTipoCancha(tarifa.getTipoCancha()));
        txtPrecio.setText(tarifa.getPrecioHora().toPlainString());
        lblEstadoValor.setText(tarifa.isActiva() ? "Activa" : "Inactiva");
        btnActivarDesactivar.setText(tarifa.isActiva() ? "Desactivar" : "Activar");
        btnActivarDesactivar.setEnabled(true);
    }

    private void limpiarFormulario() {
        codigoSeleccionado = null;
        tblTarifas.clearSelection();
        lblCodigoValor.setText("(nueva)");
        cboTipoDia.setSelectedItem(TipoDia.LUNES_VIERNES);
        cboDesde.setSelectedItem(LocalTime.of(8, 0));
        cboHasta.setSelectedItem(LocalTime.of(17, 0));
        cboAlcance.setSelectedIndex(0);
        txtPrecio.setText("");
        lblEstadoValor.setText("-");
        btnActivarDesactivar.setText("Desactivar");
        btnActivarDesactivar.setEnabled(false);
    }

    private void guardar() {
        try {
            TipoDia tipoDia = (TipoDia) cboTipoDia.getSelectedItem();
            LocalTime desde = (LocalTime) cboDesde.getSelectedItem();
            LocalTime hasta = (LocalTime) cboHasta.getSelectedItem();
            TipoCancha tipoCancha = ((OpcionTipoCancha) cboAlcance.getSelectedItem()).tipo();
            BigDecimal precio = Validador.monto(txtPrecio.getText(), "El precio por hora");
            Tarifa tarifa = codigoSeleccionado == null
                    ? servicio.registrar(tipoDia, desde, hasta, precio, tipoCancha)
                    : servicio.actualizar(codigoSeleccionado, tipoDia, desde, hasta, precio, tipoCancha);
            JOptionPane.showMessageDialog(this, "Tarifa " + tarifa.getCodigo() + " guardada.",
                    "Tarifas", JOptionPane.INFORMATION_MESSAGE);
            recargarYSeleccionar(tarifa.getCodigo());
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void activarDesactivar() {
        if (codigoSeleccionado == null) {
            return;
        }
        try {
            Tarifa tarifa = servicio.obtener(codigoSeleccionado);
            servicio.cambiarEstado(codigoSeleccionado, !tarifa.isActiva());
            recargarYSeleccionar(codigoSeleccionado);
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void recargarYSeleccionar(String codigo) {
        cargarTabla();
        int fila = modeloTabla.indiceDe(codigo);
        if (fila >= 0) {
            tblTarifas.setRowSelectionInterval(fila, fila);
        }
    }

    /** Muestra cómo se cobraría una reserva con la configuración actual. */
    private void calcularSimulacion() {
        try {
            DetallePrecio detalle = servicio.calcular(SelectorFecha.obtener(spnFechaSim),
                    (LocalTime) cboHoraSim.getSelectedItem(),
                    ((OpcionDuracion) cboDuracionSim.getSelectedItem()).duracion(),
                    (TipoCancha) cboTipoSim.getSelectedItem());
            modeloTramos.setTramos(detalle.tramos());
            String tramos = detalle.tieneVariosTramos() ? "   (" + detalle.cantidadTramos() + " tramos)" : "";
            lblTotal.setText("Total: " + FormatoMoneda.formatear(detalle.total()) + tramos);
        } catch (CanchaProException e) {
            modeloTramos.setTramos(new TramoTarifa[0]);
            lblTotal.setText("Total: -");
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Tarifas", JOptionPane.WARNING_MESSAGE);
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
        scrTarifas = new javax.swing.JScrollPane();
        tblTarifas = new javax.swing.JTable();
        pnlLateral = new javax.swing.JPanel();
        pnlFormulario = new javax.swing.JPanel();
        lblCodigo = new javax.swing.JLabel();
        lblCodigoValor = new javax.swing.JLabel();
        lblTipoDia = new javax.swing.JLabel();
        cboTipoDia = new javax.swing.JComboBox<>();
        lblDesde = new javax.swing.JLabel();
        cboDesde = new javax.swing.JComboBox<>();
        lblHasta = new javax.swing.JLabel();
        cboHasta = new javax.swing.JComboBox<>();
        lblAlcance = new javax.swing.JLabel();
        cboAlcance = new javax.swing.JComboBox<>();
        lblPrecio = new javax.swing.JLabel();
        txtPrecio = new javax.swing.JTextField();
        lblEstado = new javax.swing.JLabel();
        lblEstadoValor = new javax.swing.JLabel();
        pnlBotonesFormulario = new javax.swing.JPanel();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnActivarDesactivar = new javax.swing.JButton();
        pnlSimulador = new javax.swing.JPanel();
        lblFechaSim = new javax.swing.JLabel();
        spnFechaSim = new javax.swing.JSpinner();
        lblHoraSim = new javax.swing.JLabel();
        cboHoraSim = new javax.swing.JComboBox<>();
        lblDuracionSim = new javax.swing.JLabel();
        cboDuracionSim = new javax.swing.JComboBox<>();
        lblTipoSim = new javax.swing.JLabel();
        cboTipoSim = new javax.swing.JComboBox<>();
        btnCalcular = new javax.swing.JButton();
        scrTramos = new javax.swing.JScrollPane();
        tblTramos = new javax.swing.JTable();
        lblTotal = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        lblTitulo.setText("Tarifas");
        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        add(lblTitulo, java.awt.BorderLayout.NORTH);

        tblTarifas.setRowHeight(24);
        scrTarifas.setViewportView(tblTarifas);

        add(scrTarifas, java.awt.BorderLayout.CENTER);

        pnlLateral.setLayout(new java.awt.GridBagLayout());
        pnlFormulario.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos de la tarifa"));
        pnlFormulario.setLayout(new java.awt.GridBagLayout());
        lblCodigo.setText("Código:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblCodigo, gridBagConstraints);

        lblCodigoValor.setText("(nueva)");
        lblCodigoValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(lblCodigoValor, gridBagConstraints);

        lblTipoDia.setText("Días:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblTipoDia, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboTipoDia, gridBagConstraints);

        lblDesde.setText("Desde:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblDesde, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboDesde, gridBagConstraints);

        lblHasta.setText("Hasta:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblHasta, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboHasta, gridBagConstraints);

        lblAlcance.setText("Tipo de cancha:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblAlcance, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboAlcance, gridBagConstraints);

        lblPrecio.setText("Precio por hora (Q):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblPrecio, gridBagConstraints);

        txtPrecio.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtPrecio, gridBagConstraints);

        lblEstado.setText("Estado:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblEstado, gridBagConstraints);

        lblEstadoValor.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(lblEstadoValor, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        pnlLateral.add(pnlFormulario, gridBagConstraints);

        pnlBotonesFormulario.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        btnNuevo.setText("Nueva");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });
        pnlBotonesFormulario.add(btnNuevo);

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        pnlBotonesFormulario.add(btnGuardar);

        btnActivarDesactivar.setText("Desactivar");
        btnActivarDesactivar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActivarDesactivarActionPerformed(evt);
            }
        });
        pnlBotonesFormulario.add(btnActivarDesactivar);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        pnlLateral.add(pnlBotonesFormulario, gridBagConstraints);

        pnlSimulador.setBorder(javax.swing.BorderFactory.createTitledBorder("Simulador de precio"));
        pnlSimulador.setLayout(new java.awt.GridBagLayout());
        lblFechaSim.setText("Fecha:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlSimulador.add(lblFechaSim, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlSimulador.add(spnFechaSim, gridBagConstraints);

        lblHoraSim.setText("Hora de inicio:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlSimulador.add(lblHoraSim, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlSimulador.add(cboHoraSim, gridBagConstraints);

        lblDuracionSim.setText("Duración:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlSimulador.add(lblDuracionSim, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlSimulador.add(cboDuracionSim, gridBagConstraints);

        lblTipoSim.setText("Tipo de cancha:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlSimulador.add(lblTipoSim, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlSimulador.add(cboTipoSim, gridBagConstraints);

        btnCalcular.setText("Calcular precio");
        btnCalcular.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCalcularActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 6, 8);
        pnlSimulador.add(btnCalcular, gridBagConstraints);

        tblTramos.setRowHeight(24);
        scrTramos.setViewportView(tblTramos);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 4, 8);
        pnlSimulador.add(scrTramos, gridBagConstraints);

        lblTotal.setText("Total: -");
        lblTotal.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 8, 8);
        pnlSimulador.add(lblTotal, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlLateral.add(pnlSimulador, gridBagConstraints);

        add(pnlLateral, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        limpiarFormulario();
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardar();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnActivarDesactivarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActivarDesactivarActionPerformed
        activarDesactivar();
    }//GEN-LAST:event_btnActivarDesactivarActionPerformed

    private void btnCalcularActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCalcularActionPerformed
        calcularSimulacion();
    }//GEN-LAST:event_btnCalcularActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnActivarDesactivar;
    private javax.swing.JButton btnCalcular;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JComboBox<canchapro.canchas.OpcionTipoCancha> cboAlcance;
    private javax.swing.JComboBox<java.time.LocalTime> cboDesde;
    private javax.swing.JComboBox<canchapro.util.BloquesHorario.OpcionDuracion> cboDuracionSim;
    private javax.swing.JComboBox<java.time.LocalTime> cboHasta;
    private javax.swing.JComboBox<java.time.LocalTime> cboHoraSim;
    private javax.swing.JComboBox<TipoDia> cboTipoDia;
    private javax.swing.JComboBox<canchapro.canchas.TipoCancha> cboTipoSim;
    private javax.swing.JLabel lblAlcance;
    private javax.swing.JLabel lblCodigo;
    private javax.swing.JLabel lblCodigoValor;
    private javax.swing.JLabel lblDesde;
    private javax.swing.JLabel lblDuracionSim;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblEstadoValor;
    private javax.swing.JLabel lblFechaSim;
    private javax.swing.JLabel lblHasta;
    private javax.swing.JLabel lblHoraSim;
    private javax.swing.JLabel lblPrecio;
    private javax.swing.JLabel lblTipoDia;
    private javax.swing.JLabel lblTipoSim;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JPanel pnlBotonesFormulario;
    private javax.swing.JPanel pnlFormulario;
    private javax.swing.JPanel pnlLateral;
    private javax.swing.JPanel pnlSimulador;
    private javax.swing.JScrollPane scrTarifas;
    private javax.swing.JScrollPane scrTramos;
    private javax.swing.JSpinner spnFechaSim;
    private javax.swing.JTable tblTarifas;
    private javax.swing.JTable tblTramos;
    private javax.swing.JTextField txtPrecio;
    // End of variables declaration//GEN-END:variables
}
