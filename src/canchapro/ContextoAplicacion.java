package canchapro;

// [CANCHAS] import canchapro.canchas.CanchaRepositorio;
// [CANCHAS] import canchapro.canchas.CanchaServicio;
// [CLIENTES] import canchapro.clientes.ClienteRepositorio;
// [CLIENTES] import canchapro.clientes.ClienteServicio;
import canchapro.configuracion.Configuracion;
import canchapro.configuracion.ConfiguracionRepositorio;
import canchapro.configuracion.ConfiguracionServicio;
// [DASHBOARD] import canchapro.dashboard.DashboardServicio;
// [DISPONIBILIDAD] import canchapro.disponibilidad.DisponibilidadServicio;
import canchapro.login.LoginServicio;
// [RESERVAS_PAGOS] import canchapro.pagos.PagoRepositorio;
// [RESERVAS_PAGOS] import canchapro.pagos.PagoServicio;
import canchapro.persistencia.CargadorDatos;
import canchapro.persistencia.ErrorCarga;
import canchapro.persistencia.RegistroErroresCarga;
// [REPORTES] import canchapro.reportes.ReporteCanchaMasUtilizada;
// [REPORTES] import canchapro.reportes.ReporteClientesFrecuentes;
// [REPORTES] import canchapro.reportes.ReporteHorariosMayorDemanda;
// [REPORTES] import canchapro.reportes.ReporteIngresosPorPeriodo;
// [REPORTES] import canchapro.reportes.ReportePagosPendientes;
// [REPORTES] import canchapro.reportes.ReporteReservasCanceladas;
// [REPORTES] import canchapro.reportes.ReporteReservasPorCancha;
// [REPORTES] import canchapro.reportes.ReporteReservasPorFecha;
// [REPORTES] import canchapro.reportes.ReporteServicio;
// [RESERVAS_PAGOS] import canchapro.reservas.OcupacionReservas;
// [RESERVAS_PAGOS] import canchapro.reservas.ReservaRepositorio;
// [RESERVAS_PAGOS] import canchapro.reservas.ReservaServicio;
// [SERVICIOS] import canchapro.servicios.ServicioAdicionalRepositorio;
// [SERVICIOS] import canchapro.servicios.ServicioAdicionalServicio;
// [TARIFAS] import canchapro.tarifas.TarifaRepositorio;
// [TARIFAS] import canchapro.tarifas.TarifaServicio;
// [TIENDA] import canchapro.tienda.DetalleVentaRepositorio;
// [TIENDA] import canchapro.tienda.ProductoRepositorio;
// [TIENDA] import canchapro.tienda.ReporteVentasTienda;
// [TIENDA] import canchapro.tienda.TiendaServicio;
// [TIENDA] import canchapro.tienda.VentaRepositorio;
import canchapro.usuarios.UsuarioRepositorio;
import canchapro.usuarios.UsuarioServicio;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Construye y conecta los repositorios y servicios del sistema.
 * Se crea una sola vez en {@code main} y se pasa por constructor a las
 * pantallas, en lugar de usar variables globales.
 *
 * Flujo de arranque:
 * configuración → carpeta de datos → carga de archivos (en orden) →
 * bitácora de errores → servicios → datos iniciales → estados de reservas.
 */
public class ContextoAplicacion {

    private final ConfiguracionRepositorio configuracionRepositorio;
    private final Configuracion configuracion;
    private final Path carpetaDatos;
    private final RegistroErroresCarga registroErrores = new RegistroErroresCarga();
    /** Lee todos los archivos en orden; se conserva para poder recargarlos desde Configuración. */
    private final CargadorDatos cargador;

    private final UsuarioServicio usuarioServicio;
    private final LoginServicio loginServicio;
    // [CLIENTES] private final ClienteServicio clienteServicio;
    // [CANCHAS] private final CanchaServicio canchaServicio;
    // [TARIFAS] private final TarifaServicio tarifaServicio;
    // [SERVICIOS] private final ServicioAdicionalServicio servicioAdicionalServicio;
    // [DISPONIBILIDAD] private final DisponibilidadServicio disponibilidadServicio;
    // [RESERVAS_PAGOS] private final ReservaServicio reservaServicio;
    // [RESERVAS_PAGOS] private final PagoServicio pagoServicio;
    // [DASHBOARD] private final DashboardServicio dashboardServicio;
    // [TIENDA] private final TiendaServicio tiendaServicio;
    // [REPORTES] private final ReporteServicio reporteServicio;
    private final ConfiguracionServicio configuracionServicio;
    private final boolean administradorInicialCreado;

    public ContextoAplicacion(Path archivoConfiguracion) {
        this(archivoConfiguracion, Clock.systemDefaultZone());
    }

    /**
     * @param reloj hora del sistema (las pruebas pueden fijarla)
     */
    public ContextoAplicacion(Path archivoConfiguracion, Clock reloj) {
        // 1. Configuración
        configuracionRepositorio = new ConfiguracionRepositorio(archivoConfiguracion);
        configuracion = configuracionRepositorio.cargar(registroErrores);
        carpetaDatos = resolverCarpetaDatos();

        // 2. Repositorios. Cada uno recibe los repositorios de los que depende
        //    y se cargan en ese mismo orden.
        UsuarioRepositorio usuarioRepositorio = new UsuarioRepositorio(carpetaDatos);
        // [CLIENTES] ClienteRepositorio clienteRepositorio = new ClienteRepositorio(carpetaDatos);
        // [CANCHAS] CanchaRepositorio canchaRepositorio = new CanchaRepositorio(carpetaDatos);
        // [TARIFAS] TarifaRepositorio tarifaRepositorio = new TarifaRepositorio(carpetaDatos);
        // [SERVICIOS] ServicioAdicionalRepositorio servicioAdicionalRepositorio = new ServicioAdicionalRepositorio(carpetaDatos);
        // [RESERVAS_PAGOS] ReservaRepositorio reservaRepositorio = new ReservaRepositorio(carpetaDatos, clienteRepositorio,
                // [RESERVAS_PAGOS] canchaRepositorio, servicioAdicionalRepositorio, usuarioRepositorio);
        // [RESERVAS_PAGOS] PagoRepositorio pagoRepositorio = new PagoRepositorio(carpetaDatos, reservaRepositorio::existe,
                // [RESERVAS_PAGOS] usuarioRepositorio);
        // [RESERVAS_PAGOS] // Los pagos se asocian a sus reservas cada vez que se lee cualquiera de los dos archivos.
        // [RESERVAS_PAGOS] reservaRepositorio.setFuentePagos(pagoRepositorio::listar);
        // [RESERVAS_PAGOS] pagoRepositorio.setAlCargar(reservaRepositorio::vincularPagos);

        // [TIENDA] // Módulo Tienda: sus archivos se cargan siempre (aunque el módulo esté
        // [TIENDA] // desactivado) para que los datos estén intactos al reactivarlo.
        // [TIENDA] ProductoRepositorio productoRepositorio = new ProductoRepositorio(carpetaDatos);
        // [TIENDA] VentaRepositorio ventaRepositorio = new VentaRepositorio(carpetaDatos, clienteRepositorio, usuarioRepositorio);
        // [TIENDA] DetalleVentaRepositorio detalleVentaRepositorio = new DetalleVentaRepositorio(carpetaDatos,
                // [TIENDA] ventaRepositorio::existe, productoRepositorio);
        // [TIENDA] ventaRepositorio.setFuenteDetalles(detalleVentaRepositorio::listar);
        // [TIENDA] detalleVentaRepositorio.setAlCargar(ventaRepositorio::vincularDetalles);

        cargador = new CargadorDatos(carpetaDatos);
        cargador.registrar(usuarioRepositorio);
        // [CLIENTES] cargador.registrar(clienteRepositorio);
        // [CANCHAS] cargador.registrar(canchaRepositorio);
        // [TARIFAS] cargador.registrar(tarifaRepositorio);
        // [SERVICIOS] cargador.registrar(servicioAdicionalRepositorio);
        // [RESERVAS_PAGOS] cargador.registrar(reservaRepositorio);
        // [RESERVAS_PAGOS] cargador.registrar(pagoRepositorio);
        // [TIENDA] cargador.registrar(productoRepositorio);
        // [TIENDA] cargador.registrar(ventaRepositorio);
        // [TIENDA] cargador.registrar(detalleVentaRepositorio);
        cargador.cargarTodo(registroErrores);
        registroErrores.escribirLog(carpetaDatos);

        // 3. Servicios
        usuarioServicio = new UsuarioServicio(usuarioRepositorio);
        loginServicio = new LoginServicio(usuarioRepositorio);
        // [CLIENTES] clienteServicio = new ClienteServicio(clienteRepositorio);
        // [CANCHAS] canchaServicio = new CanchaServicio(canchaRepositorio);
        // [TARIFAS] tarifaServicio = new TarifaServicio(tarifaRepositorio);
        // [SERVICIOS] servicioAdicionalServicio = new ServicioAdicionalServicio(servicioAdicionalRepositorio);
        // [DISPONIBILIDAD] disponibilidadServicio = new DisponibilidadServicio(canchaServicio, tarifaServicio,
                // [DISPONIBILIDAD] new OcupacionReservas(reservaRepositorio), reloj);
        // [RESERVAS_PAGOS] reservaServicio = new ReservaServicio(reservaRepositorio, pagoRepositorio, disponibilidadServicio,
                // [RESERVAS_PAGOS] configuracion, reloj);
        // [RESERVAS_PAGOS] pagoServicio = new PagoServicio(pagoRepositorio, reservaServicio, configuracion, reloj);
        // [DASHBOARD] dashboardServicio = new DashboardServicio(reservaServicio, pagoServicio, canchaServicio, reloj);
        // [TIENDA] tiendaServicio = new TiendaServicio(productoRepositorio, ventaRepositorio, detalleVentaRepositorio, reloj);
        // [REPORTES] reporteServicio = crearReportes(reloj);
        configuracionServicio = new ConfiguracionServicio(configuracion, configuracionRepositorio, carpetaDatos,
                this::recargarDatos);

        // 4. Datos iniciales del primer arranque
        administradorInicialCreado = usuarioServicio.asegurarAdministrador();
        // [CANCHAS] canchaServicio.asegurarDatosIniciales();
        // [TARIFAS] tarifaServicio.asegurarDatosIniciales();
        // [SERVICIOS] servicioAdicionalServicio.asegurarDatosIniciales();
        // [TIENDA] tiendaServicio.asegurarDatosIniciales();

        // [RESERVAS_PAGOS] // 5. Reservas que empezaron o terminaron mientras el programa estaba cerrado
        // [RESERVAS_PAGOS] reservaServicio.actualizarEstadosAutomaticos();
    }

    /**
     * Vuelve a leer todos los archivos de datos (desde la pantalla de
     * Configuración). Los servicios siguen siendo los mismos; solo se
     * reemplazan los objetos en memoria.
     *
     * @return errores encontrados durante la lectura
     */
    private RegistroErroresCarga recargarDatos() {
        RegistroErroresCarga registro = new RegistroErroresCarga();
        cargador.cargarTodo(registro);
        registro.escribirLog(carpetaDatos);
        // [RESERVAS_PAGOS] reservaServicio.actualizarEstadosAutomaticos();
        return registro;
    }

    // [REPORTES] /**
     // [REPORTES] * Registra los 8 reportes obligatorios. Los reportes se exportan a la
     // [REPORTES] * carpeta "reportes", junto a la carpeta de datos.
     // [REPORTES] */
    // [REPORTES] private ReporteServicio crearReportes(Clock reloj) {
        // [REPORTES] Path carpetaReportes = carpetaDatos.toAbsolutePath().getParent().resolve("reportes");
        // [REPORTES] ReporteServicio servicio = new ReporteServicio(carpetaReportes, reloj);
        // [REPORTES] servicio.registrar(new ReporteReservasPorFecha(reservaServicio));
        // [REPORTES] servicio.registrar(new ReporteReservasPorCancha(reservaServicio, canchaServicio));
        // [REPORTES] servicio.registrar(new ReporteIngresosPorPeriodo(reservaServicio, pagoServicio));
        // [REPORTES] servicio.registrar(new ReportePagosPendientes(reservaServicio));
        // [REPORTES] servicio.registrar(new ReporteReservasCanceladas(reservaServicio));
        // [REPORTES] servicio.registrar(new ReporteCanchaMasUtilizada(reservaServicio, canchaServicio));
        // [REPORTES] servicio.registrar(new ReporteHorariosMayorDemanda(reservaServicio));
        // [REPORTES] servicio.registrar(new ReporteClientesFrecuentes(reservaServicio));
        // [REPORTES] // Solo aparece si el módulo Tienda está activo.
        // [TIENDA] servicio.registrar(new ReporteVentasTienda(tiendaServicio, configuracion));
        // [REPORTES] return servicio;
    // [REPORTES] }

    private Path resolverCarpetaDatos() {
        try {
            return Path.of(configuracion.getRutaDatos());
        } catch (InvalidPathException e) {
            registroErrores.agregar(new ErrorCarga(configuracionRepositorio.getArchivo().getFileName().toString(),
                    0, ConfiguracionRepositorio.CLAVE_RUTA_DATOS + "=" + configuracion.getRutaDatos(),
                    "ruta inválida (se usará '" + Configuracion.RUTA_DATOS_DEFECTO + "')"));
            return Path.of(Configuracion.RUTA_DATOS_DEFECTO);
        }
    }

    public Configuracion getConfiguracion() {
        return configuracion;
    }

    public Path getCarpetaDatos() {
        return carpetaDatos;
    }

    public UsuarioServicio getUsuarioServicio() {
        return usuarioServicio;
    }

    public LoginServicio getLoginServicio() {
        return loginServicio;
    }

    // [CLIENTES] public ClienteServicio getClienteServicio() {
        // [CLIENTES] return clienteServicio;
    // [CLIENTES] }

    // [CANCHAS] public CanchaServicio getCanchaServicio() {
        // [CANCHAS] return canchaServicio;
    // [CANCHAS] }

    // [TARIFAS] public TarifaServicio getTarifaServicio() {
        // [TARIFAS] return tarifaServicio;
    // [TARIFAS] }

    // [SERVICIOS] public ServicioAdicionalServicio getServicioAdicionalServicio() {
        // [SERVICIOS] return servicioAdicionalServicio;
    // [SERVICIOS] }

    // [DISPONIBILIDAD] public DisponibilidadServicio getDisponibilidadServicio() {
        // [DISPONIBILIDAD] return disponibilidadServicio;
    // [DISPONIBILIDAD] }

    // [RESERVAS_PAGOS] public ReservaServicio getReservaServicio() {
        // [RESERVAS_PAGOS] return reservaServicio;
    // [RESERVAS_PAGOS] }

    // [RESERVAS_PAGOS] public PagoServicio getPagoServicio() {
        // [RESERVAS_PAGOS] return pagoServicio;
    // [RESERVAS_PAGOS] }

    // [DASHBOARD] public DashboardServicio getDashboardServicio() {
        // [DASHBOARD] return dashboardServicio;
    // [DASHBOARD] }

    // [REPORTES] public ReporteServicio getReporteServicio() {
        // [REPORTES] return reporteServicio;
    // [REPORTES] }

    public ConfiguracionServicio getConfiguracionServicio() {
        return configuracionServicio;
    }

    // [TIENDA] public TiendaServicio getTiendaServicio() {
        // [TIENDA] return tiendaServicio;
    // [TIENDA] }

    /** Errores encontrados al leer configuración y archivos de datos. */
    public RegistroErroresCarga getRegistroErrores() {
        return registroErrores;
    }

    public boolean isAdministradorInicialCreado() {
        return administradorInicialCreado;
    }
}
