package canchapro.reservas;

import canchapro.canchas.Cancha;
import canchapro.canchas.CanchaRepositorio;
import canchapro.clientes.Cliente;
import canchapro.clientes.ClienteRepositorio;
import canchapro.pagos.Pago;
import canchapro.persistencia.CamposCsv;
import canchapro.persistencia.RepositorioCsv;
import canchapro.servicios.ServicioAdicional;
import canchapro.servicios.ServicioAdicionalRepositorio;
import canchapro.usuarios.Usuario;
import canchapro.usuarios.UsuarioRepositorio;
import canchapro.util.FormatoFecha;
import canchapro.util.excepciones.DatosInvalidosException;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Reservas guardadas en reservas.csv:
 * codigo;codigoCliente;codigoCancha;fecha;horaInicio;duracionMin;subtotalCancha;servicios;estado;
 * codigoUsuarioCreador;fechaCreacion;fechaCancelacion;motivoCancelacion;codigoUsuarioCancelacion
 *
 * El campo servicios lista los servicios de la reserva separados por '|',
 * cada uno como codigoServicio:cantidad:precioUnitario:subtotal
 * (ejemplo: SRV-001:2:25.00:50.00|SRV-004:1:50.00:100.00).
 * Los datos de cancelación quedan vacíos si la reserva no está cancelada.
 *
 * Al cargar se reconstruyen las referencias a cliente, cancha, servicios y
 * usuarios; los pagos se leen de pagos.csv y se asocian a cada reserva.
 */
// POO - Herencia / Reutilización: hereda de RepositorioCsv; aquí solo se define el formato de reservas.csv.
// POO - Polimorfismo: los métodos con @Override implementan los métodos abstractos de RepositorioCsv.
public class ReservaRepositorio extends RepositorioCsv<Reserva> {

    public static final String ARCHIVO = "reservas.csv";

    // POO - Relaciones: usa los repositorios de clientes, canchas, servicios y usuarios para
    // reconstruir las referencias de cada reserva al leer el archivo.
    private final ClienteRepositorio clientes;
    private final CanchaRepositorio canchas;
    private final ServicioAdicionalRepositorio servicios;
    private final UsuarioRepositorio usuarios;
    private Supplier<Pago[]> fuentePagos = () -> new Pago[0];

    public ReservaRepositorio(Path carpetaDatos, ClienteRepositorio clientes, CanchaRepositorio canchas,
            ServicioAdicionalRepositorio servicios, UsuarioRepositorio usuarios) {
        super(carpetaDatos.resolve(ARCHIVO));
        this.clientes = clientes;
        this.canchas = canchas;
        this.servicios = servicios;
        this.usuarios = usuarios;
    }

    /** Indica de dónde obtener los pagos para asociarlos a las reservas. */
    public void setFuentePagos(Supplier<Pago[]> fuentePagos) {
        this.fuentePagos = fuentePagos;
    }

    @Override
    protected void despuesDeCargar() {
        vincularPagos();
    }

    /** Asocia a cada reserva sus pagos (se llama tras leer reservas o pagos). */
    public void vincularPagos() {
        Pago[] todos = fuentePagos.get();
        for (Reserva reserva : listar()) {
            // POO - Colecciones: se copian a un arreglo temporal los pagos de esta reserva.
            Pago[] deLaReserva = new Pago[todos.length];
            int cantidad = 0;
            for (Pago pago : todos) {
                if (pago.getCodigoReserva().equals(reserva.getCodigo())) {
                    deLaReserva[cantidad] = pago;
                    cantidad++;
                }
            }
            reserva.reemplazarPagos(Arrays.copyOf(deLaReserva, cantidad));
        }
    }

    // --- Formato del archivo ---

    @Override
    protected String encabezado() {
        return "codigo;codigoCliente;codigoCancha;fecha;horaInicio;duracionMin;subtotalCancha;servicios;estado;"
                + "codigoUsuarioCreador;fechaCreacion;fechaCancelacion;motivoCancelacion;codigoUsuarioCancelacion";
    }

    @Override
    protected int cantidadCampos() {
        return 14;
    }

    @Override
    protected Reserva desdeCsv(String[] c) {
        Cliente cliente = clientes.buscar(c[1].trim())
                .orElseThrow(() -> new EntidadNoEncontradaException("cliente inexistente " + c[1]));
        Cancha cancha = canchas.buscar(c[2].trim())
                .orElseThrow(() -> new EntidadNoEncontradaException("cancha inexistente " + c[2]));
        Duration duracion = Duration.ofMinutes(CamposCsv.entero(c[5], "duracionMin"));
        EstadoReserva estado = CamposCsv.enumeracion(EstadoReserva.class, c[8], "estado");

        Cancelacion cancelacion = null;
        if (estado == EstadoReserva.CANCELADA) {
            cancelacion = new Cancelacion(CamposCsv.fechaHora(c[11], "fechaCancelacion"), c[12], usuario(c[13]));
        }
        return new Reserva(c[0].trim(), cliente, cancha,
                CamposCsv.fecha(c[3], "fecha"),
                CamposCsv.hora(c[4], "horaInicio"),
                duracion,
                CamposCsv.decimal(c[6], "subtotalCancha"),
                leerServicios(c[7]),
                estado,
                usuario(c[9]),
                CamposCsv.fechaHora(c[10], "fechaCreacion"),
                cancelacion);
    }

    @Override
    protected String aCsv(Reserva r) {
        Cancelacion can = r.getCancelacion();
        return String.join(";",
                r.getCodigo(), r.getCliente().getCodigo(), r.getCancha().getCodigo(),
                r.getFecha().format(FormatoFecha.ARCHIVO_FECHA),
                r.getHoraInicio().format(FormatoFecha.ARCHIVO_HORA),
                String.valueOf(r.getDuracion().toMinutes()),
                r.getSubtotalCancha().toPlainString(),
                escribirServicios(r.getServicios()),
                r.getEstado().name(),
                r.getUsuarioCreador().getCodigo(),
                r.getFechaCreacion().format(FormatoFecha.ARCHIVO_FECHA_HORA),
                can == null ? "" : can.fechaHora().format(FormatoFecha.ARCHIVO_FECHA_HORA),
                can == null ? "" : can.motivo(),
                can == null ? "" : can.usuario().getCodigo());
    }

    private Usuario usuario(String codigo) {
        return usuarios.buscar(codigo.trim())
                .orElseThrow(() -> new EntidadNoEncontradaException("usuario inexistente " + codigo));
    }

    private ReservaServicioItem[] leerServicios(String texto) {
        if (texto == null || texto.isBlank()) {
            return new ReservaServicioItem[0];
        }
        String[] partes = texto.split("\\|");
        ReservaServicioItem[] items = new ReservaServicioItem[partes.length];
        for (int i = 0; i < partes.length; i++) {
            String parte = partes[i];
            String[] d = parte.split(":", -1);
            if (d.length != 4) {
                throw new DatosInvalidosException("servicios: formato inválido '" + parte + "'");
            }
            ServicioAdicional servicio = servicios.buscar(d[0].trim())
                    .orElseThrow(() -> new EntidadNoEncontradaException("servicio inexistente " + d[0]));
            items[i] = new ReservaServicioItem(servicio, CamposCsv.entero(d[1], "cantidad"),
                    CamposCsv.decimal(d[2], "precioUnitario"), CamposCsv.decimal(d[3], "subtotal"));
        }
        return items;
    }

    private static String escribirServicios(ReservaServicioItem[] items) {
        String[] textos = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            ReservaServicioItem item = items[i];
            textos[i] = item.servicio().getCodigo() + ":" + item.cantidad() + ":"
                    + item.precioUnitario().toPlainString() + ":" + item.subtotal().toPlainString();
        }
        return String.join("|", textos);
    }

    @Override
    protected Reserva[] crearArreglo(int tamanio) {
        return new Reserva[tamanio];
    }
}
