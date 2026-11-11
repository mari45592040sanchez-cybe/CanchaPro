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
import canchapro.tienda.DetalleVentaRepositorio;
import canchapro.tienda.ProductoRepositorio;
import canchapro.tienda.ReporteVentasTienda;
import canchapro.tienda.TiendaServicio;
import canchapro.tienda.VentaRepositorio;
import canchapro.usuarios.UsuarioRepositorio;
import canchapro.usuarios.UsuarioServicio;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Clock;

// POO - Relaciones (composición): crea y conecta todos los repositorios y servicios del sistema.
// Cada servicio recibe por constructor los objetos que necesita (no se usan variables globales).
public class ContextoAplicacion {

    private final ConfiguracionRepositorio configuracionRepositorio;
    private final Configuracion configuracion;
    private final Path carpetaDatos;
    private final RegistroErroresCarga registroErrores = new RegistroErroresCarga();
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
    private final TiendaServicio tiendaServicio;
    private final ReporteServicio reporteServicio;
    private final ConfiguracionServicio configuracionServicio;
    private final boolean administradorInicialCreado;

    public ContextoAplicacion(Path archivoConfiguracion) {
        this(archivoConfiguracion, Clock.systemDefaultZone());
    }

    public ContextoAplicacion(Path archivoConfiguracion, Clock reloj) {
        configuracionRepositorio = new ConfiguracionRepositorio(archivoConfiguracion);
        configuracion = configuracionRepositorio.cargar(registroErrores);
        carpetaDatos = resolverCarpetaDatos();

        UsuarioRepositorio usuarioRepositorio = new UsuarioRepositorio(carpetaDatos);
        ClienteRepositorio clienteRepositorio = new ClienteRepositorio(carpetaDatos);
        CanchaRepositorio canchaRepositorio = new CanchaRepositorio(carpetaDatos);
        TarifaRepositorio tarifaRepositorio = new TarifaRepositorio(carpetaDatos);
        ServicioAdicionalRepositorio servicioAdicionalRepositorio = new ServicioAdicionalRepositorio(carpetaDatos);
        ReservaRepositorio reservaRepositorio = new ReservaRepositorio(carpetaDatos, clienteRepositorio,
                canchaRepositorio, servicioAdicionalRepositorio, usuarioRepositorio);
        PagoRepositorio pagoRepositorio = new PagoRepositorio(carpetaDatos, reservaRepositorio::existe,
                usuarioRepositorio);
        reservaRepositorio.setFuentePagos(pagoRepositorio::listar);
        pagoRepositorio.setAlCargar(reservaRepositorio::vincularPagos);

        ProductoRepositorio productoRepositorio = new ProductoRepositorio(carpetaDatos);
        VentaRepositorio ventaRepositorio = new VentaRepositorio(carpetaDatos, clienteRepositorio, usuarioRepositorio,
                reservaRepositorio::existe);
        DetalleVentaRepositorio detalleVentaRepositorio = new DetalleVentaRepositorio(carpetaDatos,
                ventaRepositorio::existe, productoRepositorio);
        ventaRepositorio.setFuenteDetalles(detalleVentaRepositorio::listar);
        detalleVentaRepositorio.setAlCargar(ventaRepositorio::vincularDetalles);
        // POO - Relaciones: las ventas con factura unificada se asocian a su reserva al leer los archivos.
        reservaRepositorio.setFuenteVentas(ventaRepositorio::listar);
        ventaRepositorio.setAlCargar(reservaRepositorio::vincularVentas);

        cargador = new CargadorDatos(carpetaDatos);
        cargador.registrar(usuarioRepositorio);
        cargador.registrar(clienteRepositorio);
        cargador.registrar(canchaRepositorio);
        cargador.registrar(tarifaRepositorio);
        cargador.registrar(servicioAdicionalRepositorio);
        cargador.registrar(reservaRepositorio);
        cargador.registrar(pagoRepositorio);
        cargador.registrar(productoRepositorio);
        cargador.registrar(ventaRepositorio);
        cargador.registrar(detalleVentaRepositorio);
        cargador.cargarTodo(registroErrores);
        registroErrores.escribirLog(carpetaDatos);

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
        tiendaServicio = new TiendaServicio(productoRepositorio, ventaRepositorio, detalleVentaRepositorio,
                reservaServicio, reloj);
        reporteServicio = crearReportes(reloj);
        configuracionServicio = new ConfiguracionServicio(configuracion, configuracionRepositorio, carpetaDatos,
                this::recargarDatos);

        administradorInicialCreado = usuarioServicio.asegurarAdministrador();
        canchaServicio.asegurarDatosIniciales();
        tarifaServicio.asegurarDatosIniciales();
        servicioAdicionalServicio.asegurarDatosIniciales();
        tiendaServicio.asegurarDatosIniciales();

        reservaServicio.actualizarEstadosAutomaticos();
    }

    private RegistroErroresCarga recargarDatos() {
        RegistroErroresCarga registro = new RegistroErroresCarga();
        cargador.cargarTodo(registro);
        registro.escribirLog(carpetaDatos);
        reservaServicio.actualizarEstadosAutomaticos();
        return registro;
    }

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
        servicio.registrar(new ReporteVentasTienda(tiendaServicio, configuracion));
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

    public TiendaServicio getTiendaServicio() {
        return tiendaServicio;
    }

    public RegistroErroresCarga getRegistroErrores() {
        return registroErrores;
    }

    public boolean isAdministradorInicialCreado() {
        return administradorInicialCreado;
    }
}
