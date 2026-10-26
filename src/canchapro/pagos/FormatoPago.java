package canchapro.pagos;

import canchapro.reservas.FormatoReserva;
import canchapro.reservas.Reserva;
import canchapro.util.FormatoFecha;
import canchapro.util.FormatoMoneda;
import java.math.BigDecimal;

/**
 * Texto del comprobante de pago (para mostrar e imprimir).
 */
// POO - Responsabilidad: solo da formato al comprobante; no contiene reglas de negocio.
public final class FormatoPago {

    private static final int ANCHO = 48;
    private static final String LINEA = "-".repeat(ANCHO);

    private FormatoPago() {
    }

    public static String comprobante(Pago pago, Reserva reserva, String nombreNegocio) {
        BigDecimal pagadoAntes = BigDecimal.ZERO;
        for (Pago p : reserva.getPagos()) {
            if (p.getCodigo().equals(pago.getCodigo())) {
                break;
            }
            pagadoAntes = pagadoAntes.add(p.getMonto());
        }
        BigDecimal pagadoDespues = pagadoAntes.add(pago.getMonto());

        StringBuilder sb = new StringBuilder();
        sb.append(centrar(nombreNegocio)).append('\n');
        sb.append(centrar("COMPROBANTE DE PAGO")).append("\n\n");
        sb.append(campo("Pago", pago.getCodigo()));
        sb.append(campo("Fecha", FormatoFecha.vista(pago.getFechaHora())));
        sb.append(campo("Recibió", pago.getUsuario().getNombre()));
        if (!pago.getObservacion().isBlank()) {
            sb.append(campo("Nota", pago.getObservacion()));
        }
        sb.append(LINEA).append('\n');
        sb.append(campo("Reserva", reserva.getCodigo()));
        sb.append(campo("Cliente", reserva.getCliente().getNombre() + " (" + reserva.getCliente().getTelefono() + ")"));
        sb.append(campo("Cancha", reserva.getCancha().getCodigo() + " " + reserva.getCancha().getNombre()));
        sb.append(campo("Fecha", FormatoReserva.fechaLarga(reserva.getFecha())));
        sb.append(campo("Horario", reserva.getHoraInicio() + " a " + reserva.getHoraFin()));
        sb.append(LINEA).append('\n');
        sb.append(importe("Total de la reserva", reserva.getTotal()));
        sb.append(importe("Pagado anteriormente", pagadoAntes));
        sb.append(importe("ESTE PAGO (" + pago.getMetodo() + ")", pago.getMonto()));
        sb.append(importe("Total pagado", pagadoDespues));
        sb.append(importe("SALDO PENDIENTE", reserva.getTotal().subtract(pagadoDespues)));
        sb.append(LINEA).append('\n');
        sb.append(centrar("¡Gracias por su preferencia!")).append('\n');
        return sb.toString();
    }

    private static String centrar(String texto) {
        int espacios = Math.max(0, (ANCHO - texto.length()) / 2);
        return " ".repeat(espacios) + texto;
    }

    private static String campo(String nombre, String valor) {
        return String.format("%-9s %s%n", nombre + ":", valor);
    }

    private static String importe(String concepto, BigDecimal monto) {
        return String.format("%-34s %13s%n", concepto, FormatoMoneda.formatear(monto));
    }
}
