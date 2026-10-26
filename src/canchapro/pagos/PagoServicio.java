package canchapro.pagos;

import canchapro.configuracion.Configuracion;
import canchapro.reservas.FiltroReservas;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import canchapro.usuarios.Usuario;
import canchapro.util.excepciones.DatosInvalidosException;
import canchapro.util.excepciones.EntidadNoEncontradaException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Consultas de pagos e ingresos. El registro de un pago se delega en
 * {@link ReservaServicio}, porque los pagos forman parte de la reserva y es
 * ella quien valida el saldo y su estado.
 *
 * Los ingresos se cuentan por la fecha en que se recibió el pago.
 */
// POO - Responsabilidad: consultas de pagos e ingresos; el registro del pago lo valida la reserva.
public class PagoServicio {

    // POO - Relaciones (composición): usa el repositorio de pagos y el servicio de reservas.
    private final PagoRepositorio pagos;
    private final ReservaServicio reservas;
    private final Configuracion configuracion;
    private final Clock reloj;

    public PagoServicio(PagoRepositorio pagos, ReservaServicio reservas, Configuracion configuracion, Clock reloj) {
        this.pagos = pagos;
        this.reservas = reservas;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    public Pago registrarPago(String codigoReserva, BigDecimal monto, MetodoPago metodo, String observacion,
            Usuario usuario) {
        return reservas.registrarPago(codigoReserva, monto, metodo, observacion, usuario);
    }

    /**
     * Reservas para cobrar, buscadas por código o cliente.
     *
     * @param soloConSaldo true para mostrar solo reservas no canceladas con saldo pendiente
     */
    public Reserva[] buscarReservas(String texto, boolean soloConSaldo) {
        Reserva[] todas = reservas.buscar(new FiltroReservas(null, null, null, null, texto));
        // POO - Colecciones: se copian a un arreglo temporal solo las que cumplen el filtro.
        Reserva[] encontradas = new Reserva[todas.length];
        int cantidad = 0;
        for (Reserva r : todas) {
            if (!soloConSaldo || (r.getEstado().permitePagos() && r.getSaldo().signum() > 0)) {
                encontradas[cantidad] = r;
                cantidad++;
            }
        }
        Reserva[] resultado = Arrays.copyOf(encontradas, cantidad);
        Arrays.sort(resultado, Comparator.comparing(Reserva::getFecha).reversed().thenComparing(Reserva::getHoraInicio));
        return resultado;
    }

    public Pago[] historialPagos(String codigoReserva) {
        return reservas.obtener(codigoReserva).getPagos();
    }

    public BigDecimal saldoPendiente(String codigoReserva) {
        return reservas.obtener(codigoReserva).getSaldo();
    }

    public Pago obtener(String codigoPago) {
        return pagos.buscar(codigoPago)
                .orElseThrow(() -> new EntidadNoEncontradaException("No existe el pago " + codigoPago));
    }

    /** Pagos recibidos entre dos fechas (incluidas), del más antiguo al más reciente. */
    public Pago[] pagosPorPeriodo(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        Pago[] encontrados = pagos.listarPorPeriodo(desde, hasta);
        Arrays.sort(encontrados, Comparator.comparing(Pago::getFechaHora));
        return encontrados;
    }

    public BigDecimal ingresosPorPeriodo(LocalDate desde, LocalDate hasta) {
        BigDecimal total = BigDecimal.ZERO;
        for (Pago p : pagosPorPeriodo(desde, hasta)) {
            total = total.add(p.getMonto());
        }
        return total;
    }

    /**
     * Total recibido por cada método de pago. La posición del arreglo es la
     * del método en {@code MetodoPago.values()} (se usa su ordinal como índice).
     */
    public BigDecimal[] ingresosPorMetodo(LocalDate desde, LocalDate hasta) {
        BigDecimal[] totales = new BigDecimal[MetodoPago.values().length];
        for (int i = 0; i < totales.length; i++) {
            totales[i] = BigDecimal.ZERO;
        }
        for (Pago p : pagosPorPeriodo(desde, hasta)) {
            int indice = p.getMetodo().ordinal();
            totales[indice] = totales[indice].add(p.getMonto());
        }
        return totales;
    }

    public BigDecimal ingresosDeHoy() {
        LocalDate hoy = LocalDate.now(reloj);
        return ingresosPorPeriodo(hoy, hoy);
    }

    /** Texto del comprobante de un pago. */
    public String comprobante(String codigoPago) {
        Pago pago = obtener(codigoPago);
        return FormatoPago.comprobante(pago, reservas.obtener(pago.getCodigoReserva()), configuracion.getNombreNegocio());
    }

    private static void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new DatosInvalidosException("Indique un período válido (la fecha final no puede ser anterior a la inicial).");
        }
    }
}
