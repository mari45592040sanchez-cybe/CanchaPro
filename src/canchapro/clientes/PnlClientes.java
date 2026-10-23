package canchapro.clientes;

import canchapro.util.FormatoFecha;
import canchapro.util.Validador;
import canchapro.util.excepciones.CanchaProException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Pantalla de clientes: búsqueda en vivo, registro, edición y
 * activación/desactivación.
 *
 * También se reutiliza dentro de {@link DlgBuscarCliente} en "modo
 * selección" para elegir el cliente de una reserva o venta.
 */
// POO - Herencia: la pantalla extiende JPanel (hereda todo el comportamiento de un panel de Swing).
// POO - Reutilización: este mismo panel se usa dentro de DlgBuscarCliente en "modo selección".
public class PnlClientes extends javax.swing.JPanel {

    // POO - Relaciones / Responsabilidad: la pantalla usa el servicio para las reglas de negocio;
    // ella solo muestra datos y responde a los botones.
    private final ClienteServicio servicio;
    private final ClienteTableModel modeloTabla = new ClienteTableModel();
    /** Código del cliente en edición; null cuando se registra uno nuevo. */
    private String codigoSeleccionado;
    private Consumer<Cliente> alHacerDobleClic = cliente -> { };
    /** Evita recargar el formulario cuando la selección cambia por código. */
    private boolean marcandoFila;

    public PnlClientes(ClienteServicio servicio) {
        this.servicio = servicio;
        initComponents();

        tblClientes.setModel(modeloTabla);
        tblClientes.setAutoCreateRowSorter(true);
        tblClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !marcandoFila) {
                seleccionarFila();
            }
        });
        tblClientes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblClientes.getSelectedRow() >= 0) {
                    getClienteSeleccionado().ifPresent(alHacerDobleClic);
                }
            }
        });
        txtBuscar.getDocument().addDocumentListener(alCambiar(this::cargarTabla));
        txtTelefono.getDocument().addDocumentListener(alCambiar(this::verificarTelefono));

        cargarTabla();
        limpiarFormulario();
    }

    // --- API para reutilizar el panel en DlgBuscarCliente ---

    /**
     * Oculta las opciones de administración (inactivos y desactivar):
     * en modo selección solo se buscan, registran o editan clientes activos.
     */
    public void activarModoSeleccion() {
        chkMostrarInactivos.setSelected(false);
        chkMostrarInactivos.setVisible(false);
        btnActivarDesactivar.setVisible(false);
        cargarTabla();
    }

    public Optional<Cliente> getClienteSeleccionado() {
        return codigoSeleccionado == null ? Optional.empty() : Optional.of(servicio.obtener(codigoSeleccionado));
    }

    public void setAlHacerDobleClic(Consumer<Cliente> accion) {
        this.alHacerDobleClic = accion;
    }

    public void enfocarBusqueda() {
        txtBuscar.requestFocusInWindow();
    }

    // --- Lógica de la pantalla ---

    private void cargarTabla() {
        // POO - Colecciones: el servicio devuelve un arreglo de clientes.
        Cliente[] clientes = servicio.buscar(txtBuscar.getText(), chkMostrarInactivos.isSelected());
        modeloTabla.setClientes(clientes);
        lblResultados.setText(clientes.length + (clientes.length == 1 ? " cliente" : " clientes"));
        if (codigoSeleccionado != null) {
            marcarEnTabla(codigoSeleccionado);
        }
    }

    private void seleccionarFila() {
        int fila = tblClientes.getSelectedRow();
        if (fila >= 0) {
            mostrarCliente(modeloTabla.getCliente(tblClientes.convertRowIndexToModel(fila)));
        }
    }

    private void mostrarCliente(Cliente cliente) {
        codigoSeleccionado = cliente.getCodigo();
        lblCodigoValor.setText(cliente.getCodigo());
        txtNombre.setText(cliente.getNombre());
        txtTelefono.setText(cliente.getTelefono());
        txtCorreo.setText(cliente.getCorreo());
        lblFechaRegistroValor.setText(FormatoFecha.vista(cliente.getFechaRegistro()));
        lblEstadoValor.setText(cliente.isActivo() ? "Activo" : "Inactivo");
        btnActivarDesactivar.setText(cliente.isActivo() ? "Desactivar" : "Activar");
        btnActivarDesactivar.setEnabled(true);
    }

    private void limpiarFormulario() {
        codigoSeleccionado = null;
        tblClientes.clearSelection();
        lblCodigoValor.setText("(nuevo)");
        txtNombre.setText("");
        txtTelefono.setText("");
        txtCorreo.setText("");
        lblFechaRegistroValor.setText("-");
        lblEstadoValor.setText("-");
        btnActivarDesactivar.setText("Desactivar");
        btnActivarDesactivar.setEnabled(false);
        txtNombre.requestFocusInWindow();
    }

    /**
     * Mientras se escribe el teléfono, avisa si ya pertenece a otro cliente
     * (ayuda a no registrar duplicados antes de presionar Guardar).
     */
    private void verificarTelefono() {
        String telefono = txtTelefono.getText().trim();
        String aviso = " ";
        String detalle = null;
        if (!telefono.isEmpty() && !Validador.telefonoValido(telefono)) {
            aviso = "Debe tener 8 dígitos";
        } else if (!telefono.isEmpty()) {
            Optional<Cliente> existente = servicio.buscarPorTelefono(telefono)
                    .filter(c -> !c.getCodigo().equals(codigoSeleccionado));
            if (existente.isPresent()) {
                Cliente c = existente.get();
                aviso = "Ya pertenece a " + c.getCodigo() + (c.isActivo() ? "" : " (inactivo)");
                detalle = c.getDescripcion();
            }
        }
        lblAvisoTelefono.setText(aviso);
        lblAvisoTelefono.setToolTipText(detalle);
    }

    private void guardar() {
        try {
            Cliente cliente;
            if (codigoSeleccionado == null) {
                cliente = servicio.registrar(txtNombre.getText(), txtTelefono.getText(), txtCorreo.getText());
                mostrarInformacion("Cliente " + cliente.getCodigo() + " registrado.");
            } else {
                cliente = servicio.actualizar(codigoSeleccionado, txtNombre.getText(),
                        txtTelefono.getText(), txtCorreo.getText());
                mostrarInformacion("Cliente " + cliente.getCodigo() + " actualizado.");
            }
            mostrarCliente(cliente);
            cargarTabla();
            if (modeloTabla.indiceDe(cliente.getCodigo()) < 0) {
                // El filtro de búsqueda lo oculta: se limpia para mostrarlo.
                txtBuscar.setText("");
            }
            marcarEnTabla(cliente.getCodigo());
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void activarDesactivar() {
        if (codigoSeleccionado == null) {
            return;
        }
        try {
            Cliente cliente = servicio.obtener(codigoSeleccionado);
            boolean activar = !cliente.isActivo();
            String mensaje = activar
                    ? "¿Desea activar al cliente " + cliente.getNombre() + "?"
                    : "¿Desea desactivar al cliente " + cliente.getNombre() + "?\n"
                    + "No aparecerá al crear reservas, pero se conserva su historial.";
            if (JOptionPane.showConfirmDialog(this, mensaje, "Confirmar", JOptionPane.YES_NO_OPTION)
                    != JOptionPane.YES_OPTION) {
                return;
            }
            servicio.cambiarEstado(codigoSeleccionado, activar);
            cargarTabla();
            if (modeloTabla.indiceDe(cliente.getCodigo()) < 0) {
                limpiarFormulario();
            } else {
                mostrarCliente(servicio.obtener(cliente.getCodigo()));
                marcarEnTabla(cliente.getCodigo());
            }
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    /**
     * Resalta la fila del cliente sin volver a llenar el formulario, para no
     * perder lo que el usuario esté escribiendo.
     */
    private void marcarEnTabla(String codigo) {
        int filaModelo = modeloTabla.indiceDe(codigo);
        if (filaModelo >= 0) {
            int filaVista = tblClientes.convertRowIndexToView(filaModelo);
            marcandoFila = true;
            try {
                tblClientes.setRowSelectionInterval(filaVista, filaVista);
            } finally {
                marcandoFila = false;
            }
            tblClientes.scrollRectToVisible(tblClientes.getCellRect(filaVista, 0, true));
        }
    }

    private void mostrarInformacion(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Clientes", JOptionPane.INFORMATION_MESSAGE);
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Clientes", JOptionPane.WARNING_MESSAGE);
    }

    private static DocumentListener alCambiar(Runnable accion) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                accion.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                accion.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                accion.run();
            }
        };
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
        chkMostrarInactivos = new javax.swing.JCheckBox();
        lblResultados = new javax.swing.JLabel();
        scrClientes = new javax.swing.JScrollPane();
        tblClientes = new javax.swing.JTable();
        pnlFormulario = new javax.swing.JPanel();
        lblCodigo = new javax.swing.JLabel();
        lblCodigoValor = new javax.swing.JLabel();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lblTelefono = new javax.swing.JLabel();
        txtTelefono = new javax.swing.JTextField();
        lblAvisoTelefono = new javax.swing.JLabel();
        lblCorreo = new javax.swing.JLabel();
        txtCorreo = new javax.swing.JTextField();
        lblFechaRegistro = new javax.swing.JLabel();
        lblFechaRegistroValor = new javax.swing.JLabel();
        lblEstado = new javax.swing.JLabel();
        lblEstadoValor = new javax.swing.JLabel();
        pnlBotones = new javax.swing.JPanel();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnActivarDesactivar = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        pnlNorte.setLayout(new java.awt.BorderLayout(0, 8));

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        lblTitulo.setText("Clientes");
        pnlNorte.add(lblTitulo, java.awt.BorderLayout.NORTH);

        pnlBusqueda.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));

        lblBuscar.setText("Buscar (código, nombre o teléfono):");
        pnlBusqueda.add(lblBuscar);

        txtBuscar.setColumns(28);
        pnlBusqueda.add(txtBuscar);

        chkMostrarInactivos.setText("Mostrar inactivos");
        chkMostrarInactivos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkMostrarInactivosActionPerformed(evt);
            }
        });
        pnlBusqueda.add(chkMostrarInactivos);

        lblResultados.setForeground(new java.awt.Color(128, 128, 128));
        lblResultados.setText("0 clientes");
        pnlBusqueda.add(lblResultados);

        pnlNorte.add(pnlBusqueda, java.awt.BorderLayout.CENTER);

        add(pnlNorte, java.awt.BorderLayout.NORTH);

        tblClientes.setRowHeight(24);
        scrClientes.setViewportView(tblClientes);

        add(scrClientes, java.awt.BorderLayout.CENTER);

        pnlFormulario.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos del cliente"));
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

        lblNombre.setText("Nombre completo:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblNombre, gridBagConstraints);

        txtNombre.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtNombre, gridBagConstraints);

        lblTelefono.setText("Teléfono:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblTelefono, gridBagConstraints);

        txtTelefono.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 8);
        pnlFormulario.add(txtTelefono, gridBagConstraints);

        lblAvisoTelefono.setFont(new java.awt.Font("Segoe UI", 0, 11)); // NOI18N
        lblAvisoTelefono.setForeground(new java.awt.Color(185, 28, 28));
        lblAvisoTelefono.setText(" ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 4, 8);
        pnlFormulario.add(lblAvisoTelefono, gridBagConstraints);

        lblCorreo.setText("Correo (opcional):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblCorreo, gridBagConstraints);

        txtCorreo.setColumns(20);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtCorreo, gridBagConstraints);

        lblFechaRegistro.setText("Registrado:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblFechaRegistro, gridBagConstraints);

        lblFechaRegistroValor.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(lblFechaRegistroValor, gridBagConstraints);

        lblEstado.setText("Estado:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblEstado, gridBagConstraints);

        lblEstadoValor.setText("-");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(lblEstadoValor, gridBagConstraints);

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

    private void chkMostrarInactivosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkMostrarInactivosActionPerformed
        cargarTabla();
    }//GEN-LAST:event_chkMostrarInactivosActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnActivarDesactivar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JCheckBox chkMostrarInactivos;
    private javax.swing.JLabel lblAvisoTelefono;
    private javax.swing.JLabel lblBuscar;
    private javax.swing.JLabel lblCodigo;
    private javax.swing.JLabel lblCodigoValor;
    private javax.swing.JLabel lblCorreo;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblEstadoValor;
    private javax.swing.JLabel lblFechaRegistro;
    private javax.swing.JLabel lblFechaRegistroValor;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblResultados;
    private javax.swing.JLabel lblTelefono;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel pnlBotones;
    private javax.swing.JPanel pnlBusqueda;
    private javax.swing.JPanel pnlFormulario;
    private javax.swing.JPanel pnlNorte;
    private javax.swing.JScrollPane scrClientes;
    private javax.swing.JTable tblClientes;
    private javax.swing.JTextField txtBuscar;
    private javax.swing.JTextField txtCorreo;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtTelefono;
    // End of variables declaration//GEN-END:variables
}
