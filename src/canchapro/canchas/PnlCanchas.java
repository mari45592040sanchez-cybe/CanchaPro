package canchapro.canchas;

import canchapro.util.BloquesHorario;
import canchapro.util.excepciones.CanchaProException;
import java.time.LocalTime;
import java.util.function.Function;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

/**
 * Pantalla de administración de canchas: datos, tipo, horario y estado.
 */
// POO - Herencia: la pantalla extiende JPanel de Swing.
public class PnlCanchas extends javax.swing.JPanel {

    // POO - Relaciones / Responsabilidad: la pantalla usa el servicio para las reglas de negocio.
    private final CanchaServicio servicio;
    private final CanchaTableModel modeloTabla = new CanchaTableModel();
    /** Código de la cancha en edición; null cuando se registra una nueva. */
    private String codigoSeleccionado;
    /** Devuelve las reservas futuras de una cancha (descripción), para advertir antes de deshabilitarla. */
    private Function<String, String[]> reservasAfectadas = codigo -> new String[0];

    public PnlCanchas(CanchaServicio servicio) {
        this.servicio = servicio;
        initComponents();

        cboTipo.setModel(new DefaultComboBoxModel<>(TipoCancha.values()));
        cboEstado.setModel(new DefaultComboBoxModel<>(EstadoCancha.values()));
        cboApertura.setModel(new DefaultComboBoxModel<>(BloquesHorario.horasDelDia()));
        cboCierre.setModel(new DefaultComboBoxModel<>(BloquesHorario.horasDelDia()));
        spnCapacidad.setModel(new SpinnerNumberModel(10, 1, Cancha.CAPACIDAD_MAXIMA, 1));

        tblCanchas.setModel(modeloTabla);
        tblCanchas.setAutoCreateRowSorter(true);
        tblCanchas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCanchas.setDefaultRenderer(EstadoCancha.class, new EstadoCanchaRenderer());
        tblCanchas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarFila();
            }
        });

        cargarTabla();
        limpiarFormulario();
    }

    public void setReservasAfectadas(Function<String, String[]> consulta) {
        this.reservasAfectadas = consulta;
    }

    /**
     * Si la cancha deja de estar disponible y tiene reservas futuras, se
     * listan para que el operador decida. Las reservas no se cancelan solas.
     *
     * @return true si se puede continuar con el cambio
     */
    private boolean confirmarReservasAfectadas(String codigo) {
        String[] afectadas = reservasAfectadas.apply(codigo);
        if (afectadas.length == 0) {
            return true;
        }
        StringBuilder mensaje = new StringBuilder("La cancha tiene ").append(afectadas.length)
                .append(" reserva(s) pendientes o confirmadas a futuro:\n\n");
        for (int i = 0; i < afectadas.length && i < 10; i++) {
            mensaje.append("• ").append(afectadas[i]).append('\n');
        }
        if (afectadas.length > 10) {
            mensaje.append("… y ").append(afectadas.length - 10).append(" más.\n");
        }
        mensaje.append("\nLas reservas NO se cancelan automáticamente: revíselas en el módulo Reservas.\n")
                .append("¿Desea cambiar el estado de la cancha de todos modos?");
        return JOptionPane.showConfirmDialog(this, mensaje.toString(), "Reservas afectadas",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void cargarTabla() {
        modeloTabla.setCanchas(servicio.listar());
    }

    private void seleccionarFila() {
        int fila = tblCanchas.getSelectedRow();
        if (fila >= 0) {
            mostrarCancha(modeloTabla.getCancha(tblCanchas.convertRowIndexToModel(fila)));
        }
    }

    private void mostrarCancha(Cancha cancha) {
        codigoSeleccionado = cancha.getCodigo();
        lblCodigoValor.setText(cancha.getCodigo());
        txtNombre.setText(cancha.getNombre());
        cboTipo.setSelectedItem(cancha.getTipo());
        spnCapacidad.setValue(cancha.getCapacidad());
        cboApertura.setSelectedItem(cancha.getHoraApertura());
        cboCierre.setSelectedItem(cancha.getHoraCierre());
        cboEstado.setSelectedItem(cancha.getEstado());
        cboEstado.setEnabled(true);
        txtDescripcion.setText(cancha.getDescripcion());
    }

    private void limpiarFormulario() {
        codigoSeleccionado = null;
        tblCanchas.clearSelection();
        lblCodigoValor.setText("(nueva)");
        txtNombre.setText("");
        cboTipo.setSelectedItem(TipoCancha.FUTBOL_5);
        spnCapacidad.setValue(TipoCancha.FUTBOL_5.getCapacidadSugerida());
        cboApertura.setSelectedItem(LocalTime.of(8, 0));
        cboCierre.setSelectedItem(LocalTime.of(23, 0));
        // Una cancha nueva siempre inicia DISPONIBLE.
        cboEstado.setSelectedItem(EstadoCancha.DISPONIBLE);
        cboEstado.setEnabled(false);
        txtDescripcion.setText("");
        txtNombre.requestFocusInWindow();
    }

    private void guardar() {
        try {
            TipoCancha tipo = (TipoCancha) cboTipo.getSelectedItem();
            int capacidad = (Integer) spnCapacidad.getValue();
            LocalTime apertura = (LocalTime) cboApertura.getSelectedItem();
            LocalTime cierre = (LocalTime) cboCierre.getSelectedItem();
            Cancha cancha;
            if (codigoSeleccionado == null) {
                cancha = servicio.registrar(txtNombre.getText(), tipo, capacidad, apertura, cierre,
                        txtDescripcion.getText());
                mostrarInformacion("Cancha " + cancha.getCodigo() + " registrada.");
            } else {
                EstadoCancha estadoAnterior = servicio.obtener(codigoSeleccionado).getEstado();
                EstadoCancha estadoNuevo = (EstadoCancha) cboEstado.getSelectedItem();
                if (estadoAnterior.permiteReservas() && !estadoNuevo.permiteReservas()
                        && !confirmarReservasAfectadas(codigoSeleccionado)) {
                    return;
                }
                cancha = servicio.actualizar(codigoSeleccionado, txtNombre.getText(), tipo, capacidad,
                        estadoNuevo, apertura, cierre, txtDescripcion.getText());
                String mensaje = "Cancha " + cancha.getCodigo() + " actualizada.";
                if (estadoAnterior.permiteReservas() && !cancha.permiteReservas()) {
                    mensaje += "\nYa no aparecerá en la consulta de disponibilidad.";
                }
                mostrarInformacion(mensaje);
            }
            cargarTabla();
            marcarEnTabla(cancha.getCodigo());
        } catch (CanchaProException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Canchas", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void marcarEnTabla(String codigo) {
        int filaModelo = modeloTabla.indiceDe(codigo);
        if (filaModelo >= 0) {
            int filaVista = tblCanchas.convertRowIndexToView(filaModelo);
            tblCanchas.setRowSelectionInterval(filaVista, filaVista);
        }
    }

    private void mostrarInformacion(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Canchas", JOptionPane.INFORMATION_MESSAGE);
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
        scrCanchas = new javax.swing.JScrollPane();
        tblCanchas = new javax.swing.JTable();
        pnlFormulario = new javax.swing.JPanel();
        lblCodigo = new javax.swing.JLabel();
        lblCodigoValor = new javax.swing.JLabel();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lblTipo = new javax.swing.JLabel();
        cboTipo = new javax.swing.JComboBox<>();
        lblCapacidad = new javax.swing.JLabel();
        spnCapacidad = new javax.swing.JSpinner();
        lblApertura = new javax.swing.JLabel();
        cboApertura = new javax.swing.JComboBox<>();
        lblCierre = new javax.swing.JLabel();
        cboCierre = new javax.swing.JComboBox<>();
        lblEstado = new javax.swing.JLabel();
        cboEstado = new javax.swing.JComboBox<>();
        lblDescripcion = new javax.swing.JLabel();
        txtDescripcion = new javax.swing.JTextField();
        lblAyuda = new javax.swing.JLabel();
        pnlBotones = new javax.swing.JPanel();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        lblTitulo.setText("Canchas");
        add(lblTitulo, java.awt.BorderLayout.NORTH);

        tblCanchas.setRowHeight(24);
        scrCanchas.setViewportView(tblCanchas);

        add(scrCanchas, java.awt.BorderLayout.CENTER);

        pnlFormulario.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos de la cancha"));
        pnlFormulario.setLayout(new java.awt.GridBagLayout());

        lblCodigo.setText("Código:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblCodigo, gridBagConstraints);

        lblCodigoValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblCodigoValor.setText("(nueva)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(lblCodigoValor, gridBagConstraints);

        lblNombre.setText("Nombre:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblNombre, gridBagConstraints);

        txtNombre.setColumns(18);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtNombre, gridBagConstraints);

        lblTipo.setText("Tipo:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblTipo, gridBagConstraints);

        cboTipo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboTipoActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboTipo, gridBagConstraints);

        lblCapacidad.setText("Capacidad (jugadores):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblCapacidad, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(spnCapacidad, gridBagConstraints);

        lblApertura.setText("Hora de apertura:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblApertura, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboApertura, gridBagConstraints);

        lblCierre.setText("Hora de cierre:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblCierre, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboCierre, gridBagConstraints);

        lblEstado.setText("Estado:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblEstado, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboEstado, gridBagConstraints);

        lblDescripcion.setText("Descripción:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblDescripcion, gridBagConstraints);

        txtDescripcion.setColumns(18);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtDescripcion, gridBagConstraints);

        lblAyuda.setForeground(new java.awt.Color(128, 128, 128));
        lblAyuda.setText("<html>Las canchas no se eliminan: para retirarla cambie su estado a <b>Inactiva</b>.<br>En mantenimiento o inactiva no aparece como disponible.</html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 8, 8, 8);
        pnlFormulario.add(lblAyuda, gridBagConstraints);

        add(pnlFormulario, java.awt.BorderLayout.EAST);

        pnlBotones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btnNuevo.setText("Nueva");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });
        pnlBotones.add(btnNuevo);

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        pnlBotones.add(btnGuardar);

        add(pnlBotones, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        limpiarFormulario();
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardar();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void cboTipoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboTipoActionPerformed
        // Al registrar una cancha nueva se propone la capacidad típica del tipo elegido.
        if (codigoSeleccionado == null && cboTipo.getSelectedItem() instanceof TipoCancha tipo) {
            spnCapacidad.setValue(tipo.getCapacidadSugerida());
        }
    }//GEN-LAST:event_cboTipoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JComboBox<java.time.LocalTime> cboApertura;
    private javax.swing.JComboBox<java.time.LocalTime> cboCierre;
    private javax.swing.JComboBox<EstadoCancha> cboEstado;
    private javax.swing.JComboBox<TipoCancha> cboTipo;
    private javax.swing.JLabel lblApertura;
    private javax.swing.JLabel lblAyuda;
    private javax.swing.JLabel lblCapacidad;
    private javax.swing.JLabel lblCierre;
    private javax.swing.JLabel lblCodigo;
    private javax.swing.JLabel lblCodigoValor;
    private javax.swing.JLabel lblDescripcion;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblTipo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel pnlBotones;
    private javax.swing.JPanel pnlFormulario;
    private javax.swing.JScrollPane scrCanchas;
    private javax.swing.JSpinner spnCapacidad;
    private javax.swing.JTable tblCanchas;
    private javax.swing.JTextField txtDescripcion;
    private javax.swing.JTextField txtNombre;
    // End of variables declaration//GEN-END:variables
}
