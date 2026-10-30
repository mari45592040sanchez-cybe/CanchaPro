package canchapro;

import canchapro.canchas.CanchaRepositorio;
import canchapro.canchas.CanchaServicio;
import canchapro.clientes.ClienteRepositorio;
import canchapro.clientes.ClienteServicio;
import canchapro.configuracion.Configuracion;
import canchapro.configuracion.ConfiguracionRepositorio;
import canchapro.configuracion.ConfiguracionServicio;
import canchapro.dashboard.DashboardServicio;
import canchapro.disponibilidad.DisponibilidadServicio;
import canchapro.login.LoginServicio;
import canchapro.pagos.PagoRepositorio;
import canchapro.pagos.PagoServicio;
import canchapro.persistencia.CargadorDatos;
import canchapro.persistencia.ErrorCarga;
import canchapro.persistencia.RegistroErroresCarga;
import canchapro.reportes.ReporteCanchaMasUtilizada;
import canchapro.reportes.ReporteClientesFrecuentes;
import canchapro.reportes.ReporteHorariosMayorDemanda;
import canchapro.reportes.ReporteIngresosPorPeriodo;
import canchapro.reportes.ReportePagosPendientes;
import canchapro.reportes.ReporteReservasCanceladas;
import canchapro.reportes.ReporteReservasPorCancha;
import canchapro.reportes.ReporteReservasPorFecha;
import canchapro.reportes.ReporteServicio;
import canchapro.reservas.OcupacionReservas;
import canchapro.reservas.ReservaRepositorio;
import canchapro.reservas.ReservaServicio;
import canchapro.servicios.ServicioAdicionalRepositorio;
import canchapro.servicios.ServicioAdicionalServicio;
import canchapro.tarifas.TarifaRepositorio;
import canchapro.tarifas.TarifaServicio;
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
// POO - Relaciones (composición): crea y conecta todos los repositorios y servicios del sistema.
// Cada servicio recibe por constructor los objetos que necesita (no se usan variables globales).
public class ContextoAplicacion {

    private final ConfiguracionRepositorio configuracionRepositorio;
    private final Configuracion configuracion;
    private final Path carpetaDatos;
    private final RegistroErroresCarga registroErrores = new RegistroErroresCarga();
    /** Lee todos los archivos en orden; se conserva para poder recargarlos desde Configuración. */
    private final CargadorDatos cargador;

    private final UsuarioServicio usuarioServicio;
    private final LoginServicio loginServicio;
    private final ClienteServicio clienteServicio;
    private final CanchaServicio canchaServicio;
    private final TarifaServicio tarifaServicio;
    private final ServicioAdicionalServicio servicioAdicionalServicio;
    private final DisponibilidadServicio disponibilidadServicio;
    private final ReservaServicio reservaServicio;
    private final PagoServicio pagoServicio;
    private final DashboardServicio dashboardServicio;
    // [TIENDA] private final TiendaServicio tiendaServicio;
    private final ReporteServicio reporteServicio;
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
        ClienteRepositorio clienteRepositorio = new ClienteRepositorio(carpetaDatos);
        CanchaRepositorio canchaRepositorio = new CanchaRepositorio(carpetaDatos);
        TarifaRepositorio tarifaRepositorio = new TarifaRepositorio(carpetaDatos);
        ServicioAdicionalRepositorio servicioAdicionalRepositorio = new ServicioAdicionalRepositorio(carpetaDatos);
        ReservaRepositorio reservaRepositorio = new ReservaRepositorio(carpetaDatos, clienteRepositorio,
                canchaRepositorio, servicioAdicionalRepositorio, usuarioRepositorio);
        PagoRepositorio pagoRepositorio = new PagoRepositorio(carpetaDatos, reservaRepositorio::existe,
                usuarioRepositorio);
        // Los pagos se asocian a sus reservas cada vez que se lee cualquiera de los dos archivos.
        reservaRepositorio.setFuentePagos(pagoRepositorio::listar);
        pagoRepositorio.setAlCargar(reservaRepositorio::vincularPagos);

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
        cargador.registrar(clienteRepositorio);
        cargador.registrar(canchaRepositorio);
        cargador.registrar(tarifaRepositorio);
        cargador.registrar(servicioAdicionalRepositorio);
        cargador.registrar(reservaRepositorio);
        cargador.registrar(pagoRepositorio);
        // [TIENDA] cargador.registrar(productoRepositorio);
        // [TIENDA] cargador.registrar(ventaRepositorio);
        // [TIENDA] cargador.registrar(detalleVentaRepositorio);
        cargador.cargarTodo(registroErrores);
        registroErrores.escribirLog(carpetaDatos);

        // 3. Servicios
        usuarioServicio = new UsuarioServicio(usuarioRepositorio);
        loginServicio = new LoginServicio(usuarioRepositorio);
        clienteServicio = new ClienteServicio(clienteRepositorio);
        canchaServicio = new CanchaServicio(canchaRepositorio);
        tarifaServicio = new TarifaServicio(tarifaRepositorio);
        servicioAdicionalServicio = new ServicioAdicionalServicio(servicioAdicionalRepositorio);
        disponibilidadServicio = new DisponibilidadServicio(canchaServicio, tarifaServicio,
                new OcupacionReservas(reservaRepositorio), reloj);
        reservaServicio = new ReservaServicio(reservaRepositorio, pagoRepositorio, disponibilidadServicio,
                configuracion, reloj);
        pagoServicio = new PagoServicio(pagoRepositorio, reservaServicio, configuracion, reloj);
        dashboardServicio = new DashboardServicio(reservaServicio, pagoServicio, canchaServicio, reloj);
        // [TIENDA] tiendaServicio = new TiendaServicio(productoRepositorio, ventaRepositorio, detalleVentaRepositorio, reloj);
        reporteServicio = crearReportes(reloj);
        configuracionServicio = new ConfiguracionServicio(configuracion, configuracionRepositorio, carpetaDatos,
                this::recargarDatos);

        // 4. Datos iniciales del primer arranque
        administradorInicialCreado = usuarioServicio.asegurarAdministrador();
        canchaServicio.asegurarDatosIniciales();
        tarifaServicio.asegurarDatosIniciales();
        servicioAdicionalServicio.asegurarDatosIniciales();
        // [TIENDA] tiendaServicio.asegurarDatosIniciales();

        // 5. Reservas que empezaron o terminaron mientras el programa estaba cerrado
        reservaServicio.actualizarEstadosAutomaticos();
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
        reservaServicio.actualizarEstadosAutomaticos();
        return registro;
    }

    /**
     * Registra los 8 reportes obligatorios. Los reportes se exportan a la
     * carpeta "reportes", junto a la carpeta de datos.
     */
    private ReporteServicio crearReportes(Clock reloj) {
        Path carpetaReportes = carpetaDatos.toAbsolutePath().getParent().resolve("reportes");
        ReporteServicio servicio = new ReporteServicio(carpetaReportes, reloj);
        servicio.registrar(new ReporteReservasPorFecha(reservaServicio));
        servicio.registrar(new ReporteReservasPorCancha(reservaServicio, canchaServicio));
        servicio.registrar(new ReporteIngresosPorPeriodo(reservaServicio, pagoServicio));
        servicio.registrar(new ReportePagosPendientes(reservaServicio));
        servicio.registrar(new ReporteReservasCanceladas(reservaServicio));
        servicio.registrar(new ReporteCanchaMasUtilizada(reservaServicio, canchaServicio));
        servicio.registrar(new ReporteHorariosMayorDemanda(reservaServicio));
        servicio.registrar(new ReporteClientesFrecuentes(reservaServicio));
        // Solo aparece si el módulo Tienda está activo.
        // [TIENDA] servicio.registrar(new ReporteVentasTienda(tiendaServicio, configuracion));
        return servicio;
    }

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

    public ClienteServicio getClienteServicio() {
        return clienteServicio;
    }

    public CanchaServicio getCanchaServicio() {
        return canchaServicio;
    }

    public TarifaServicio getTarifaServicio() {
        return tarifaServicio;
    }

    public ServicioAdicionalServicio getServicioAdicionalServicio() {
        return servicioAdicionalServicio;
    }

    public DisponibilidadServicio getDisponibilidadServicio() {
        return disponibilidadServicio;
    }

    public ReservaServicio getReservaServicio() {
        return reservaServicio;
    }

    public PagoServicio getPagoServicio() {
        return pagoServicio;
    }

    public DashboardServicio getDashboardServicio() {
        return dashboardServicio;
    }

    public ReporteServicio getReporteServicio() {
        return reporteServicio;
    }

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
