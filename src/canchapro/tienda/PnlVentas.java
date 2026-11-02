package canchapro.tienda;

import canchapro.clientes.Cliente;
import canchapro.clientes.ClienteServicio;
import canchapro.clientes.DlgBuscarCliente;
import canchapro.login.Sesion;
import canchapro.pagos.MetodoPago;
import canchapro.principal.Refrescable;
import canchapro.util.Dialogos;
import canchapro.util.FormatoFecha;
import canchapro.util.FormatoMoneda;
import canchapro.util.excepciones.CanchaProException;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Punto de venta de la tienda: elegir productos, armar el carrito, cobrar y
 * consultar o anular las ventas del día.
 */
// POO - Herencia: la pantalla extiende JPanel de Swing.
// POO - Relaciones: usa TiendaServicio y ClienteServicio (cliente opcional en la venta).
public class PnlVentas extends javax.swing.JPanel implements Refrescable {

    private final TiendaServicio servicio;
    private final ClienteServicio clienteServicio;
    private final Sesion sesion;
    private final ProductoTableModel modeloCatalogo = new ProductoTableModel();
    private final CarritoTableModel modeloCarrito = new CarritoTableModel();
    private final VentaTableModel modeloVentas = new VentaTableModel();
    private Cliente cliente;

    public PnlVentas(TiendaServicio servicio, ClienteServicio clienteServicio, Sesion sesion) {
        this.servicio = servicio;
        this.clienteServicio = clienteServicio;
        this.sesion = sesion;
        initComponents();

        spnCantidad.setModel(new SpinnerNumberModel(1, 1, 999, 1));
        cboMetodo.setModel(new DefaultComboBoxModel<>(MetodoPago.values()));

        tblCatalogo.setModel(modeloCatalogo);
        tblCatalogo.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        StockBajoRenderer renderer = new StockBajoRenderer(modeloCatalogo::getProducto);
        tblCatalogo.setDefaultRenderer(Object.class, renderer);
        tblCatalogo.setDefaultRenderer(Integer.class, renderer);
        tblCatalogo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    agregar();
                }
            }
        });
        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                cargarCatalogo();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                cargarCatalogo();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                cargarCatalogo();
            }
        });

        tblCarrito.setModel(modeloCarrito);
        tblCarrito.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scrCarrito.setPreferredSize(new Dimension(380, 220));
        tblVentas.setModel(modeloVentas);
        tblVentas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        refrescar();
    }

    @Override
    public void refrescar() {
        cargarCatalogo();
        cargarVentasDeHoy();
        actualizarTotal();
    }

    private void cargarCatalogo() {
        modeloCatalogo.setProductos(servicio.buscarProductosActivos(txtBuscar.getText()));
    }

    private void cargarVentasDeHoy() {
        modeloVentas.setVentas(servicio.ventasPorPeriodo(servicio.hoy(), servicio.hoy()));
        lblTotalHoy.setText("Vendido hoy: " + FormatoMoneda.formatear(servicio.totalVendidoHoy()));
    }

    private void actualizarTotal() {
        lblTotal.setText("Total: " + FormatoMoneda.formatear(modeloCarrito.getTotal()));
        btnConfirmar.setEnabled(modeloCarrito.getRowCount() > 0);
    }

    private void agregar() {
        int fila = tblCatalogo.getSelectedRow();
        if (fila < 0) {
            mostrarAdvertencia("Seleccione un producto de la lista.");
            return;
        }
        Producto producto = modeloCatalogo.getProducto(fila);
        int cantidad = (Integer) spnCantidad.getValue();
        try {
            // Se verifica contra lo que ya hay en el carrito.
            servicio.verificarStock(producto, modeloCarrito.cantidadDe(producto) + cantidad);
            modeloCarrito.agregar(producto, cantidad);
            spnCantidad.setValue(1);
            actualizarTotal();
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
        }
    }

    private void quitar() {
        int fila = tblCarrito.getSelectedRow();
        if (fila >= 0) {
            modeloCarrito.quitar(fila);
            actualizarTotal();
        }
    }

    private void confirmar() {
        try {
            Venta venta = servicio.registrarVenta(cliente, modeloCarrito.getItems(),
                    (MetodoPago) cboMetodo.getSelectedItem(), sesion.getUsuario());
            Dialogos.mostrarTexto(this, "Venta registrada", "Venta " + venta.getCodigo() + " registrada.", detalle(venta));
            modeloCarrito.vaciar();
            quitarCliente();
            refrescar();
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
            // El inventario pudo cambiar: se muestra la existencia actual.
            cargarCatalogo();
        }
    }

    private void verVenta() {
        int fila = tblVentas.getSelectedRow();
        if (fila >= 0) {
            Venta venta = modeloVentas.getVenta(fila);
            Dialogos.mostrarTexto(this, "Detalle de venta", venta.getCodigo() + " · " + venta.getEstado(), detalle(venta));
        }
    }

    private void anular() {
        int fila = tblVentas.getSelectedRow();
        if (fila < 0) {
            return;
        }
        Venta venta = modeloVentas.getVenta(fila);
        int opcion = JOptionPane.showConfirmDialog(this, "¿Anular la venta " + venta.getCodigo() + " por "
                + FormatoMoneda.formatear(venta.getTotal()) + "?\nLas unidades vuelven al inventario.",
                "Anular venta", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            try {
                servicio.anularVenta(venta.getCodigo());
                refrescar();
            } catch (CanchaProException e) {
                mostrarAdvertencia(e.getMessage());
            }
        }
    }

    private void elegirCliente() {
        DlgBuscarCliente.seleccionar(SwingUtilities.getWindowAncestor(this), clienteServicio).ifPresent(c -> {
            cliente = c;
            lblClienteValor.setText(c.getDescripcion());
        });
    }

    private void quitarCliente() {
        cliente = null;
        lblClienteValor.setText("(venta de mostrador)");
    }

    /** Texto con las líneas de una venta (letra de ancho fijo). */
    private static String detalle(Venta v) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fecha:    ").append(FormatoFecha.vista(v.getFechaHora())).append('\n');
        sb.append("Cliente:  ").append(v.getCliente() == null ? "(mostrador)" : v.getCliente().getDescripcion()).append('\n');
        sb.append("Atendió:  ").append(v.getUsuario().getNombre()).append('\n');
        sb.append("Pago:     ").append(v.getMetodoPago()).append("\n\n");
        for (DetalleVenta d : v.getDetalles()) {
            sb.append(String.format("%-26s %3d x %10s %12s%n", d.getProducto().getNombre(), d.getCantidad(),
                    FormatoMoneda.formatear(d.getPrecioUnitario()), FormatoMoneda.formatear(d.getSubtotal())));
        }
        sb.append("-".repeat(56)).append('\n');
        sb.append(String.format("%-43s %12s%n", "TOTAL", FormatoMoneda.formatear(v.getTotal())));
        return sb.toString();
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Tienda", JOptionPane.WARNING_MESSAGE);
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

        pnlIzquierda = new javax.swing.JPanel();
        pnlCatalogo = new javax.swing.JPanel();
        pnlBuscar = new javax.swing.JPanel();
        lblBuscar = new javax.swing.JLabel();
        txtBuscar = new javax.swing.JTextField();
        lblCantidad = new javax.swing.JLabel();
        spnCantidad = new javax.swing.JSpinner();
        btnAgregar = new javax.swing.JButton();
        scrCatalogo = new javax.swing.JScrollPane();
        tblCatalogo = new javax.swing.JTable();
        pnlHistorial = new javax.swing.JPanel();
        scrVentas = new javax.swing.JScrollPane();
        tblVentas = new javax.swing.JTable();
        pnlAccionesHistorial = new javax.swing.JPanel();
        lblTotalHoy = new javax.swing.JLabel();
        pnlBotonesHistorial = new javax.swing.JPanel();
        btnVerVenta = new javax.swing.JButton();
        btnAnular = new javax.swing.JButton();
        pnlCarrito = new javax.swing.JPanel();
        scrCarrito = new javax.swing.JScrollPane();
        tblCarrito = new javax.swing.JTable();
        btnQuitar = new javax.swing.JButton();
        lblTotal = new javax.swing.JLabel();
        lblCliente = new javax.swing.JLabel();
        lblClienteValor = new javax.swing.JLabel();
        pnlClienteBotones = new javax.swing.JPanel();
        btnCliente = new javax.swing.JButton();
        btnSinCliente = new javax.swing.JButton();
        lblMetodo = new javax.swing.JLabel();
        cboMetodo = new javax.swing.JComboBox<>();
        pnlAccionesVenta = new javax.swing.JPanel();
        btnVaciar = new javax.swing.JButton();
        btnConfirmar = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        pnlIzquierda.setLayout(new java.awt.GridBagLayout());
        pnlCatalogo.setBorder(javax.swing.BorderFactory.createTitledBorder("Productos (doble clic para agregar)"));
        pnlCatalogo.setLayout(new java.awt.BorderLayout(0, 6));
        pnlBuscar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        lblBuscar.setText("Buscar producto:");
        pnlBuscar.add(lblBuscar);

        txtBuscar.setColumns(18);
        pnlBuscar.add(txtBuscar);

        lblCantidad.setText("Cantidad:");
        pnlBuscar.add(lblCantidad);

        pnlBuscar.add(spnCantidad);

        btnAgregar.setText("Agregar al carrito");
        btnAgregar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarActionPerformed(evt);
            }
        });
        pnlBuscar.add(btnAgregar);

        pnlCatalogo.add(pnlBuscar, java.awt.BorderLayout.NORTH);

        tblCatalogo.setRowHeight(24);
        scrCatalogo.setViewportView(tblCatalogo);

        pnlCatalogo.add(scrCatalogo, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.6;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        pnlIzquierda.add(pnlCatalogo, gridBagConstraints);

        pnlHistorial.setBorder(javax.swing.BorderFactory.createTitledBorder("Ventas de hoy"));
        pnlHistorial.setLayout(new java.awt.BorderLayout(0, 4));
        tblVentas.setRowHeight(24);
        scrVentas.setViewportView(tblVentas);

        pnlHistorial.add(scrVentas, java.awt.BorderLayout.CENTER);

        pnlAccionesHistorial.setLayout(new java.awt.BorderLayout());
        lblTotalHoy.setText("Vendido hoy: -");
        lblTotalHoy.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pnlAccionesHistorial.add(lblTotalHoy, java.awt.BorderLayout.WEST);

        pnlBotonesHistorial.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        btnVerVenta.setText("Ver detalle");
        btnVerVenta.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerVentaActionPerformed(evt);
            }
        });
        pnlBotonesHistorial.add(btnVerVenta);

        btnAnular.setText("Anular venta…");
        btnAnular.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAnularActionPerformed(evt);
            }
        });
        pnlBotonesHistorial.add(btnAnular);

        pnlAccionesHistorial.add(pnlBotonesHistorial, java.awt.BorderLayout.EAST);

        pnlHistorial.add(pnlAccionesHistorial, java.awt.BorderLayout.SOUTH);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.4;
        pnlIzquierda.add(pnlHistorial, gridBagConstraints);

        add(pnlIzquierda, java.awt.BorderLayout.CENTER);

        pnlCarrito.setBorder(javax.swing.BorderFactory.createTitledBorder("Venta actual"));
        pnlCarrito.setLayout(new java.awt.GridBagLayout());
        tblCarrito.setRowHeight(24);
        scrCarrito.setViewportView(tblCarrito);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlCarrito.add(scrCarrito, gridBagConstraints);

        btnQuitar.setText("Quitar producto");
        btnQuitar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQuitarActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 8, 8);
        pnlCarrito.add(btnQuitar, gridBagConstraints);

        lblTotal.setText("Total: Q 0.00");
        lblTotal.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 10, 8);
        pnlCarrito.add(lblTotal, gridBagConstraints);

        lblCliente.setText("Cliente:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlCarrito.add(lblCliente, gridBagConstraints);

        lblClienteValor.setText("(venta de mostrador)");
        lblClienteValor.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlCarrito.add(lblClienteValor, gridBagConstraints);

        pnlClienteBotones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 0));
        btnCliente.setText("Elegir cliente…");
        btnCliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClienteActionPerformed(evt);
            }
        });
        pnlClienteBotones.add(btnCliente);

        btnSinCliente.setText("Sin cliente");
        btnSinCliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSinClienteActionPerformed(evt);
            }
        });
        pnlClienteBotones.add(btnSinCliente);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 8);
        pnlCarrito.add(pnlClienteBotones, gridBagConstraints);

        lblMetodo.setText("Método de pago:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlCarrito.add(lblMetodo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlCarrito.add(cboMetodo, gridBagConstraints);

        pnlAccionesVenta.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        btnVaciar.setText("Vaciar");
        btnVaciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVaciarActionPerformed(evt);
            }
        });
        pnlAccionesVenta.add(btnVaciar);

        btnConfirmar.setText("Confirmar venta");
        btnConfirmar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmarActionPerformed(evt);
            }
        });
        pnlAccionesVenta.add(btnConfirmar);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 4, 0);
        pnlCarrito.add(pnlAccionesVenta, gridBagConstraints);

        add(pnlCarrito, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    private void btnAgregarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarActionPerformed
        agregar();
    }//GEN-LAST:event_btnAgregarActionPerformed

    private void btnVerVentaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerVentaActionPerformed
        verVenta();
    }//GEN-LAST:event_btnVerVentaActionPerformed

    private void btnAnularActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnularActionPerformed
        anular();
    }//GEN-LAST:event_btnAnularActionPerformed

    private void btnQuitarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQuitarActionPerformed
        quitar();
    }//GEN-LAST:event_btnQuitarActionPerformed

    private void btnClienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClienteActionPerformed
        elegirCliente();
    }//GEN-LAST:event_btnClienteActionPerformed

    private void btnSinClienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSinClienteActionPerformed
        quitarCliente();
    }//GEN-LAST:event_btnSinClienteActionPerformed

    private void btnVaciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVaciarActionPerformed
        modeloCarrito.vaciar();
        actualizarTotal();
    }//GEN-LAST:event_btnVaciarActionPerformed

    private void btnConfirmarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmarActionPerformed
        confirmar();
    }//GEN-LAST:event_btnConfirmarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAgregar;
    private javax.swing.JButton btnAnular;
    private javax.swing.JButton btnCliente;
    private javax.swing.JButton btnConfirmar;
    private javax.swing.JButton btnQuitar;
    private javax.swing.JButton btnSinCliente;
    private javax.swing.JButton btnVaciar;
    private javax.swing.JButton btnVerVenta;
    private javax.swing.JComboBox<canchapro.pagos.MetodoPago> cboMetodo;
    private javax.swing.JLabel lblBuscar;
    private javax.swing.JLabel lblCantidad;
    private javax.swing.JLabel lblCliente;
    private javax.swing.JLabel lblClienteValor;
    private javax.swing.JLabel lblMetodo;
    private javax.swing.JLabel lblTotal;
    private javax.swing.JLabel lblTotalHoy;
    private javax.swing.JPanel pnlAccionesHistorial;
    private javax.swing.JPanel pnlAccionesVenta;
    private javax.swing.JPanel pnlBotonesHistorial;
    private javax.swing.JPanel pnlBuscar;
    private javax.swing.JPanel pnlCarrito;
    private javax.swing.JPanel pnlCatalogo;
    private javax.swing.JPanel pnlClienteBotones;
    private javax.swing.JPanel pnlHistorial;
    private javax.swing.JPanel pnlIzquierda;
    private javax.swing.JScrollPane scrCarrito;
    private javax.swing.JScrollPane scrCatalogo;
    private javax.swing.JScrollPane scrVentas;
    private javax.swing.JSpinner spnCantidad;
    private javax.swing.JTable tblCarrito;
    private javax.swing.JTable tblCatalogo;
    private javax.swing.JTable tblVentas;
    private javax.swing.JTextField txtBuscar;
    // End of variables declaration//GEN-END:variables
}
