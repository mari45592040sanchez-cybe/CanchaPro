package canchapro.usuarios;

import canchapro.login.Sesion;
import canchapro.util.excepciones.CanchaProException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

/**
 * Pantalla del administrador para registrar empleados (u otros
 * administradores) y elegir los módulos que cada empleado podrá ver.
 */
public class PnlUsuarios extends javax.swing.JPanel {

    private final UsuarioServicio servicio;
    private final Sesion sesion;
    private final UsuarioTableModel modeloTabla = new UsuarioTableModel();
    private final Map<Modulo, JCheckBox> casillasModulos = new EnumMap<>(Modulo.class);
    /** Código del usuario en edición; null cuando se registra uno nuevo. */
    private String codigoSeleccionado;

    public PnlUsuarios(UsuarioServicio servicio, Sesion sesion) {
        this.servicio = servicio;
        this.sesion = sesion;
        initComponents();

        cboRol.setModel(new DefaultComboBoxModel<>(Rol.values()));
        tblUsuarios.setModel(modeloTabla);
        tblUsuarios.setAutoCreateRowSorter(true);
        tblUsuarios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarFila();
            }
        });

        crearCasillasModulos();
        cargarTabla();
        limpiarFormulario();
    }

    private void crearCasillasModulos() {
        for (Modulo modulo : Modulo.values()) {
            if (!modulo.isSoloAdministrador()) {
                JCheckBox casilla = new JCheckBox(modulo.getNombreVisible());
                casillasModulos.put(modulo, casilla);
                pnlModulos.add(casilla);
            }
        }
    }

    private void cargarTabla() {
        modeloTabla.setUsuarios(servicio.listar());
    }

    private void seleccionarFila() {
        int fila = tblUsuarios.getSelectedRow();
        if (fila >= 0) {
            mostrarUsuario(modeloTabla.getUsuario(tblUsuarios.convertRowIndexToModel(fila)));
        }
    }

    private void mostrarUsuario(Usuario usuario) {
        codigoSeleccionado = usuario.getCodigo();
        lblCodigoValor.setText(usuario.getCodigo());
        cboRol.setSelectedItem(usuario.getRol());
        cboRol.setEnabled(false);
        txtNombre.setText(usuario.getNombre());
        txtTelefono.setText(usuario.getTelefono());
        txtCorreo.setText(usuario.getCorreo());
        txtUsername.setText(usuario.getUsername());
        txtPassword.setText("");
        txtPassword.setEnabled(false);
        lblEstadoValor.setText(usuario.isActivo() ? "Activo" : "Inactivo");

        Set<Modulo> asignados = usuario instanceof Empleado empleado ? empleado.getModulos() : Set.of();
        casillasModulos.forEach((modulo, casilla) -> casilla.setSelected(asignados.contains(modulo)));
        habilitarCasillas(usuario.getRol() == Rol.EMPLEADO);

        btnActivarDesactivar.setText(usuario.isActivo() ? "Desactivar" : "Activar");
        btnActivarDesactivar.setEnabled(true);
        btnRestablecerPassword.setEnabled(true);
    }

    private void limpiarFormulario() {
        codigoSeleccionado = null;
        tblUsuarios.clearSelection();
        lblCodigoValor.setText("(nuevo)");
        cboRol.setEnabled(true);
        cboRol.setSelectedItem(Rol.EMPLEADO);
        txtNombre.setText("");
        txtTelefono.setText("");
        txtCorreo.setText("");
        txtUsername.setText("");
        txtPassword.setText("");
        txtPassword.setEnabled(true);
        lblEstadoValor.setText("-");
        casillasModulos.values().forEach(casilla -> casilla.setSelected(false));
        habilitarCasillas(true);
        btnActivarDesactivar.setText("Desactivar");
        btnActivarDesactivar.setEnabled(false);
        btnRestablecerPassword.setEnabled(false);
        txtNombre.requestFocusInWindow();
    }

    /** Los administradores ven todos los módulos, por eso no se eligen. */
    private void habilitarCasillas(boolean habilitar) {
        casillasModulos.values().forEach(casilla -> casilla.setEnabled(habilitar));
    }

    private Set<Modulo> modulosMarcados() {
        Set<Modulo> marcados = EnumSet.noneOf(Modulo.class);
        casillasModulos.forEach((modulo, casilla) -> {
            if (casilla.isSelected()) {
                marcados.add(modulo);
            }
        });
        return marcados;
    }

    private void guardar() {
        try {
            Usuario usuario;
            if (codigoSeleccionado == null) {
                usuario = servicio.registrar((Rol) cboRol.getSelectedItem(), txtNombre.getText(),
                        txtTelefono.getText(), txtCorreo.getText(), txtUsername.getText(),
                        new String(txtPassword.getPassword()), modulosMarcados());
                mostrarInformacion("Usuario " + usuario.getCodigo() + " registrado.\n"
                        + "Deberá cambiar su contraseña en el primer ingreso.");
            } else {
                usuario = servicio.actualizar(codigoSeleccionado, txtNombre.getText(),
                        txtTelefono.getText(), txtCorreo.getText(), txtUsername.getText(), modulosMarcados());
                mostrarInformacion("Usuario " + usuario.getCodigo() + " actualizado.");
            }
            recargarYSeleccionar(usuario.getCodigo());
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void activarDesactivar() {
        if (codigoSeleccionado == null) {
            return;
        }
        try {
            Usuario usuario = servicio.obtener(codigoSeleccionado);
            boolean activar = !usuario.isActivo();
            String accion = activar ? "activar" : "desactivar";
            int opcion = JOptionPane.showConfirmDialog(this,
                    "¿Desea " + accion + " al usuario " + usuario.getUsername() + "?",
                    "Confirmar", JOptionPane.YES_NO_OPTION);
            if (opcion == JOptionPane.YES_OPTION) {
                servicio.cambiarEstado(codigoSeleccionado, activar, sesion.getUsuario());
                recargarYSeleccionar(codigoSeleccionado);
            }
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void restablecerPassword() {
        if (codigoSeleccionado == null) {
            return;
        }
        try {
            Usuario usuario = servicio.obtener(codigoSeleccionado);
            int opcion = JOptionPane.showConfirmDialog(this,
                    "Se generará una contraseña temporal para " + usuario.getUsername() + ".\n¿Desea continuar?",
                    "Restablecer contraseña", JOptionPane.YES_NO_OPTION);
            if (opcion == JOptionPane.YES_OPTION) {
                String temporal = servicio.restablecerPassword(codigoSeleccionado);
                // Campo de solo lectura para que el administrador pueda copiarla.
                JTextField campo = new JTextField(temporal);
                campo.setEditable(false);
                JOptionPane.showMessageDialog(this,
                        new Object[]{"Contraseña temporal de " + usuario.getUsername() + ":", campo,
                            "El usuario deberá cambiarla al ingresar."},
                        "Restablecer contraseña", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void recargarYSeleccionar(String codigo) {
        cargarTabla();
        int filaModelo = modeloTabla.indiceDe(codigo);
        if (filaModelo >= 0) {
            int filaVista = tblUsuarios.convertRowIndexToView(filaModelo);
            tblUsuarios.setRowSelectionInterval(filaVista, filaVista);
            tblUsuarios.scrollRectToVisible(tblUsuarios.getCellRect(filaVista, 0, true));
        }
    }

    private void mostrarInformacion(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Usuarios", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Usuarios", JOptionPane.WARNING_MESSAGE);
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
        scrUsuarios = new javax.swing.JScrollPane();
        tblUsuarios = new javax.swing.JTable();
        pnlFormulario = new javax.swing.JPanel();
        lblCodigo = new javax.swing.JLabel();
        lblCodigoValor = new javax.swing.JLabel();
        lblRol = new javax.swing.JLabel();
        cboRol = new javax.swing.JComboBox<>();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lblTelefono = new javax.swing.JLabel();
        txtTelefono = new javax.swing.JTextField();
        lblCorreo = new javax.swing.JLabel();
        txtCorreo = new javax.swing.JTextField();
        lblUsername = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        lblPassword = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        lblEstado = new javax.swing.JLabel();
        lblEstadoValor = new javax.swing.JLabel();
        pnlModulos = new javax.swing.JPanel();
        pnlBotones = new javax.swing.JPanel();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnActivarDesactivar = new javax.swing.JButton();
        btnRestablecerPassword = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        lblTitulo.setText("Gestión de usuarios");
        add(lblTitulo, java.awt.BorderLayout.NORTH);

        tblUsuarios.setRowHeight(24);
        scrUsuarios.setViewportView(tblUsuarios);

        add(scrUsuarios, java.awt.BorderLayout.CENTER);

        pnlFormulario.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos del usuario"));
        pnlFormulario.setLayout(new java.awt.GridBagLayout());

        lblCodigo.setText("Código:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblCodigo, gridBagConstraints);

        lblCodigoValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblCodigoValor.setText("(nuevo)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(lblCodigoValor, gridBagConstraints);

        lblRol.setText("Rol:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblRol, gridBagConstraints);

        cboRol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboRolActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboRol, gridBagConstraints);

        lblNombre.setText("Nombre completo:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblNombre, gridBagConstraints);

        txtNombre.setColumns(18);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtNombre, gridBagConstraints);

        lblTelefono.setText("Teléfono:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblTelefono, gridBagConstraints);

        txtTelefono.setColumns(18);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtTelefono, gridBagConstraints);

        lblCorreo.setText("Correo:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblCorreo, gridBagConstraints);

        txtCorreo.setColumns(18);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtCorreo, gridBagConstraints);

        lblUsername.setText("Usuario (login):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblUsername, gridBagConstraints);

        txtUsername.setColumns(18);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtUsername, gridBagConstraints);

        lblPassword.setText("Contraseña inicial:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblPassword, gridBagConstraints);

        txtPassword.setColumns(18);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtPassword, gridBagConstraints);

        lblEstado.setText("Estado:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblEstado, gridBagConstraints);

        lblEstadoValor.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(lblEstadoValor, gridBagConstraints);

        pnlModulos.setBorder(javax.swing.BorderFactory.createTitledBorder("Módulos visibles para el empleado"));
        pnlModulos.setLayout(new javax.swing.BoxLayout(pnlModulos, javax.swing.BoxLayout.Y_AXIS));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 8, 8, 8);
        pnlFormulario.add(pnlModulos, gridBagConstraints);

        add(pnlFormulario, java.awt.BorderLayout.EAST);

        pnlBotones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        btnNuevo.setText("Nuevo");
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

        btnActivarDesactivar.setText("Desactivar");
        btnActivarDesactivar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActivarDesactivarActionPerformed(evt);
            }
        });
        pnlBotones.add(btnActivarDesactivar);

        btnRestablecerPassword.setText("Restablecer contraseña");
        btnRestablecerPassword.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRestablecerPasswordActionPerformed(evt);
            }
        });
        pnlBotones.add(btnRestablecerPassword);

        add(pnlBotones, java.awt.BorderLayout.SOUTH);
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

    private void btnRestablecerPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRestablecerPasswordActionPerformed
        restablecerPassword();
    }//GEN-LAST:event_btnRestablecerPasswordActionPerformed

    private void cboRolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboRolActionPerformed
        if (codigoSeleccionado == null) {
            habilitarCasillas(cboRol.getSelectedItem() == Rol.EMPLEADO);
        }
    }//GEN-LAST:event_cboRolActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnActivarDesactivar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JButton btnRestablecerPassword;
    private javax.swing.JComboBox<Rol> cboRol;
    private javax.swing.JLabel lblCodigo;
    private javax.swing.JLabel lblCodigoValor;
    private javax.swing.JLabel lblCorreo;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblEstadoValor;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblPassword;
    private javax.swing.JLabel lblRol;
    private javax.swing.JLabel lblTelefono;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblUsername;
    private javax.swing.JPanel pnlBotones;
    private javax.swing.JPanel pnlFormulario;
    private javax.swing.JPanel pnlModulos;
    private javax.swing.JScrollPane scrUsuarios;
    private javax.swing.JTable tblUsuarios;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtCorreo;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtTelefono;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
