package canchapro.reservas;

import canchapro.canchas.Cancha;
import canchapro.clientes.Cliente;
import canchapro.configuracion.Configuracion;
import canchapro.disponibilidad.DisponibilidadServicio;
import canchapro.disponibilidad.SolicitudDisponibilidad;
import canchapro.pagos.MetodoPago;
import canchapro.pagos.Pago;
import canchapro.pagos.PagoRepositorio;
import canchapro.tarifas.DetallePrecio;
import canchapro.usuarios.Usuario;
import canchapro.util.GeneradorCodigo;
import canchapro.util.Texto;
import canchapro.util.excepciones.CanchaProException;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import canchapro.util.excepciones.PagoInvalidoException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Reglas de negocio de las reservas: preparar, confirmar, cobrar, cancelar,
 * avanzar estados según la hora y consultar.
 */
// POO - Responsabilidad: reglas de negocio de reservas (doble validación de disponibilidad, anticipo, estados).
public class ReservaServicio {

    private static final String PREFIJO_RESERVA = "RES-";
    private static final String PREFIJO_PAGO = "PAG-";
    private static final Comparator<Reserva> CRONOLOGICO =
            Comparator.comparing(Reserva::getFecha).thenComparing(Reserva::getHoraInicio)
                    .thenComparing(r -> r.getCancha().getCodigo());

    // POO - Relaciones (composición): usa los repositorios de reservas y pagos, y el motor de disponibilidad.
    private final ReservaRepositorio reservas;
    private final PagoRepositorio pagos;
    private final DisponibilidadServicio disponibilidad;
    private final Configuracion configuracion;
    private final Clock reloj;

    public ReservaServicio(ReservaRepositorio reservas, PagoRepositorio pagos,
            DisponibilidadServicio disponibilidad, Configuracion configuracion, Clock reloj) {
        this.reservas = reservas;
        this.pagos = pagos;
        this.disponibilidad = disponibilidad;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    // --- Crear reservas ---

    /**
     * Calcula la reserva sin guardarla, para mostrar el resumen.
     * Valida cliente activo, disponibilidad de la cancha y servicios.
     */
    public ReservaBorrador prepararReserva(Cliente cliente, SolicitudDisponibilidad solicitud, Cancha cancha,
            SolicitudServicio[] serviciosSolicitados) {
        if (cliente == null) {
            throw new ReglaNegocioException("Seleccione o registre el cliente.");
        }
        if (!cliente.isActivo()) {
            throw new ReglaNegocioException("El cliente " + cliente.getNombre() + " está inactivo.");
        }
        if (cancha == null) {
            throw new ReglaNegocioException("Seleccione una cancha disponible.");
        }
        disponibilidad.verificarDisponible(cancha, solicitud, null);
        DetallePrecio precioCancha = disponibilidad.calcularPrecio(cancha, solicitud);

        // POO - Colecciones: un item por cada servicio solicitado, en un arreglo del mismo tamaño.
        ReservaServicioItem[] items = new ReservaServicioItem[serviciosSolicitados.length];
        BigDecimal total = precioCancha.total();
        for (int i = 0; i < serviciosSolicitados.length; i++) {
            SolicitudServicio s = serviciosSolicitados[i];
            if (!s.servicio().isActivo()) {
                throw new ReglaNegocioException("El servicio " + s.servicio().getNombre() + " ya no está disponible.");
            }
            items[i] = ReservaServicioItem.crear(s.servicio(), s.cantidad(), solicitud.duracion());
            total = total.add(items[i].subtotal());
        }
        return new ReservaBorrador(cliente, cancha, solicitud, precioCancha, items, total, anticipoMinimo(total));
    }

    /**
     * Guarda la reserva. Antes vuelve a verificar la disponibilidad, por si
     * otra persona reservó el mismo horario mientras se llenaba el formulario.
     *
     * @param montoInicial anticipo recibido, o null/0 si no se paga nada aún
     * @return la reserva creada (PENDIENTE, o CONFIRMADA si se pagó el anticipo mínimo)
     */
    public Reserva confirmarReserva(ReservaBorrador borrador, BigDecimal montoInicial, MetodoPago metodo,
            String observacion, Usuario usuario) {
        // Se recalcula todo con los datos actuales (no se confía en el borrador).
        ReservaServicioItem[] itemsBorrador = borrador.servicios();
        SolicitudServicio[] solicitados = new SolicitudServicio[itemsBorrador.length];
        for (int i = 0; i < itemsBorrador.length; i++) {
            solicitados[i] = new SolicitudServicio(itemsBorrador[i].servicio(), itemsBorrador[i].cantidad());
        }
        ReservaBorrador vigente = prepararReserva(borrador.cliente(), borrador.solicitud(), borrador.cancha(),
                solicitados);
        if (vigente.total().compareTo(borrador.total()) != 0) {
            throw new ReglaNegocioException("Los precios cambiaron desde que se preparó la reserva. "
                    + "Revise el resumen e intente de nuevo.");
        }
        boolean hayPago = montoInicial != null && montoInicial.signum() != 0;
        if (hayPago && montoInicial.compareTo(vigente.total()) > 0) {
            throw new PagoInvalidoException("El pago inicial no puede ser mayor que el total de la reserva.");
        }

        LocalDateTime ahora = LocalDateTime.now(reloj);
        String codigo = GeneradorCodigo.siguiente(PREFIJO_RESERVA, 5, reservas.codigos());
        SolicitudDisponibilidad s = vigente.solicitud();
        Reserva reserva = new Reserva(codigo, vigente.cliente(), vigente.cancha(), s.fecha(), s.horaInicio(),
                s.duracion(), vigente.precioCancha().total(), vigente.servicios(), EstadoReserva.PENDIENTE,
                usuario, ahora, null);

        Pago pago = null;
        if (hayPago) {
            pago = new Pago(siguienteCodigoPago(), codigo, ahora, montoInicial, metodo, observacion, usuario);
            reserva.registrarPago(pago);
        }
        reserva.confirmarSiCorresponde(anticipoMinimo(reserva.getTotal()));

        reservas.agregar(reserva);
        if (pago != null) {
            pagos.agregar(pago);
        }
        guardarTodo();
        return reserva;
    }

    // --- Pagos ---

    /**
     * Registra un abono. Si con él se alcanza el anticipo mínimo, la reserva
     * pasa de PENDIENTE a CONFIRMADA.
     */
    public Pago registrarPago(String codigoReserva, BigDecimal monto, MetodoPago metodo, String observacion,
            Usuario usuario) {
        Reserva reserva = obtener(codigoReserva);
        Pago pago = new Pago(siguienteCodigoPago(), codigoReserva, LocalDateTime.now(reloj), monto, metodo,
                observacion, usuario);
        reserva.registrarPago(pago);
        reserva.confirmarSiCorresponde(anticipoMinimo(reserva.getTotal()));
        pagos.agregar(pago);
        guardarTodo();
        return pago;
    }

    /** Monto que debe pagarse para que una reserva quede confirmada. */
    public BigDecimal anticipoMinimo(BigDecimal total) {
        return total.multiply(BigDecimal.valueOf(configuracion.getAnticipoMinimoPorcentaje()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String siguienteCodigoPago() {
        return GeneradorCodigo.siguiente(PREFIJO_PAGO, 5, pagos.codigos());
    }

    // --- Cancelar y estados ---

    /**
     * Cancela la reserva (solo PENDIENTE o CONFIRMADA). La reserva no se
     * elimina: queda en el historial con fecha, motivo y usuario, y su
     * horario se libera.
     */
    public void cancelar(String codigo, String motivo, Usuario usuario) {
        Reserva reserva = obtener(codigo);
        reserva.cancelar(motivo, usuario, LocalDateTime.now(reloj));
        reservas.actualizar(reserva);
        reservas.guardar();
    }

    /**
     * Pasa a EN_CURSO las reservas que ya empezaron y a FINALIZADA las que
     * ya terminaron. Se ejecuta al iniciar y cada minuto.
     *
     * @return cantidad de reservas que cambiaron de estado
     */
    public int actualizarEstadosAutomaticos() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        int cambios = 0;
        for (Reserva reserva : reservas.listar()) {
            if (reserva.actualizarEstadoSegun(ahora)) {
                cambios++;
            }
        }
        if (cambios > 0) {
            reservas.guardar();
        }
        return cambios;
    }

    // --- Consultas ---

    public Reserva obtener(String codigo) {
        return reservas.buscar(codigo)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe la reserva " + codigo));
    }

    public Reserva[] buscar(FiltroReservas f) {
        String texto = f.texto() == null ? "" : f.texto().trim();
        Reserva[] todas = reservas.listar();
        // POO - Colecciones: se copian a un arreglo temporal solo las que cumplen todos los filtros.
        Reserva[] encontradas = new Reserva[todas.length];
        int cantidad = 0;
        for (Reserva r : todas) {
            boolean cumple = (f.desde() == null || !r.getFecha().isBefore(f.desde()))
                    && (f.hasta() == null || !r.getFecha().isAfter(f.hasta()))
                    && (f.codigoCancha() == null || r.getCancha().getCodigo().equals(f.codigoCancha()))
                    && (f.estado() == null || r.getEstado() == f.estado())
                    && (texto.isEmpty() || Texto.contieneTodas(r.getCodigo() + " " + r.getCliente().getCodigo()
                            + " " + r.getCliente().getNombre() + " " + r.getCliente().getTelefono(), texto));
            if (cumple) {
                encontradas[cantidad] = r;
                cantidad++;
            }
        }
        Reserva[] resultado = Arrays.copyOf(encontradas, cantidad);
        Arrays.sort(resultado, CRONOLOGICO);
        return resultado;
    }

    public Reserva[] reservasDelDia(LocalDate fecha) {
        return buscar(new FiltroReservas(fecha, fecha, null, null, null));
    }

    public Reserva[] historialCliente(String codigoCliente) {
        Reserva[] todas = reservas.listar();
        Reserva[] encontradas = new Reserva[todas.length];
        int cantidad = 0;
        for (Reserva r : todas) {
            if (r.getCliente().getCodigo().equals(codigoCliente)) {
                encontradas[cantidad] = r;
                cantidad++;
            }
        }
        Reserva[] resultado = Arrays.copyOf(encontradas, cantidad);
        Arrays.sort(resultado, CRONOLOGICO.reversed());
        return resultado;
    }

    /**
     * Reservas aún no realizadas (pendientes o confirmadas, desde hoy) de una
     * cancha. Se usan para advertir al poner la cancha en mantenimiento.
     */
    public Reserva[] reservasFuturasActivas(String codigoCancha) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        Reserva[] todas = reservas.listar();
        Reserva[] encontradas = new Reserva[todas.length];
        int cantidad = 0;
        for (Reserva r : todas) {
            boolean activa = r.getEstado() == EstadoReserva.PENDIENTE || r.getEstado() == EstadoReserva.CONFIRMADA;
            if (r.getCancha().getCodigo().equals(codigoCancha) && activa && r.getIntervalo().fin().isAfter(ahora)) {
                encontradas[cantidad] = r;
                cantidad++;
            }
        }
        Reserva[] resultado = Arrays.copyOf(encontradas, cantidad);
        Arrays.sort(resultado, CRONOLOGICO);
        return resultado;
    }

    /** Descripción de las reservas futuras de una cancha (para el aviso de la pantalla de Canchas). */
    public String[] describirReservasFuturas(String codigoCancha) {
        Reserva[] futuras = reservasFuturasActivas(codigoCancha);
        String[] descripciones = new String[futuras.length];
        for (int i = 0; i < futuras.length; i++) {
            descripciones[i] = futuras[i].getDescripcion();
        }
        return descripciones;
    }

    private void guardarTodo() {
        // Primero los pagos: si fallara la escritura de reservas, al reiniciar
        // el pago sigue asociado a su reserva por el código.
        try {
            pagos.guardar();
            reservas.guardar();
        } catch (CanchaProException e) {
            reservas.cargar();
            throw e;
        }
    }
}
