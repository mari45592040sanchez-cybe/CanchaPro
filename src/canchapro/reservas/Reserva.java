package canchapro.reservas;

import canchapro.canchas.Cancha;
import canchapro.clientes.Cliente;
import canchapro.disponibilidad.IntervaloHorario;
import canchapro.pagos.Pago;
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

/**
 * Reserva de una cancha. Es el objeto central del sistema: conoce a su
 * cliente, su cancha, sus servicios y sus pagos, y protege sus reglas
 * (estados, pagos y cancelación).
 *
 * Los totales nunca se escriben a mano: se calculan a partir del precio de
 * la cancha y de los servicios guardados al crear la reserva.
 */
// POO - Polimorfismo (interfaz): implementa Identificable para poder guardarse en un RepositorioCsv.
// POO - Responsabilidad: la reserva protege sus propias reglas (estados, pagos y cancelación).
public class Reserva implements Identificable {

    private static final int CAPACIDAD_INICIAL_PAGOS = 4;

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

    // --- Cálculos ---

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

    public BigDecimal getTotal() {
        return subtotalCancha.add(getTotalServicios());
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

    // --- Reglas de negocio ---

    /**
     * Cambia el estado solo si la transición tiene sentido.
     *
     * @throws ReglaNegocioException si la transición no está permitida
     *                               (por ejemplo, de FINALIZADA a PENDIENTE)
     */
    public void cambiarEstado(EstadoReserva destino) {
        if (!estado.puedeTransicionarA(destino)) {
            throw new ReglaNegocioException("La reserva " + codigo + " está " + estado.toString().toLowerCase()
                    + " y no puede pasar a " + destino.toString().toLowerCase() + ".");
        }
        estado = destino;
    }

    /**
     * Registra un pago (anticipo o abono).
     *
     * @throws PagoInvalidoException si la reserva está cancelada, el pago no
     *                               es de esta reserva o supera el saldo pendiente
     */
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

    /**
     * Si la reserva está PENDIENTE y lo pagado alcanza el anticipo mínimo,
     * pasa a CONFIRMADA.
     *
     * @return true si cambió de estado
     */
    public boolean confirmarSiCorresponde(BigDecimal anticipoMinimo) {
        if (estado == EstadoReserva.PENDIENTE && getTotalPagado().compareTo(anticipoMinimo) >= 0) {
            cambiarEstado(EstadoReserva.CONFIRMADA);
            return true;
        }
        return false;
    }

    /**
     * Cancela la reserva conservándola en el historial (nunca se elimina).
     */
    public void cancelar(String motivo, Usuario usuario, LocalDateTime momento) {
        if (!estado.permiteCancelar()) {
            throw new ReglaNegocioException("Solo se pueden cancelar reservas pendientes o confirmadas. La reserva "
                    + codigo + " está " + estado.toString().toLowerCase() + ".");
        }
        Cancelacion datos = new Cancelacion(momento, motivo, usuario);
        cambiarEstado(EstadoReserva.CANCELADA);
        cancelacion = datos;
    }

    /**
     * Avanza el estado según la hora actual: al llegar la hora de inicio
     * pasa a EN_CURSO y al terminar pasa a FINALIZADA.
     *
     * @return true si el estado cambió
     */
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

    /** Reemplaza los pagos al reconstruir la reserva desde archivo. */
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

    /** "RES-00012 · 12/10/2026 18:00-19:00 · C02 El Maracaná · Juan Pérez" */
    public String getDescripcion() {
        return codigo + " · " + FormatoFecha.vista(fecha) + " " + horaInicio + "-" + getHoraFin()
                + " · " + cancha.getCodigo() + " " + cancha.getNombre() + " · " + cliente.getNombre();
    }
}
