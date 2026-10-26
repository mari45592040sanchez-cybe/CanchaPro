package canchapro.reservas;

import canchapro.canchas.Cancha;
import canchapro.canchas.OpcionTipoCancha;
import canchapro.canchas.TipoCancha;
import canchapro.clientes.Cliente;
import canchapro.clientes.ClienteServicio;
import canchapro.clientes.DlgBuscarCliente;
import canchapro.configuracion.Configuracion;
import canchapro.disponibilidad.DisponibilidadServicio;
import canchapro.disponibilidad.DisponibleRenderer;
import canchapro.disponibilidad.ResultadoDisponibilidad;
import canchapro.disponibilidad.ResultadoTableModel;
import canchapro.disponibilidad.SolicitudDisponibilidad;
import canchapro.login.Sesion;
import canchapro.pagos.MetodoPago;
import canchapro.principal.Refrescable;
import canchapro.servicios.ServicioAdicionalServicio;
import canchapro.util.BloquesHorario;
import canchapro.util.BloquesHorario.OpcionDuracion;
import canchapro.util.Dialogos;
import canchapro.util.FormatoMoneda;
import canchapro.util.SelectorFecha;
import canchapro.util.Validador;
import canchapro.util.excepciones.CanchaProException;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Consumer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

/**
 * Asistente de nueva reserva, siguiendo el flujo del enunciado:
 * 1 cliente → 2-4 fecha, horario y consulta → 5 cancha disponible →
 * 6 servicios → 7 resumen con total calculado → 8 pago inicial → 9 confirmar.
 *
 * La pantalla no calcula precios ni decide disponibilidad: todo lo hace
 * {@link ReservaServicio}.
 */
// POO - Herencia: extiende JPanel. POO - Polimorfismo: implementa Refrescable.
// POO - Relaciones: usa los servicios de reservas, disponibilidad, clientes y servicios adicionales.
public class PnlNuevaReserva extends javax.swing.JPanel implements Refrescable {

    private final ReservaServicio reservaServicio;
    private final DisponibilidadServicio disponibilidadServicio;
    private final ClienteServicio clienteServicio;
    private final ServicioAdicionalServicio servicioAdicionalServicio;
    private final Configuracion configuracion;
    private final Sesion sesion;
    private final ResultadoTableModel modeloCanchas = new ResultadoTableModel();
    private final ServicioSeleccionTableModel modeloServicios = new ServicioSeleccionTableModel();
    private Consumer<Reserva> alCrearReserva = r -> { };

    private Cliente cliente;
    /** Solicitud de la última consulta; null si hay que volver a consultar. */
    private SolicitudDisponibilidad solicitudConsultada;
    private ReservaBorrador borrador;
    private boolean cargando;

    public PnlNuevaReserva(ReservaServicio reservaServicio, DisponibilidadServicio disponibilidadServicio,
            ClienteServicio clienteServicio, ServicioAdicionalServicio servicioAdicionalServicio,
            Configuracion configuracion, Sesion sesion) {
        this.reservaServicio = reservaServicio;
        this.disponibilidadServicio = disponibilidadServicio;
        this.clienteServicio = clienteServicio;
        this.servicioAdicionalServicio = servicioAdicionalServicio;
        this.configuracion = configuracion;
        this.sesion = sesion;
        cargando = true;
        initComponents();

        SelectorFecha.configurar(spnFecha, LocalDate.now());
        cboHora.setModel(new DefaultComboBoxModel<>(BloquesHorario.horasEntre(LocalTime.of(6, 0), LocalTime.of(23, 0))));
        cboHora.setSelectedItem(LocalTime.of(18, 0));
        cboDuracion.setModel(new DefaultComboBoxModel<>(BloquesHorario.duracionesPermitidas()));
        cboTipo.setModel(new DefaultComboBoxModel<>(OpcionTipoCancha.opciones()));
        cboMetodo.setModel(new DefaultComboBoxModel<>(MetodoPago.values()));

        tblCanchas.setModel(modeloCanchas);
        tblCanchas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCanchas.setDefaultRenderer(Boolean.class, new DisponibleRenderer());
        tblCanchas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarResumen();
            }
        });

        tblServicios.setModel(modeloServicios);
        tblServicios.getColumnModel().getColumn(0).setMaxWidth(70);
        modeloServicios.addTableModelListener(e -> actualizarResumen());
        modeloServicios.setServicios(servicioAdicionalServicio.listarActivos());

        // Cambiar fecha, hora, duración o tipo invalida la consulta anterior.
        spnFecha.addChangeListener(e -> invalidarConsulta());
        ActionListener invalidar = e -> invalidarConsulta();
        cboHora.addActionListener(invalidar);
        cboDuracion.addActionListener(invalidar);
        cboTipo.addActionListener(invalidar);
        cargando = false;
        actualizarResumen();
    }

    /** Acción a ejecutar después de crear una reserva (por ejemplo, refrescar el listado). */
    public void setAlCrearReserva(Consumer<Reserva> accion) {
        this.alCrearReserva = accion;
    }

    /**
     * Llena el formulario con un horario y una cancha elegidos desde la
     * pantalla de Disponibilidad.
     */
    public void precargar(SolicitudDisponibilidad solicitud, Cancha cancha) {
        cargando = true;
        SelectorFecha.establecer(spnFecha, solicitud.fecha());
        cboHora.setSelectedItem(solicitud.horaInicio());
        cboDuracion.setSelectedItem(new OpcionDuracion(solicitud.duracion()));
        cboTipo.setSelectedItem(new OpcionTipoCancha(solicitud.tipoCancha()));
        cargando = false;
        consultar();
        for (int i = 0; i < modeloCanchas.getRowCount(); i++) {
            if (modeloCanchas.getResultado(i).cancha().getCodigo().equals(cancha.getCodigo())) {
                tblCanchas.setRowSelectionInterval(i, i);
            }
        }
    }

    @Override
    public void refrescar() {
        // Solo se recarga el catálogo si el usuario no ha elegido servicios.
        if (!modeloServicios.haySeleccionados()) {
            modeloServicios.setServicios(servicioAdicionalServicio.listarActivos());
        }
    }

    // --- Pasos del asistente ---

    private void seleccionarCliente() {
        DlgBuscarCliente.seleccionar(SwingUtilities.getWindowAncestor(this), clienteServicio).ifPresent(c -> {
            cliente = c;
            lblClienteValor.setText(c.getDescripcion());
            actualizarResumen();
        });
    }

    private SolicitudDisponibilidad leerSolicitud() {
        TipoCancha tipo = ((OpcionTipoCancha) cboTipo.getSelectedItem()).tipo();
        return new SolicitudDisponibilidad(SelectorFecha.obtener(spnFecha), (LocalTime) cboHora.getSelectedItem(),
                ((OpcionDuracion) cboDuracion.getSelectedItem()).duracion(), tipo);
    }

    private void consultar() {
        try {
            SolicitudDisponibilidad solicitud = leerSolicitud();
            ResultadoDisponibilidad[] disponibles = disponibilidadServicio.listarDisponibles(solicitud);
            modeloCanchas.setResultados(disponibles);
            solicitudConsultada = solicitud;
            lblCanchasInfo.setText(disponibles.length == 0
                    ? "No hay canchas disponibles en ese horario. Pruebe otra hora, duración o tipo."
                    : disponibles.length + " cancha(s) disponible(s) de " + solicitud.horaInicio() + " a "
                    + solicitud.horaFin() + ". Seleccione una.");
            if (disponibles.length == 1) {
                tblCanchas.setRowSelectionInterval(0, 0);
            }
        } catch (CanchaProException e) {
            invalidarConsulta();
            mostrarAdvertencia(e.getMessage());
        }
        actualizarResumen();
    }

    private void invalidarConsulta() {
        if (cargando) {
            return;
        }
        solicitudConsultada = null;
        modeloCanchas.setResultados(new ResultadoDisponibilidad[0]);
        lblCanchasInfo.setText("Consulte la disponibilidad para ver las canchas libres.");
        actualizarResumen();
    }

    private Cancha canchaSeleccionada() {
        int fila = tblCanchas.getSelectedRow();
        return fila < 0 ? null : modeloCanchas.getResultado(fila).cancha();
    }

    /** Recalcula el resumen y el total cada vez que cambia algún paso. */
    private void actualizarResumen() {
        if (cargando) {
            return;
        }
        borrador = null;
        Cancha cancha = canchaSeleccionada();
        String pendiente = cliente == null ? "Paso 1: seleccione o registre el cliente."
                : solicitudConsultada == null ? "Pasos 2-4: indique fecha y horario y presione \"Consultar disponibilidad\"."
                : cancha == null ? "Paso 5: seleccione una cancha disponible."
                : null;
        if (pendiente != null) {
            txtResumen.setText(pendiente);
        } else {
            try {
                borrador = reservaServicio.prepararReserva(cliente, solicitudConsultada, cancha,
                        modeloServicios.getSeleccionados());
                txtResumen.setText(FormatoReserva.resumen(borrador, configuracion.getAnticipoMinimoPorcentaje()));
            } catch (CanchaProException e) {
                txtResumen.setText("No se puede preparar la reserva:\n" + e.getMessage());
            }
        }
        txtResumen.setCaretPosition(0);
        btnConfirmar.setEnabled(borrador != null);
        lblAnticipo.setText(borrador == null
                ? "Sin pago inicial la reserva queda PENDIENTE."
                : "Para quedar CONFIRMADA pague al menos " + FormatoMoneda.formatear(borrador.anticipoMinimo()) + ".");
    }

    private void confirmar() {
        if (borrador == null) {
            return;
        }
        try {
            String textoMonto = txtMonto.getText().trim();
            BigDecimal monto = textoMonto.isEmpty() ? null : Validador.monto(textoMonto, "El monto del pago inicial");
            Reserva reserva = reservaServicio.confirmarReserva(borrador, monto, (MetodoPago) cboMetodo.getSelectedItem(),
                    txtObservacion.getText(), sesion.getUsuario());
            Dialogos.mostrarTexto(this, "Reserva confirmada", "Reserva " + reserva.getCodigo() + " creada.",
                    FormatoReserva.detalle(reserva));
            limpiar();
            alCrearReserva.accept(reserva);
        } catch (CanchaProException e) {
            mostrarAdvertencia(e.getMessage());
            // La disponibilidad pudo cambiar: se vuelve a consultar.
            if (solicitudConsultada != null) {
                consultar();
            }
        }
    }

    private void limpiar() {
        cliente = null;
        lblClienteValor.setText("Ningún cliente seleccionado");
        modeloServicios.setServicios(servicioAdicionalServicio.listarActivos());
        txtMonto.setText("");
        txtObservacion.setText("");
        cboMetodo.setSelectedIndex(0);
        invalidarConsulta();
    }

    private void mostrarAdvertencia(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Nueva reserva", JOptionPane.WARNING_MESSAGE);
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
        pnlPasos = new javax.swing.JPanel();
        pnlCliente = new javax.swing.JPanel();
        lblClienteValor = new javax.swing.JLabel();
        btnSeleccionarCliente = new javax.swing.JButton();
        pnlHorario = new javax.swing.JPanel();
        lblFecha = new javax.swing.JLabel();
        spnFecha = new javax.swing.JSpinner();
        lblHora = new javax.swing.JLabel();
        cboHora = new javax.swing.JComboBox<>();
        lblDuracion = new javax.swing.JLabel();
        cboDuracion = new javax.swing.JComboBox<>();
        lblTipo = new javax.swing.JLabel();
        cboTipo = new javax.swing.JComboBox<>();
        btnConsultar = new javax.swing.JButton();
        pnlCanchas = new javax.swing.JPanel();
        scrCanchas = new javax.swing.JScrollPane();
        tblCanchas = new javax.swing.JTable();
        lblCanchasInfo = new javax.swing.JLabel();
        pnlServicios = new javax.swing.JPanel();
        scrServicios = new javax.swing.JScrollPane();
        tblServicios = new javax.swing.JTable();
        pnlLateral = new javax.swing.JPanel();
        pnlResumen = new javax.swing.JPanel();
        scrResumen = new javax.swing.JScrollPane();
        txtResumen = new javax.swing.JTextArea();
        pnlPago = new javax.swing.JPanel();
        lblMonto = new javax.swing.JLabel();
        txtMonto = new javax.swing.JTextField();
        lblMetodo = new javax.swing.JLabel();
        cboMetodo = new javax.swing.JComboBox<>();
        lblObservacion = new javax.swing.JLabel();
        txtObservacion = new javax.swing.JTextField();
        lblAnticipo = new javax.swing.JLabel();
        pnlAcciones = new javax.swing.JPanel();
        btnLimpiar = new javax.swing.JButton();
        btnConfirmar = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        lblTitulo.setText("Nueva reserva");
        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        add(lblTitulo, java.awt.BorderLayout.NORTH);

        pnlPasos.setLayout(new java.awt.GridBagLayout());
        pnlCliente.setBorder(javax.swing.BorderFactory.createTitledBorder("1. Cliente"));
        pnlCliente.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 2));
        lblClienteValor.setText("Ningún cliente seleccionado");
        lblClienteValor.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        pnlCliente.add(lblClienteValor);

        btnSeleccionarCliente.setText("Buscar / registrar cliente…");
        btnSeleccionarCliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSeleccionarClienteActionPerformed(evt);
            }
        });
        pnlCliente.add(btnSeleccionarCliente);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        pnlPasos.add(pnlCliente, gridBagConstraints);

        pnlHorario.setBorder(javax.swing.BorderFactory.createTitledBorder("2-4. Fecha, horario y tipo de cancha"));
        pnlHorario.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 2));
        lblFecha.setText("Fecha:");
        pnlHorario.add(lblFecha);

        pnlHorario.add(spnFecha);

        lblHora.setText("Hora:");
        pnlHorario.add(lblHora);

        pnlHorario.add(cboHora);

        lblDuracion.setText("Duración:");
        pnlHorario.add(lblDuracion);

        pnlHorario.add(cboDuracion);

        lblTipo.setText("Tipo:");
        pnlHorario.add(lblTipo);

        pnlHorario.add(cboTipo);

        btnConsultar.setText("Consultar disponibilidad");
        btnConsultar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConsultarActionPerformed(evt);
            }
        });
        pnlHorario.add(btnConsultar);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        pnlPasos.add(pnlHorario, gridBagConstraints);

        pnlCanchas.setBorder(javax.swing.BorderFactory.createTitledBorder("5. Cancha disponible"));
        pnlCanchas.setLayout(new java.awt.BorderLayout(0, 4));
        tblCanchas.setRowHeight(24);
        scrCanchas.setViewportView(tblCanchas);

        pnlCanchas.add(scrCanchas, java.awt.BorderLayout.CENTER);

        lblCanchasInfo.setText("Consulte la disponibilidad para ver las canchas libres.");
        lblCanchasInfo.setForeground(new java.awt.Color(128, 128, 128));
        pnlCanchas.add(lblCanchasInfo, java.awt.BorderLayout.SOUTH);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        pnlPasos.add(pnlCanchas, gridBagConstraints);

        pnlServicios.setBorder(javax.swing.BorderFactory.createTitledBorder("6. Servicios adicionales (opcional)"));
        pnlServicios.setLayout(new java.awt.BorderLayout());
        tblServicios.setRowHeight(24);
        scrServicios.setViewportView(tblServicios);

        pnlServicios.add(scrServicios, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        pnlPasos.add(pnlServicios, gridBagConstraints);

        add(pnlPasos, java.awt.BorderLayout.CENTER);

        pnlLateral.setLayout(new java.awt.GridBagLayout());
        pnlResumen.setBorder(javax.swing.BorderFactory.createTitledBorder("7. Resumen"));
        pnlResumen.setLayout(new java.awt.BorderLayout());
        txtResumen.setEditable(false);
        txtResumen.setColumns(52);
        txtResumen.setRows(18);
        txtResumen.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        scrResumen.setViewportView(txtResumen);

        pnlResumen.add(scrResumen, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        pnlLateral.add(pnlResumen, gridBagConstraints);

        pnlPago.setBorder(javax.swing.BorderFactory.createTitledBorder("8. Pago inicial (opcional)"));
        pnlPago.setLayout(new java.awt.GridBagLayout());
        lblMonto.setText("Monto (Q):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlPago.add(lblMonto, gridBagConstraints);

        txtMonto.setColumns(10);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlPago.add(txtMonto, gridBagConstraints);

        lblMetodo.setText("Método:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlPago.add(lblMetodo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlPago.add(cboMetodo, gridBagConstraints);

        lblObservacion.setText("Observación:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 4, 8);
        pnlPago.add(lblObservacion, gridBagConstraints);

        txtObservacion.setColumns(18);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 0, 4, 8);
        pnlPago.add(txtObservacion, gridBagConstraints);

        lblAnticipo.setText("Sin pago inicial la reserva queda PENDIENTE.");
        lblAnticipo.setForeground(new java.awt.Color(128, 128, 128));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(4, 8, 6, 8);
        pnlPago.add(lblAnticipo, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        pnlLateral.add(pnlPago, gridBagConstraints);

        pnlAcciones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        btnLimpiar.setText("Limpiar");
        btnLimpiar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLimpiarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnLimpiar);

        btnConfirmar.setText("9. Confirmar reserva");
        btnConfirmar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConfirmarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnConfirmar);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        pnlLateral.add(pnlAcciones, gridBagConstraints);

        add(pnlLateral, java.awt.BorderLayout.EAST);
    }// </editor-fold>//GEN-END:initComponents

    private void btnSeleccionarClienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSeleccionarClienteActionPerformed
        seleccionarCliente();
    }//GEN-LAST:event_btnSeleccionarClienteActionPerformed

    private void btnConsultarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConsultarActionPerformed
        consultar();
    }//GEN-LAST:event_btnConsultarActionPerformed

    private void btnLimpiarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLimpiarActionPerformed
        limpiar();
    }//GEN-LAST:event_btnLimpiarActionPerformed

    private void btnConfirmarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConfirmarActionPerformed
        confirmar();
    }//GEN-LAST:event_btnConfirmarActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConfirmar;
    private javax.swing.JButton btnConsultar;
    private javax.swing.JButton btnLimpiar;
    private javax.swing.JButton btnSeleccionarCliente;
    private javax.swing.JComboBox<canchapro.util.BloquesHorario.OpcionDuracion> cboDuracion;
    private javax.swing.JComboBox<java.time.LocalTime> cboHora;
    private javax.swing.JComboBox<canchapro.pagos.MetodoPago> cboMetodo;
    private javax.swing.JComboBox<canchapro.canchas.OpcionTipoCancha> cboTipo;
    private javax.swing.JLabel lblAnticipo;
    private javax.swing.JLabel lblCanchasInfo;
    private javax.swing.JLabel lblClienteValor;
    private javax.swing.JLabel lblDuracion;
    private javax.swing.JLabel lblFecha;
    private javax.swing.JLabel lblHora;
    private javax.swing.JLabel lblMetodo;
    private javax.swing.JLabel lblMonto;
    private javax.swing.JLabel lblObservacion;
    private javax.swing.JLabel lblTipo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel pnlAcciones;
    private javax.swing.JPanel pnlCanchas;
    private javax.swing.JPanel pnlCliente;
    private javax.swing.JPanel pnlHorario;
    private javax.swing.JPanel pnlLateral;
    private javax.swing.JPanel pnlPago;
    private javax.swing.JPanel pnlPasos;
    private javax.swing.JPanel pnlResumen;
    private javax.swing.JPanel pnlServicios;
    private javax.swing.JScrollPane scrCanchas;
    private javax.swing.JScrollPane scrResumen;
    private javax.swing.JScrollPane scrServicios;
    private javax.swing.JSpinner spnFecha;
    private javax.swing.JTable tblCanchas;
    private javax.swing.JTable tblServicios;
    private javax.swing.JTextField txtMonto;
    private javax.swing.JTextField txtObservacion;
    private javax.swing.JTextArea txtResumen;
    // End of variables declaration//GEN-END:variables
}
