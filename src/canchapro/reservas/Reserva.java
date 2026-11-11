package canchapro.reservas;

import canchapro.canchas.Cancha;
import canchapro.clientes.Cliente;
import canchapro.disponibilidad.IntervaloHorario;
import canchapro.pagos.Pago;
import canchapro.tienda.Venta;
import canchapro.usuarios.Usuario;
import canchapro.util.FormatoFecha;
import canchapro.util.FormatoMoneda;
import canchapro.util.Identificable;
import canchapro.util.excepciones.PagoInvalidoException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

// POO - Polimorfismo (interfaz): implementa Identificable para poder guardarse en un RepositorioCsv.
// POO - Responsabilidad: la reserva protege sus propias reglas (estados, pagos y cancelación).
public class Reserva implements Identificable {

    private static final int CAPACIDAD_INICIAL_PAGOS = 4;
    private static final int CAPACIDAD_INICIAL_CONSUMOS = 4;

    // POO - Encapsulamiento: todos los atributos son privados; el estado solo cambia con los métodos de negocio.
    private final String codigo;
    // POO - Relaciones (asociación): la reserva conoce a su cliente, su cancha y al usuario que la creó.
    private final Cliente cliente;
    private final Cancha cancha;
    private final LocalDate fecha;
    private final LocalTime horaInicio;
    private final Duration duracion;
    private final BigDecimal subtotalCancha;
    // POO - Colecciones / Relaciones (composición): arreglo de servicios adicionales de la reserva.
    private final ReservaServicioItem[] servicios;
    // POO - Colecciones: "colección de pagos asociados a la reserva" (enunciado, sección 9).
    // Arreglo de objetos Pago + contador; si se llena, se crea uno del doble de tamaño.
    private Pago[] pagos = new Pago[CAPACIDAD_INICIAL_PAGOS];
    private int cantidadPagos;
    // POO - Colecciones / Relaciones (asociación): arreglo de objetos Venta con las compras de la tienda
    // cargadas a esta reserva (factura unificada) + contador; si se llena, se crea uno del doble de tamaño.
    private Venta[] consumos = new Venta[CAPACIDAD_INICIAL_CONSUMOS];
    private int cantidadConsumos;
    private final Usuario usuarioCreador;
    private final LocalDateTime fechaCreacion;
    private EstadoReserva estado;
    private Cancelacion cancelacion;

    public Reserva(String codigo, Cliente cliente, Cancha cancha, LocalDate fecha, LocalTime horaInicio,
            Duration duracion, BigDecimal subtotalCancha, ReservaServicioItem[] servicios,
            EstadoReserva estado, Usuario usuarioCreador, LocalDateTime fechaCreacion, Cancelacion cancelacion) {
        this.codigo = Objects.requireNonNull(codigo);
        this.cliente = Objects.requireNonNull(cliente, "La reserva necesita un cliente");
        this.cancha = Objects.requireNonNull(cancha, "La reserva necesita una cancha");
        this.fecha = Objects.requireNonNull(fecha);
        this.horaInicio = Objects.requireNonNull(horaInicio);
        this.duracion = Objects.requireNonNull(duracion);
        this.subtotalCancha = Objects.requireNonNull(subtotalCancha);
        this.servicios = Arrays.copyOf(servicios, servicios.length);
        this.estado = Objects.requireNonNull(estado);
        this.usuarioCreador = Objects.requireNonNull(usuarioCreador);
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion);
        if ((estado == EstadoReserva.CANCELADA) != (cancelacion != null)) {
            throw new ReglaNegocioException("Una reserva cancelada debe tener los datos de la cancelación (y solo ella).");
        }
        this.cancelacion = cancelacion;
    }

    public LocalTime getHoraFin() {
        return horaInicio.plus(duracion);
    }

    public IntervaloHorario getIntervalo() {
        return IntervaloHorario.de(fecha, horaInicio, duracion);
    }

    public BigDecimal getTotalServicios() {
        BigDecimal total = BigDecimal.ZERO;
        for (ReservaServicioItem item : servicios) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    // POO - Colecciones: recorre el arreglo de consumos y suma solo las ventas que no están anuladas.
    public BigDecimal getTotalTienda() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cantidadConsumos; i++) {
            if (consumos[i].getEstado() == Venta.Estado.REGISTRADA) {
                total = total.add(consumos[i].getTotal());
            }
        }
        return total;
    }

    public BigDecimal getTotal() {
        return subtotalCancha.add(getTotalServicios()).add(getTotalTienda());
    }

    public BigDecimal getTotalPagado() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cantidadPagos; i++) {
            total = total.add(pagos[i].getMonto());
        }
        return total;
    }

    public BigDecimal getSaldo() {
        return getTotal().subtract(getTotalPagado());
    }

    public void cambiarEstado(EstadoReserva destino) {
        if (!estado.puedeTransicionarA(destino)) {
            throw new ReglaNegocioException("La reserva " + codigo + " está " + estado.toString().toLowerCase()
                    + " y no puede pasar a " + destino.toString().toLowerCase() + ".");
        }
        estado = destino;
    }

    public void registrarPago(Pago pago) {
        if (!estado.permitePagos()) {
            throw new PagoInvalidoException("No se pueden registrar pagos en una reserva cancelada.");
        }
        if (!pago.getCodigoReserva().equals(codigo)) {
            throw new PagoInvalidoException("El pago " + pago.getCodigo() + " no corresponde a la reserva " + codigo + ".");
        }
        if (pago.getMonto().compareTo(getSaldo()) > 0) {
            throw new PagoInvalidoException("El pago (" + FormatoMoneda.formatear(pago.getMonto())
                    + ") excede el saldo pendiente (" + FormatoMoneda.formatear(getSaldo()) + ").");
        }
        agregarPago(pago);
    }

    /** Agrega al final del arreglo de pagos; si está lleno, lo agranda al doble. */
    private void agregarPago(Pago pago) {
        if (cantidadPagos == pagos.length) {
            Pago[] mayor = new Pago[pagos.length * 2];
            for (int i = 0; i < cantidadPagos; i++) {
                mayor[i] = pagos[i];
            }
            pagos = mayor;
        }
        pagos[cantidadPagos] = pago;
        cantidadPagos++;
    }

    // POO - Método: indica si la reserva todavía admite compras de la tienda (no cancelada y con saldo).
    public boolean aceptaConsumos() {
        return estado.permitePagos() && getSaldo().signum() > 0;
    }

    // POO - Responsabilidad: la reserva protege su regla; solo admite consumos mientras tenga saldo pendiente.
    public void verificarAceptaConsumos() {
        if (!estado.permitePagos()) {
            throw new ReglaNegocioException("La reserva " + codigo
                    + " está cancelada y no admite compras de la tienda.");
        }
        if (getSaldo().signum() <= 0) {
            throw new ReglaNegocioException("La reserva " + codigo + " ya está pagada por completo. "
                    + "Registre la compra con factura individual.");
        }
    }

    // POO - Método: carga una venta de la tienda (factura unificada) a la cuenta de la reserva.
    public void registrarConsumo(Venta venta) {
        if (!codigo.equals(venta.getCodigoReserva())) {
            throw new ReglaNegocioException("La venta " + venta.getCodigo() + " no corresponde a la reserva "
                    + codigo + ".");
        }
        verificarAceptaConsumos();
        agregarConsumo(venta);
    }

    // POO - Método: una venta cargada solo puede anularse si lo ya pagado no supera el total que quedaría.
    public void verificarAnulacionConsumo(Venta venta) {
        BigDecimal totalSinVenta = getTotal().subtract(venta.getTotal());
        if (getTotalPagado().compareTo(totalSinVenta) > 0) {
            throw new ReglaNegocioException("No se puede anular la venta " + venta.getCodigo()
                    + ": la reserva " + codigo + " ya tiene pagado " + FormatoMoneda.formatear(getTotalPagado())
                    + " y sin esa venta su total sería " + FormatoMoneda.formatear(totalSinVenta) + ".");
        }
    }

    // POO - Colecciones: agrega al final del arreglo de consumos; si está lleno, lo agranda al doble.
    private void agregarConsumo(Venta venta) {
        if (cantidadConsumos == consumos.length) {
            Venta[] mayor = new Venta[consumos.length * 2];
            for (int i = 0; i < cantidadConsumos; i++) {
                mayor[i] = consumos[i];
            }
            consumos = mayor;
        }
        consumos[cantidadConsumos] = venta;
        cantidadConsumos++;
    }

    // POO - Colecciones: reemplaza el arreglo de consumos al reconstruir la reserva desde archivo.
    void reemplazarConsumos(Venta[] cargados) {
        consumos = new Venta[Math.max(CAPACIDAD_INICIAL_CONSUMOS, cargados.length)];
        cantidadConsumos = 0;
        for (Venta venta : cargados) {
            agregarConsumo(venta);
        }
        Arrays.sort(consumos, 0, cantidadConsumos, Comparator.comparing(Venta::getFechaHora));
    }

    public boolean confirmarSiCorresponde(BigDecimal anticipoMinimo) {
        if (estado == EstadoReserva.PENDIENTE && getTotalPagado().compareTo(anticipoMinimo) >= 0) {
            cambiarEstado(EstadoReserva.CONFIRMADA);
            return true;
        }
        return false;
    }

    public void cancelar(String motivo, Usuario usuario, LocalDateTime momento) {
        if (!estado.permiteCancelar()) {
            throw new ReglaNegocioException("Solo se pueden cancelar reservas pendientes o confirmadas. La reserva "
                    + codigo + " está " + estado.toString().toLowerCase() + ".");
        }
        Cancelacion datos = new Cancelacion(momento, motivo, usuario);
        cambiarEstado(EstadoReserva.CANCELADA);
        cancelacion = datos;
    }

    public boolean actualizarEstadoSegun(LocalDateTime ahora) {
        EstadoReserva anterior = estado;
        IntervaloHorario intervalo = getIntervalo();
        if (!ahora.isBefore(intervalo.inicio()) && estado.puedeTransicionarA(EstadoReserva.EN_CURSO)) {
            cambiarEstado(EstadoReserva.EN_CURSO);
        }
        if (!ahora.isBefore(intervalo.fin()) && estado.puedeTransicionarA(EstadoReserva.FINALIZADA)) {
            cambiarEstado(EstadoReserva.FINALIZADA);
        }
        return estado != anterior;
    }

    void reemplazarPagos(Pago[] cargados) {
        pagos = new Pago[Math.max(CAPACIDAD_INICIAL_PAGOS, cargados.length)];
        cantidadPagos = 0;
        for (Pago pago : cargados) {
            agregarPago(pago);
        }
        Arrays.sort(pagos, 0, cantidadPagos, Comparator.comparing(Pago::getFechaHora));
    }

    // --- Getters ---

    @Override
    public String getCodigo() {
        return codigo;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Cancha getCancha() {
        return cancha;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public Duration getDuracion() {
        return duracion;
    }

    public BigDecimal getSubtotalCancha() {
        return subtotalCancha;
    }

    // POO - Encapsulamiento: se devuelven copias; desde afuera no se puede alterar servicios ni pagos.
    public ReservaServicioItem[] getServicios() {
        return Arrays.copyOf(servicios, servicios.length);
    }

    public Pago[] getPagos() {
        return Arrays.copyOf(pagos, cantidadPagos);
    }

    // POO - Encapsulamiento / Colecciones: devuelve un arreglo nuevo solo con las ventas no anuladas.
    public Venta[] getConsumos() {
        Venta[] vigentes = new Venta[cantidadConsumos];
        int cantidad = 0;
        for (int i = 0; i < cantidadConsumos; i++) {
            if (consumos[i].getEstado() == Venta.Estado.REGISTRADA) {
                vigentes[cantidad] = consumos[i];
                cantidad++;
            }
        }
        return Arrays.copyOf(vigentes, cantidad);
    }

    public int getCantidadPagos() {
        return cantidadPagos;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Cancelacion getCancelacion() {
        return cancelacion;
    }

    public Usuario getUsuarioCreador() {
        return usuarioCreador;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getDescripcion() {
        return codigo + " · " + FormatoFecha.vista(fecha) + " " + horaInicio + "-" + getHoraFin()
                + " · " + cancha.getCodigo() + " " + cancha.getNombre() + " · " + cliente.getNombre();
    }
}
