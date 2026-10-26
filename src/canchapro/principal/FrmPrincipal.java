package canchapro.principal;

import canchapro.ContextoAplicacion;
import canchapro.canchas.PnlCanchas;
import canchapro.clientes.PnlClientes;
import canchapro.configuracion.PnlConfiguracion;
// [DASHBOARD] import canchapro.dashboard.Indicador;
// [DASHBOARD] import canchapro.dashboard.PnlDashboard;
import canchapro.disponibilidad.PnlDisponibilidad;
import canchapro.login.DlgCambiarPassword;
import canchapro.login.FrmLogin;
import canchapro.login.Sesion;
import canchapro.pagos.PnlPagos;
// [REPORTES] import canchapro.reportes.PnlReportes;
import canchapro.reservas.PnlModuloReservas;
import canchapro.reservas.PnlNuevaReserva;
import canchapro.reservas.PnlReservas;
import canchapro.servicios.PnlServicios;
import canchapro.tarifas.PnlTarifas;
// [TIENDA] import canchapro.tienda.PnlModuloTienda;
// [TIENDA] import canchapro.tienda.PnlProductos;
// [TIENDA] import canchapro.tienda.PnlVentas;
// [TIENDA] import canchapro.tienda.TiendaServicio;
// [TIENDA] import canchapro.util.FormatoMoneda;
import canchapro.usuarios.Modulo;
import canchapro.usuarios.PnlUsuarios;
import canchapro.usuarios.Usuario;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.EnumMap;
// [TIENDA] import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/**
 * Ventana principal: menú lateral con los módulos permitidos para el
 * usuario y un panel central donde se muestra el módulo seleccionado.
 */
// POO - Herencia: la ventana principal extiende JFrame de Swing.
public class FrmPrincipal extends javax.swing.JFrame {

    private static final Color COLOR_MENU = new Color(31, 41, 55);
    private static final Color COLOR_SELECCIONADO = new Color(22, 101, 52);
    private static final Color COLOR_TEXTO_SECCION = new Color(156, 163, 175);
    private static final String TARJETA_SIN_MODULOS = "SIN_MODULOS";
    /** Cada minuto se actualizan los estados de las reservas según la hora. */
    private static final int INTERVALO_ESTADOS_MS = 60_000;

    // POO - Relaciones: la ventana usa el contexto (servicios) y la sesión del usuario conectado.
    private final ContextoAplicacion contexto;
    private final Sesion sesion;
    // POO - Colecciones: mapa módulo -> pantalla ya creada (cada pantalla se crea una sola vez).
    private final Map<Modulo, JComponent> paneles = new EnumMap<>(Modulo.class);
    private final Map<Modulo, JButton> botones = new EnumMap<>(Modulo.class);
    private final Timer temporizadorEstados;
    private Modulo moduloActual;
    /** Oyentes registrados en Configuración (patrón Observador). */
    private final Runnable oyenteCambios = this::configuracionCambiada;
    private final Runnable oyenteRecarga = this::datosRecargados;

    public FrmPrincipal(ContextoAplicacion contexto, Sesion sesion) {
        this.contexto = contexto;
        this.sesion = sesion;
        initComponents();

        actualizarTitulo();
        Usuario usuario = sesion.getUsuario();
        lblUsuario.setText(usuario.getNombre() + "  (" + usuario.getRol() + ")");
        construirMenu(null);
        setExtendedState(MAXIMIZED_BOTH);

        contexto.getConfiguracionServicio().agregarOyenteCambios(oyenteCambios);
        contexto.getConfiguracionServicio().agregarOyenteRecarga(oyenteRecarga);

        temporizadorEstados = new Timer(INTERVALO_ESTADOS_MS, e -> actualizarEstadosReservas());
        temporizadorEstados.start();
    }

    /**
     * Pasa a EN_CURSO / FINALIZADA las reservas según la hora y, si hubo
     * cambios, refresca la pantalla visible.
     */
    private void actualizarEstadosReservas() {
        try {
            boolean cambiaron = contexto.getReservaServicio().actualizarEstadosAutomaticos() > 0;
            // El inicio se refresca cada minuto (ingresos, reservas en curso);
            // las demás pantallas solo si cambió el estado de alguna reserva.
            if (cambiaron || moduloActual == Modulo.DASHBOARD) {
                refrescarModuloActual();
            }
        } catch (RuntimeException e) {
            // Por ejemplo, el archivo está bloqueado: se reintentará en el próximo minuto.
            System.err.println("No se pudieron actualizar los estados de las reservas: " + e.getMessage());
        }
    }

    private void refrescarModuloActual() {
        if (moduloActual != null && paneles.get(moduloActual) instanceof Refrescable refrescable) {
            refrescable.refrescar();
        }
    }

    private void actualizarTitulo() {
        String negocio = contexto.getConfiguracion().getNombreNegocio();
        setTitle("CanchaPro - " + negocio);
        lblTitulo.setText("CanchaPro  |  " + negocio);
    }

    /**
     * La configuración cambió: puede aparecer o desaparecer la Tienda, o
     * cambiar el nombre. El inicio y los reportes se vuelven a crear porque
     * muestran (o no) información de la Tienda.
     */
    private void configuracionCambiada() {
        actualizarTitulo();
        descartarPanel(Modulo.DASHBOARD);
        descartarPanel(Modulo.REPORTES);
        Modulo actual = moduloActual;
        moduloActual = null;
        construirMenu(actual);
    }

    private void descartarPanel(Modulo modulo) {
        JComponent panel = paneles.remove(modulo);
        if (panel != null) {
            pnlContenido.remove(panel);
        }
    }

    /**
     * Se volvieron a leer los archivos: las pantallas abiertas tienen objetos
     * viejos, así que se descartan y se crean de nuevo al mostrarlas.
     */
    private void datosRecargados() {
        Modulo actual = moduloActual;
        paneles.clear();
        pnlContenido.removeAll();
        moduloActual = null;
        construirMenu(actual);
    }

    @Override
    public void dispose() {
        temporizadorEstados.stop();
        contexto.getConfiguracionServicio().quitarOyente(oyenteCambios);
        contexto.getConfiguracionServicio().quitarOyente(oyenteRecarga);
        super.dispose();
    }

    /**
     * Un módulo se muestra si el usuario tiene permiso y además el módulo
     * está habilitado en la configuración (por ejemplo, la Tienda).
     */
    private boolean esVisible(Modulo modulo) {
        // POO - Polimorfismo: puedeAcceder() responde distinto si el usuario es Administrador o Empleado.
        return sesion.puedeAcceder(modulo) && contexto.getConfiguracion().estaHabilitado(modulo);
    }

    /**
     * Crea un botón por cada módulo visible para el usuario y muestra el
     * módulo preferido (si sigue visible) o el primero de la lista.
     */
    private void construirMenu(Modulo preferido) {
        pnlMenu.removeAll();
        botones.clear();
        Modulo primero = null;
        boolean seccionAdminAgregada = false;

        for (Modulo modulo : Modulo.values()) {
            if (!esVisible(modulo)) {
                continue;
            }
            if (modulo.isSoloAdministrador() && !seccionAdminAgregada) {
                pnlMenu.add(Box.createVerticalStrut(16));
                pnlMenu.add(crearTituloSeccion("ADMINISTRACIÓN"));
                pnlMenu.add(Box.createVerticalStrut(4));
                seccionAdminAgregada = true;
            }
            JButton boton = crearBotonMenu(modulo);
            botones.put(modulo, boton);
            pnlMenu.add(boton);
            pnlMenu.add(Box.createVerticalStrut(4));
            if (primero == null) {
                primero = modulo;
            }
        }
        pnlMenu.revalidate();
        pnlMenu.repaint();

        if (primero == null) {
            pnlContenido.add(new PnlEnConstruccion("Sin módulos asignados",
                    "No tiene módulos asignados. Contacte al administrador."), TARJETA_SIN_MODULOS);
            ((CardLayout) pnlContenido.getLayout()).show(pnlContenido, TARJETA_SIN_MODULOS);
        } else {
            mostrarModulo(preferido != null && esVisible(preferido) ? preferido : primero);
        }
    }

    /**
     * Muestra el panel del módulo. Los paneles se crean la primera vez que
     * se abren y luego se reutilizan.
     */
    public void mostrarModulo(Modulo modulo) {
        if (!esVisible(modulo)) {
            return;
        }
        if (!paneles.containsKey(modulo)) {
            JComponent panel = crearPanel(modulo);
            paneles.put(modulo, panel);
            pnlContenido.add(panel, modulo.name());
        // POO - Polimorfismo (interfaz): cualquier pantalla que implemente Refrescable se actualiza igual.
        } else if (modulo != moduloActual && paneles.get(modulo) instanceof Refrescable refrescable) {
            // Los datos pudieron cambiar en otro módulo.
            refrescable.refrescar();
        }
        moduloActual = modulo;
        ((CardLayout) pnlContenido.getLayout()).show(pnlContenido, modulo.name());
        botones.forEach((m, boton) -> boton.setBackground(m == modulo ? COLOR_SELECCIONADO : COLOR_MENU));
    }

    // POO - Polimorfismo: todas las pantallas se tratan como JComponent, sin importar su clase concreta.
    private JComponent crearPanel(Modulo modulo) {
        return switch (modulo) {
            // [DASHBOARD] case DASHBOARD -> new PnlDashboard(contexto.getDashboardServicio(), this::irA, this::esVisible,
                    // [DASHBOARD] indicadoresTienda());
            // [TIENDA] case TIENDA -> new PnlModuloTienda(
                    // [TIENDA] new PnlVentas(contexto.getTiendaServicio(), contexto.getClienteServicio(), sesion),
                    // [TIENDA] new PnlProductos(contexto.getTiendaServicio()));
            case CLIENTES -> new PnlClientes(contexto.getClienteServicio());
            
            case DISPONIBILIDAD -> crearPanelDisponibilidad();
            case RESERVAS -> new PnlModuloReservas(
                    new PnlNuevaReserva(contexto.getReservaServicio(), contexto.getDisponibilidadServicio(),
                            contexto.getClienteServicio(), contexto.getServicioAdicionalServicio(),
                            contexto.getConfiguracion(), sesion),
                    new PnlReservas(contexto.getReservaServicio(), contexto.getCanchaServicio(),
                            contexto.getConfiguracion(), sesion));
            // [REPORTES] case REPORTES -> new PnlReportes(contexto.getReporteServicio(), contexto.getCanchaServicio());
            case PAGOS -> new PnlPagos(contexto.getPagoServicio(), contexto.getReservaServicio(),
                    contexto.getConfiguracion(), sesion);
            case CANCHAS -> crearPanelCanchas();
            case TARIFAS -> new PnlTarifas(contexto.getTarifaServicio());
            case SERVICIOS -> new PnlServicios(contexto.getServicioAdicionalServicio());
            case USUARIOS -> new PnlUsuarios(contexto.getUsuarioServicio(), sesion);
            case CONFIGURACION -> new PnlConfiguracion(contexto.getConfiguracionServicio());            
            default -> new PnlEnConstruccion(modulo.getNombreVisible(),
                    "Este módulo se implementará en una próxima tarea.");
        };
    }

    // [TIENDA] /** Tarjetas de la Tienda para el inicio (solo si el módulo está activo y permitido). */
    // [TIENDA] private List<Indicador> indicadoresTienda() {
        // [TIENDA] if (!esVisible(Modulo.TIENDA)) {
            // [TIENDA] return List.of();
        // [TIENDA] }
        // [TIENDA] TiendaServicio tienda = contexto.getTiendaServicio();
        // [TIENDA] return List.of(
                // [TIENDA] new Indicador("Tienda: vendido hoy", () -> FormatoMoneda.formatear(tienda.totalVendidoHoy())),
                // [TIENDA] new Indicador("Tienda: productos con stock bajo",
                        // [TIENDA] () -> String.valueOf(tienda.productosConStockBajo().size())));
    // [TIENDA] }

    /**
     * Navega a un módulo desde un acceso rápido. Para Reservas abre
     * directamente la pestaña "Nueva reserva".
     */
    private void irA(Modulo modulo) {
        mostrarModulo(modulo);
        if (modulo == Modulo.RESERVAS && paneles.get(modulo) instanceof PnlModuloReservas reservas) {
            reservas.mostrarNuevaReserva();
        }
    }

    /** Disponibilidad con el botón "Reservar" si el usuario puede usar Reservas. */
    private PnlDisponibilidad crearPanelDisponibilidad() {
        PnlDisponibilidad panel = new PnlDisponibilidad(contexto.getDisponibilidadServicio());
        if (esVisible(Modulo.RESERVAS)) {
            panel.setAlReservar((solicitud, cancha) -> {
                mostrarModulo(Modulo.RESERVAS);
                ((PnlModuloReservas) paneles.get(Modulo.RESERVAS)).nuevaReserva(solicitud, cancha);
            });
        }
        return panel;
    }

    /** Canchas, avisando las reservas futuras afectadas al ponerla en mantenimiento. */
    private PnlCanchas crearPanelCanchas() {
        PnlCanchas panel = new PnlCanchas(contexto.getCanchaServicio());
        panel.setReservasAfectadas(codigoCancha -> contexto.getReservaServicio()
                .describirReservasFuturas(codigoCancha));
        return panel;
    }

    private JButton crearBotonMenu(Modulo modulo) {
        JButton boton = new JButton(modulo.getNombreVisible());
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setAlignmentX(LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        boton.setFont(boton.getFont().deriveFont(Font.PLAIN, 14f));
        boton.setForeground(Color.WHITE);
        boton.setBackground(COLOR_MENU);
        boton.setOpaque(true);
        boton.setContentAreaFilled(false);
        boton.setBorderPainted(false);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        boton.addActionListener(e -> mostrarModulo(modulo));
        return boton;
    }

    private JLabel crearTituloSeccion(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(COLOR_TEXTO_SECCION);
        etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD, 11f));
        etiqueta.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        etiqueta.setAlignmentX(LEFT_ALIGNMENT);
        return etiqueta;
    }

    private void cerrarSesion() {
        int opcion = JOptionPane.showConfirmDialog(this, "¿Desea cerrar la sesión?",
                "Cerrar sesión", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            dispose();
            new FrmLogin(contexto).setVisible(true);
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

        pnlEncabezado = new javax.swing.JPanel();
        lblTitulo = new javax.swing.JLabel();
        pnlSesion = new javax.swing.JPanel();
        lblUsuario = new javax.swing.JLabel();
        btnCambiarPassword = new javax.swing.JButton();
        btnCerrarSesion = new javax.swing.JButton();
        pnlMenu = new javax.swing.JPanel();
        pnlContenido = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("CanchaPro - Complejo Deportivo La 10");
        setMinimumSize(new java.awt.Dimension(1000, 650));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        pnlEncabezado.setBackground(new java.awt.Color(22, 101, 52));
        pnlEncabezado.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 10));
        pnlEncabezado.setLayout(new java.awt.BorderLayout());

        lblTitulo.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        lblTitulo.setForeground(new java.awt.Color(255, 255, 255));
        lblTitulo.setText("CanchaPro  |  Complejo Deportivo \"La 10\"");
        pnlEncabezado.add(lblTitulo, java.awt.BorderLayout.WEST);

        pnlSesion.setOpaque(false);
        pnlSesion.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        lblUsuario.setForeground(new java.awt.Color(255, 255, 255));
        lblUsuario.setText("Usuario");
        pnlSesion.add(lblUsuario);

        btnCambiarPassword.setText("Cambiar contraseña");
        btnCambiarPassword.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCambiarPasswordActionPerformed(evt);
            }
        });
        pnlSesion.add(btnCambiarPassword);

        btnCerrarSesion.setText("Cerrar sesión");
        btnCerrarSesion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCerrarSesionActionPerformed(evt);
            }
        });
        pnlSesion.add(btnCerrarSesion);

        pnlEncabezado.add(pnlSesion, java.awt.BorderLayout.EAST);

        getContentPane().add(pnlEncabezado, java.awt.BorderLayout.NORTH);

        pnlMenu.setBackground(new java.awt.Color(31, 41, 55));
        pnlMenu.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 8, 10, 8));
        pnlMenu.setPreferredSize(new java.awt.Dimension(220, 400));
        pnlMenu.setLayout(new javax.swing.BoxLayout(pnlMenu, javax.swing.BoxLayout.Y_AXIS));
        getContentPane().add(pnlMenu, java.awt.BorderLayout.WEST);

        pnlContenido.setLayout(new java.awt.CardLayout());
        getContentPane().add(pnlContenido, java.awt.BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnCerrarSesionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCerrarSesionActionPerformed
        cerrarSesion();
    }//GEN-LAST:event_btnCerrarSesionActionPerformed

    private void btnCambiarPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCambiarPasswordActionPerformed
        new DlgCambiarPassword(this, contexto.getLoginServicio(), sesion.getUsuario(), false).setVisible(true);
    }//GEN-LAST:event_btnCambiarPasswordActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        int opcion = JOptionPane.showConfirmDialog(this, "¿Desea salir de CanchaPro?",
                "Salir", JOptionPane.YES_NO_OPTION);
        if (opcion == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }//GEN-LAST:event_formWindowClosing

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCambiarPassword;
    private javax.swing.JButton btnCerrarSesion;
    private javax.swing.JLabel lblTitulo;
    private javax.swing.JLabel lblUsuario;
    private javax.swing.JPanel pnlContenido;
    private javax.swing.JPanel pnlEncabezado;
    private javax.swing.JPanel pnlMenu;
    private javax.swing.JPanel pnlSesion;
    // End of variables declaration//GEN-END:variables
}
