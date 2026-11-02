package canchapro.tienda;

import canchapro.principal.Refrescable;
import canchapro.util.Validador;
import canchapro.util.excepciones.CanchaProException;
import canchapro.util.excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

/**
 * Catálogo de productos de la tienda e inventario.
 */
// POO - Herencia: la pantalla extiende JPanel de Swing.
// POO - Relaciones: usa TiendaServicio para las reglas de negocio.
public class PnlProductos extends javax.swing.JPanel implements Refrescable {

    private final TiendaServicio servicio;
    private final ProductoTableModel modeloTabla = new ProductoTableModel();
    /** Código del producto en edición; null cuando se registra uno nuevo. */
    private String codigoSeleccionado;

    public PnlProductos(TiendaServicio servicio) {
        this.servicio = servicio;
        initComponents();

        cboCategoria.setModel(new DefaultComboBoxModel<>(CategoriaProducto.values()));
        spnStockMinimo.setModel(new SpinnerNumberModel(5, 0, 100_000, 1));
        tblProductos.setModel(modeloTabla);
        tblProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        StockBajoRenderer renderer = new StockBajoRenderer(modeloTabla::getProducto);
        tblProductos.setDefaultRenderer(Object.class, renderer);
        tblProductos.setDefaultRenderer(Integer.class, renderer);
        tblProductos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarFila();
            }
        });

        cargarTabla();
        limpiarFormulario();
    }

    @Override
    public void refrescar() {
        cargarTabla();
        if (codigoSeleccionado != null) {
            seleccionar(codigoSeleccionado);
        }
    }

    private void cargarTabla() {
        Producto[] productos = servicio.listarProductos();
        modeloTabla.setProductos(productos);
        int bajos = servicio.productosConStockBajo().length;
        lblStockBajo.setText(bajos == 0 ? " " : bajos + " producto(s) con stock bajo");
    }

    private void seleccionarFila() {
        int fila = tblProductos.getSelectedRow();
        if (fila >= 0) {
            mostrarProducto(modeloTabla.getProducto(fila));
        }
    }

    private void mostrarProducto(Producto p) {
        codigoSeleccionado = p.getCodigo();
        lblCodigoValor.setText(p.getCodigo());
        txtNombre.setText(p.getNombre());
        cboCategoria.setSelectedItem(p.getCategoria());
        txtPrecio.setText(p.getPrecio().toPlainString());
        lblExistencia.setText("Existencia:");
        txtExistencia.setText(String.valueOf(p.getExistencia()));
        txtExistencia.setEditable(false);
        spnStockMinimo.setValue(p.getStockMinimo());
        lblEstadoValor.setText(p.isActivo() ? "Activo" : "Inactivo");
        btnActivarDesactivar.setText(p.isActivo() ? "Desactivar" : "Activar");
        btnActivarDesactivar.setEnabled(true);
        btnReponer.setEnabled(true);
    }

    private void limpiarFormulario() {
        codigoSeleccionado = null;
        tblProductos.clearSelection();
        lblCodigoValor.setText("(nuevo)");
        txtNombre.setText("");
        cboCategoria.setSelectedIndex(0);
        txtPrecio.setText("");
        lblExistencia.setText("Existencia inicial:");
        txtExistencia.setText("0");
        txtExistencia.setEditable(true);
        spnStockMinimo.setValue(5);
        lblEstadoValor.setText("-");
        btnActivarDesactivar.setText("Desactivar");
        btnActivarDesactivar.setEnabled(false);
        btnReponer.setEnabled(false);
        txtNombre.requestFocusInWindow();
    }

    private void guardar() {
        try {
            BigDecimal precio = Validador.monto(txtPrecio.getText(), "El precio");
            CategoriaProducto categoria = (CategoriaProducto) cboCategoria.getSelectedItem();
            int minimo = (Integer) spnStockMinimo.getValue();
            Producto p = codigoSeleccionado == null
                    ? servicio.registrarProducto(txtNombre.getText(), categoria, precio,
                            entero(txtExistencia.getText(), "La existencia inicial"), minimo)
                    : servicio.actualizarProducto(codigoSeleccionado, txtNombre.getText(), categoria, precio, minimo);
            JOptionPane.showMessageDialog(this, "Producto " + p.getCodigo() + " guardado.",
                    "Productos", JOptionPane.INFORMATION_MESSAGE);
            cargarTabla();
            seleccionar(p.getCodigo());
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void reponer() {
        if (codigoSeleccionado == null) {
            return;
        }
        Producto p = servicio.obtenerProducto(codigoSeleccionado);
        String texto = JOptionPane.showInputDialog(this, "Unidades que ingresan de " + p.getNombre()
                + " (existencia actual: " + p.getExistencia() + "):", "Reponer inventario", JOptionPane.QUESTION_MESSAGE);
        if (texto == null) {
            return;
        }
        try {
            Producto actualizado = servicio.reponerInventario(codigoSeleccionado, entero(texto, "La cantidad"));
            cargarTabla();
            seleccionar(actualizado.getCodigo());
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void activarDesactivar() {
        if (codigoSeleccionado == null) {
            return;
        }
        try {
            Producto p = servicio.obtenerProducto(codigoSeleccionado);
            servicio.cambiarEstadoProducto(codigoSeleccionado, !p.isActivo());
            cargarTabla();
            seleccionar(p.getCodigo());
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void seleccionar(String codigo) {
        int fila = modeloTabla.indiceDe(codigo);
        if (fila >= 0) {
            tblProductos.setRowSelectionInterval(fila, fila);
        }
    }

    private static int entero(String texto, String campo) {
        try {
            return Integer.parseInt(Validador.requerido(texto, campo));
        } catch (NumberFormatException e) {
            throw new DatosInvalidosException(campo + " debe ser un número entero.");
        }
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Productos", JOptionPane.WARNING_MESSAGE);
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

        scrProductos = new javax.swing.JScrollPane();
        tblProductos = new javax.swing.JTable();
        pnlFormulario = new javax.swing.JPanel();
        lblCodigo = new javax.swing.JLabel();
        lblCodigoValor = new javax.swing.JLabel();
        lblNombre = new javax.swing.JLabel();
        txtNombre = new javax.swing.JTextField();
        lblCategoria = new javax.swing.JLabel();
        cboCategoria = new javax.swing.JComboBox<>();
        lblPrecio = new javax.swing.JLabel();
        txtPrecio = new javax.swing.JTextField();
        lblExistencia = new javax.swing.JLabel();
        txtExistencia = new javax.swing.JTextField();
        lblStockMinimo = new javax.swing.JLabel();
        spnStockMinimo = new javax.swing.JSpinner();
        lblEstado = new javax.swing.JLabel();
        lblEstadoValor = new javax.swing.JLabel();
        lblAyuda = new javax.swing.JLabel();
        pnlBotones = new javax.swing.JPanel();
        lblStockBajo = new javax.swing.JLabel();
        pnlAcciones = new javax.swing.JPanel();
        btnNuevo = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnReponer = new javax.swing.JButton();
        btnActivarDesactivar = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        tblProductos.setRowHeight(24);
        scrProductos.setViewportView(tblProductos);

        add(scrProductos, java.awt.BorderLayout.CENTER);

        pnlFormulario.setBorder(javax.swing.BorderFactory.createTitledBorder("Datos del producto"));
        pnlFormulario.setLayout(new java.awt.GridBagLayout());
        lblCodigo.setText("Código:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblCodigo, gridBagConstraints);

        lblCodigoValor.setText("(nuevo)");
        lblCodigoValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
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

        lblCategoria.setText("Categoría:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblCategoria, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(cboCategoria, gridBagConstraints);

        lblPrecio.setText("Precio (Q):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblPrecio, gridBagConstraints);

        txtPrecio.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtPrecio, gridBagConstraints);

        lblExistencia.setText("Existencia inicial:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblExistencia, gridBagConstraints);

        txtExistencia.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(txtExistencia, gridBagConstraints);

        lblStockMinimo.setText("Stock mínimo:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlFormulario.add(lblStockMinimo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlFormulario.add(spnStockMinimo, gridBagConstraints);

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

        lblAyuda.setText("<html>La existencia solo cambia al vender o con <b>Reponer</b>.<br>Las filas en rojo tienen stock bajo (existencia &le; mínimo).<br>Los productos no se eliminan: desactívelos.</html>");
        lblAyuda.setForeground(new java.awt.Color(128, 128, 128));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 8, 8, 8);
        pnlFormulario.add(lblAyuda, gridBagConstraints);

        add(pnlFormulario, java.awt.BorderLayout.EAST);

        pnlBotones.setLayout(new java.awt.BorderLayout());
        lblStockBajo.setText(" ");
        lblStockBajo.setForeground(new java.awt.Color(185, 28, 28));
        pnlBotones.add(lblStockBajo, java.awt.BorderLayout.WEST);

        pnlAcciones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        btnNuevo.setText("Nuevo");
        btnNuevo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNuevoActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnNuevo);

        btnGuardar.setText("Guardar");
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnGuardar);

        btnReponer.setText("Reponer inventario…");
        btnReponer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReponerActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnReponer);

        btnActivarDesactivar.setText("Desactivar");
        btnActivarDesactivar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnActivarDesactivarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnActivarDesactivar);

        pnlBotones.add(pnlAcciones, java.awt.BorderLayout.EAST);

        add(pnlBotones, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnNuevoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNuevoActionPerformed
        limpiarFormulario();
    }//GEN-LAST:event_btnNuevoActionPerformed

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        guardar();
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnReponerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReponerActionPerformed
        reponer();
    }//GEN-LAST:event_btnReponerActionPerformed

    private void btnActivarDesactivarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnActivarDesactivarActionPerformed
        activarDesactivar();
    }//GEN-LAST:event_btnActivarDesactivarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnActivarDesactivar;
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnNuevo;
    private javax.swing.JButton btnReponer;
    private javax.swing.JComboBox<CategoriaProducto> cboCategoria;
    private javax.swing.JLabel lblAyuda;
    private javax.swing.JLabel lblCategoria;
    private javax.swing.JLabel lblCodigo;
    private javax.swing.JLabel lblCodigoValor;
    private javax.swing.JLabel lblEstado;
    private javax.swing.JLabel lblEstadoValor;
    private javax.swing.JLabel lblExistencia;
    private javax.swing.JLabel lblNombre;
    private javax.swing.JLabel lblPrecio;
    private javax.swing.JLabel lblStockBajo;
    private javax.swing.JLabel lblStockMinimo;
    private javax.swing.JPanel pnlAcciones;
    private javax.swing.JPanel pnlBotones;
    private javax.swing.JPanel pnlFormulario;
    private javax.swing.JScrollPane scrProductos;
    private javax.swing.JSpinner spnStockMinimo;
    private javax.swing.JTable tblProductos;
    private javax.swing.JTextField txtExistencia;
    private javax.swing.JTextField txtNombre;
    private javax.swing.JTextField txtPrecio;
    // End of variables declaration//GEN-END:variables
}
