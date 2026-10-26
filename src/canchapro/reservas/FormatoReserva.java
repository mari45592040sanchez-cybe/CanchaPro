package canchapro.reservas;

import canchapro.pagos.Pago;
import canchapro.tarifas.TramoTarifa;
import canchapro.util.BloquesHorario;
import canchapro.util.FormatoFecha;
import canchapro.util.FormatoMoneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Textos de resumen de reservas (para pantallas y comprobantes).
 * No contiene reglas de negocio: solo da formato a datos ya calculados.
 */
// POO - Responsabilidad: solo da formato de texto; no contiene reglas de negocio.
public final class FormatoReserva {

    private static final Locale ES = Locale.of("es");
    private static final String LINEA = "-".repeat(52);

    private FormatoReserva() {
    }

    /** Resumen previo a confirmar una reserva. */
    public static String resumen(ReservaBorrador b, int porcentajeAnticipo) {
        StringBuilder sb = new StringBuilder();
        sb.append(campo("Cliente", b.cliente().getDescripcion()));
        sb.append(campo("Cancha", b.cancha().toString()));
        sb.append(campo("Fecha", fechaLarga(b.solicitud().fecha())));
        sb.append(campo("Horario", b.solicitud().horaInicio() + " a " + b.solicitud().horaFin()
                + " (" + BloquesHorario.describir(b.solicitud().duracion()) + ")"));
        sb.append('\n').append("Uso de la cancha\n");
        for (TramoTarifa t : b.precioCancha().tramos()) {
            sb.append(importe("  " + t.horaDesde() + "-" + t.horaHasta() + "  " + t.minutos() + " min x "
                    + FormatoMoneda.formatear(t.precioHora()) + "/h", t.subtotal()));
        }
        agregarServicios(sb, b.servicios());
        sb.append(LINEA).append('\n');
        sb.append(importe("TOTAL", b.total()));
        sb.append(importe("Anticipo mínimo (" + porcentajeAnticipo + "%)", b.anticipoMinimo()));
        return sb.toString();
    }

    /** Detalle completo de una reserva guardada. */
    public static String detalle(Reserva r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getCodigo()).append("   Estado: ").append(r.getEstado().toString().toUpperCase()).append("\n\n");
        sb.append(campo("Cliente", r.getCliente().getDescripcion()));
        sb.append(campo("Cancha", r.getCancha().toString()));
        sb.append(campo("Fecha", fechaLarga(r.getFecha())));
        sb.append(campo("Horario", r.getHoraInicio() + " a " + r.getHoraFin()
                + " (" + BloquesHorario.describir(r.getDuracion()) + ")"));
        sb.append(campo("Creada", FormatoFecha.vista(r.getFechaCreacion()) + " por " + r.getUsuarioCreador().getNombre()));
        sb.append('\n');
        sb.append(importe("Uso de la cancha", r.getSubtotalCancha()));
        agregarServicios(sb, r.getServicios());
        sb.append(LINEA).append('\n');
        sb.append(importe("TOTAL", r.getTotal()));

        sb.append("\nPagos\n");
        if (r.getCantidadPagos() == 0) {
            sb.append("  (sin pagos)\n");
        }
        for (Pago p : r.getPagos()) {
            sb.append(importe("  " + p.getCodigo() + " " + FormatoFecha.vista(p.getFechaHora()) + " "
                    + p.getMetodo(), p.getMonto()));
        }
        sb.append(importe("Pagado", r.getTotalPagado()));
        sb.append(importe("SALDO", r.getSaldo()));

        Cancelacion c = r.getCancelacion();
        if (c != null) {
            sb.append('\n').append("CANCELADA el ").append(FormatoFecha.vista(c.fechaHora()))
                    .append(" por ").append(c.usuario().getNombre()).append('\n');
            sb.append("Motivo: ").append(c.motivo()).append('\n');
        }
        return sb.toString();
    }

    public static String fechaLarga(LocalDate fecha) {
        String dia = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, ES);
        return dia + " " + FormatoFecha.vista(fecha);
    }

    private static void agregarServicios(StringBuilder sb, ReservaServicioItem[] servicios) {
        if (servicios.length == 0) {
            return;
        }
        sb.append("Servicios adicionales\n");
        for (ReservaServicioItem i : servicios) {
            sb.append(importe("  " + i.servicio().getNombre() + " x" + i.cantidad()
                    + " (" + i.servicio().getTipoCobro().toString().toLowerCase() + ")", i.subtotal()));
        }
    }

    private static String campo(String nombre, String valor) {
        return String.format("%-9s %s%n", nombre + ":", valor);
    }

    private static String importe(String concepto, BigDecimal monto) {
        return String.format("%-38s %13s%n", concepto, FormatoMoneda.formatear(monto));
    }
}
