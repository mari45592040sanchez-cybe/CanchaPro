package canchapro.disponibilidad;

import canchapro.canchas.Cancha;
import canchapro.canchas.OpcionTipoCancha;
import canchapro.canchas.TipoCancha;
import canchapro.disponibilidad.GrillaOcupacion.TipoCelda;
import canchapro.principal.Refrescable;
import canchapro.util.BloquesHorario;
import canchapro.util.BloquesHorario.OpcionDuracion;
import canchapro.util.FormatoFecha;
import canchapro.util.SelectorFecha;
import canchapro.util.excepciones.CanchaProException;
import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

/**
 * Consulta de disponibilidad: el usuario indica fecha, hora, duración y
 * tipo de cancha, y el sistema determina qué canchas se pueden reservar.
 * Abajo se muestra la ocupación del día por cancha y por bloque de 30 minutos.
 */
// POO - Herencia: extiende JPanel. POO - Polimorfismo: implementa Refrescable (la ventana principal la refresca).
public class PnlDisponibilidad extends javax.swing.JPanel implements Refrescable {

    private static final LocalTime PRIMERA_HORA = LocalTime.of(6, 0);
    private static final LocalTime ULTIMA_HORA = LocalTime.of(23, 0);

    private final DisponibilidadServicio servicio;
    private final ResultadoTableModel modeloResultados = new ResultadoTableModel();
    private final GrillaTableModel modeloGrilla = new GrillaTableModel();
    /** Última consulta realizada, para resaltarla en la grilla. */
    private SolicitudDisponibilidad ultimaSolicitud;
    /** Evita redibujar la grilla mientras se configuran los controles. */
    private boolean inicializado;
    /** Acción del botón Reservar; null si el usuario no tiene acceso a Reservas. */
    private BiConsumer<SolicitudDisponibilidad, Cancha> alReservar;

    public PnlDisponibilidad(DisponibilidadServicio servicio) {
        this.servicio = servicio;
        initComponents();

        SelectorFecha.configurar(spnFecha, LocalDate.now());
        spnFecha.addChangeListener(e -> {
            if (inicializado) {
                actualizarGrilla();
            }
        });
        cboHora.setModel(new DefaultComboBoxModel<>(BloquesHorario.horasEntre(PRIMERA_HORA, ULTIMA_HORA)));
        cboHora.setSelectedItem(siguienteHoraSugerida());
        cboDuracion.setModel(new DefaultComboBoxModel<>(BloquesHorario.duracionesPermitidas()));
        cboTipo.setModel(new DefaultComboBoxModel<>(OpcionTipoCancha.opciones()));

        tblResultados.setModel(modeloResultados);
        tblResultados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblResultados.setDefaultRenderer(Boolean.class, new DisponibleRenderer());
        tblResultados.getSelectionModel().addListSelectionListener(e -> actualizarBotonReservar());
        pnlAcciones.setVisible(false);

        tblGrilla.setModel(modeloGrilla);
        tblGrilla.setDefaultRenderer(GrillaOcupacion.Celda.class, new CeldaOcupacionRenderer());
        tblGrilla.setRowSelectionAllowed(false);
        tblGrilla.setRowHeight(22);
        tblGrilla.getTableHeader().setReorderingAllowed(false);

        crearLeyenda();
        inicializado = true;
        actualizarGrilla();
    }

    /**
     * Habilita el botón "Reservar", que abre Nueva Reserva con el horario y
     * la cancha seleccionados.
     */
    public void setAlReservar(BiConsumer<SolicitudDisponibilidad, Cancha> accion) {
        this.alReservar = accion;
        pnlAcciones.setVisible(accion != null);
        actualizarBotonReservar();
    }

    @Override
    public void refrescar() {
        if (ultimaSolicitud != null) {
            try {
                modeloResultados.setResultados(servicio.consultar(ultimaSolicitud));
            } catch (CanchaProException e) {
                // La consulta anterior ya no es válida (por ejemplo, la hora pasó).
                modeloResultados.setResultados(new ResultadoDisponibilidad[0]);
                ultimaSolicitud = null;
            }
        }
        actualizarGrilla();
    }

    private void actualizarBotonReservar() {
        int fila = tblResultados.getSelectedRow();
        btnReservar.setEnabled(alReservar != null && ultimaSolicitud != null && fila >= 0
                && modeloResultados.getResultado(fila).disponible());
    }

    private void reservar() {
        int fila = tblResultados.getSelectedRow();
        if (alReservar != null && ultimaSolicitud != null && fila >= 0) {
            ResultadoDisponibilidad r = modeloResultados.getResultado(fila);
            if (r.disponible()) {
                alReservar.accept(ultimaSolicitud, r.cancha());
            }
        }
    }

    /** Próxima hora en punto o y media a partir de ahora (o la primera del día). */
    private LocalTime siguienteHoraSugerida() {
        LocalTime ahora = servicio.horaActual();
        LocalTime sugerida = ahora.withMinute(ahora.getMinute() < 30 ? 30 : 0);
        if (ahora.getMinute() >= 30) {
            sugerida = sugerida.plusHours(1);
        }
        if (sugerida.isBefore(PRIMERA_HORA) || sugerida.isAfter(ULTIMA_HORA) || sugerida.isBefore(ahora)) {
            return LocalTime.of(8, 0);
        }
        return sugerida;
    }

    private void crearLeyenda() {
        for (TipoCelda tipo : TipoCelda.values()) {
            JLabel muestra = new JLabel("  " + tipo + "  ");
            muestra.setOpaque(true);
            muestra.setBackground(CeldaOcupacionRenderer.colorDe(tipo));
            muestra.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            pnlLeyenda.add(muestra);
        }
        JLabel solicitado = new JLabel("  Horario consultado  ");
        solicitado.setBorder(BorderFactory.createLineBorder(new Color(37, 99, 235), 2));
        pnlLeyenda.add(solicitado);
    }

    private SolicitudDisponibilidad leerSolicitud() {
        return new SolicitudDisponibilidad(SelectorFecha.obtener(spnFecha),
                (LocalTime) cboHora.getSelectedItem(),
                ((OpcionDuracion) cboDuracion.getSelectedItem()).duracion(),
                tipoSeleccionado());
    }

    private TipoCancha tipoSeleccionado() {
        return ((OpcionTipoCancha) cboTipo.getSelectedItem()).tipo();
    }

    private void consultar() {
        try {
            SolicitudDisponibilidad solicitud = leerSolicitud();
            ResultadoDisponibilidad[] resultados = servicio.consultar(solicitud);
            modeloResultados.setResultados(resultados);
            ultimaSolicitud = solicitud;

            int disponibles = 0;
            for (ResultadoDisponibilidad r : resultados) {
                if (r.disponible()) {
                    disponibles++;
                }
            }
            lblResumen.setText(disponibles + " de " + resultados.length + " cancha(s) disponibles el "
                    + FormatoFecha.vista(solicitud.fecha()) + " de " + solicitud.horaInicio()
                    + " a " + solicitud.horaFin() + ".");
            actualizarGrilla();
        } catch (CanchaProException e) {
            modeloResultados.setResultados(new ResultadoDisponibilidad[0]);
            ultimaSolicitud = null;
            lblResumen.setText(e.getMessage());
            actualizarGrilla();
            JOptionPane.showMessageDialog(this, e.getMessage(), "Disponibilidad", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Redibuja la grilla del día seleccionado, resaltando la última consulta si corresponde. */
    private void actualizarGrilla() {
        LocalDate fecha = SelectorFecha.obtener(spnFecha);
        SolicitudDisponibilidad resaltar = ultimaSolicitud != null && ultimaSolicitud.fecha().equals(fecha)
                ? ultimaSolicitud : null;
        modeloGrilla.setGrilla(servicio.ocupacionDelDia(fecha, tipoSeleccionado(), resaltar));
        tblGrilla.getColumnModel().getColumn(0).setMaxWidth(70);
        ((TitledBorder) pnlOcupacion.getBorder())
                .setTitle("Ocupación del " + FormatoFecha.vista(fecha));
        pnlOcupacion.repaint();
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
        pnlConsulta = new javax.swing.JPanel();
        lblFecha = new javax.swing.JLabel();
        spnFecha = new javax.swing.JSpinner();
        lblHora = new javax.swing.JLabel();
        cboHora = new javax.swing.JComboBox<>();
        lblDuracion = new javax.swing.JLabel();
        cboDuracion = new javax.swing.JComboBox<>();
        lblTipo = new javax.swing.JLabel();
        cboTipo = new javax.swing.JComboBox<>();
        btnConsultar = new javax.swing.JButton();
        lblResumen = new javax.swing.JLabel();
        pnlCentro = new javax.swing.JPanel();
        pnlResultados = new javax.swing.JPanel();
        scrResultados = new javax.swing.JScrollPane();
        tblResultados = new javax.swing.JTable();
        pnlOcupacion = new javax.swing.JPanel();
        pnlLeyenda = new javax.swing.JPanel();
        scrGrilla = new javax.swing.JScrollPane();
        tblGrilla = new javax.swing.JTable();
        pnlAcciones = new javax.swing.JPanel();
        lblAyudaReservar = new javax.swing.JLabel();
        btnReservar = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setLayout(new java.awt.BorderLayout(12, 12));

        pnlNorte.setLayout(new java.awt.BorderLayout(0, 8));
        lblTitulo.setText("Disponibilidad");
        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 22)); // NOI18N
        pnlNorte.add(lblTitulo, java.awt.BorderLayout.NORTH);

        pnlConsulta.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
        lblFecha.setText("Fecha:");
        pnlConsulta.add(lblFecha);

        pnlConsulta.add(spnFecha);

        lblHora.setText("Hora de inicio:");
        pnlConsulta.add(lblHora);

        pnlConsulta.add(cboHora);

        lblDuracion.setText("Duración:");
        pnlConsulta.add(lblDuracion);

        pnlConsulta.add(cboDuracion);

        lblTipo.setText("Tipo:");
        pnlConsulta.add(lblTipo);

        cboTipo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboTipoActionPerformed(evt);
            }
        });
        pnlConsulta.add(cboTipo);

        btnConsultar.setText("Consultar disponibilidad");
        btnConsultar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConsultarActionPerformed(evt);
            }
        });
        pnlConsulta.add(btnConsultar);

        pnlNorte.add(pnlConsulta, java.awt.BorderLayout.CENTER);

        lblResumen.setText("Indique fecha, hora, duración y tipo de cancha, y presione Consultar.");
        lblResumen.setForeground(new java.awt.Color(128, 128, 128));
        pnlNorte.add(lblResumen, java.awt.BorderLayout.SOUTH);

        add(pnlNorte, java.awt.BorderLayout.NORTH);

        pnlCentro.setLayout(new java.awt.GridBagLayout());
        pnlResultados.setBorder(javax.swing.BorderFactory.createTitledBorder("Canchas para el horario consultado"));
        pnlResultados.setLayout(new java.awt.BorderLayout());
        tblResultados.setRowHeight(24);
        scrResultados.setViewportView(tblResultados);

        pnlResultados.add(scrResultados, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.35;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 8, 0);
        pnlCentro.add(pnlResultados, gridBagConstraints);

        pnlOcupacion.setBorder(javax.swing.BorderFactory.createTitledBorder("Ocupación del día"));
        pnlOcupacion.setLayout(new java.awt.BorderLayout(0, 6));
        pnlLeyenda.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
        pnlOcupacion.add(pnlLeyenda, java.awt.BorderLayout.NORTH);

        tblGrilla.setRowHeight(24);
        scrGrilla.setViewportView(tblGrilla);

        pnlOcupacion.add(scrGrilla, java.awt.BorderLayout.CENTER);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.65;
        pnlCentro.add(pnlOcupacion, gridBagConstraints);

        add(pnlCentro, java.awt.BorderLayout.CENTER);

        pnlAcciones.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        lblAyudaReservar.setText("Seleccione una cancha disponible del resultado para reservarla.");
        lblAyudaReservar.setForeground(new java.awt.Color(128, 128, 128));
        pnlAcciones.add(lblAyudaReservar);

        btnReservar.setText("Reservar cancha seleccionada");
        btnReservar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReservarActionPerformed(evt);
            }
        });
        pnlAcciones.add(btnReservar);

        add(pnlAcciones, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents

    private void btnConsultarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConsultarActionPerformed
        consultar();
    }//GEN-LAST:event_btnConsultarActionPerformed

    private void btnReservarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReservarActionPerformed
        reservar();
    }//GEN-LAST:event_btnReservarActionPerformed

    private void cboTipoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cboTipoActionPerformed
        if (inicializado) {
            actualizarGrilla();
        }
    }//GEN-LAST:event_cboTipoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConsultar;
    private javax.swing.JButton btnReservar;
    private javax.swing.JComboBox<canchapro.util.BloquesHorario.OpcionDuracion> cboDuracion;
    private javax.swing.JComboBox<java.time.LocalTime> cboHora;
    private javax.swing.JComboBox<canchapro.canchas.OpcionTipoCancha> cboTipo;
    private javax.swing.JLabel lblAyudaReservar;
    private javax.swing.JLabel lblDuracion;
    private javax.swing.JLabel lblFecha;
    private javax.swing.JLabel lblHora;
    private javax.swing.JLabel lblResumen;
    private javax.swing.JLabel lblTipo;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JPanel pnlAcciones;
    private javax.swing.JPanel pnlCentro;
    private javax.swing.JPanel pnlConsulta;
    private javax.swing.JPanel pnlLeyenda;
    private javax.swing.JPanel pnlNorte;
    private javax.swing.JPanel pnlOcupacion;
    private javax.swing.JPanel pnlResultados;
    private javax.swing.JScrollPane scrGrilla;
    private javax.swing.JScrollPane scrResultados;
    private javax.swing.JSpinner spnFecha;
    private javax.swing.JTable tblGrilla;
    private javax.swing.JTable tblResultados;
    // End of variables declaration//GEN-END:variables
}
