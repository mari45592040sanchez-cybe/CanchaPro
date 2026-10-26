package canchapro.reservas;

import canchapro.canchas.Cancha;
import canchapro.clientes.Cliente;
import canchapro.disponibilidad.SolicitudDisponibilidad;
import canchapro.tarifas.DetallePrecio;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * Reserva calculada pero todavía no guardada: sirve para mostrar el resumen
 * (tramos de tarifa, servicios, total y anticipo mínimo) antes de confirmar.
 *
 * @param cliente        cliente
 * @param cancha         cancha elegida
 * @param solicitud      fecha, hora y duración
 * @param precioCancha   detalle del cobro por uso de la cancha
 * @param servicios      servicios adicionales con su importe
 * @param total          total a pagar
 * @param anticipoMinimo monto mínimo para que la reserva quede CONFIRMADA
 */
// POO - Encapsulamiento: record inmutable; el arreglo de servicios se copia al crear y al consultar.
// POO - Relaciones: agrupa el cliente, la cancha, la solicitud y el detalle del precio.
public record ReservaBorrador(Cliente cliente, Cancha cancha, SolicitudDisponibilidad solicitud,
        DetallePrecio precioCancha, ReservaServicioItem[] servicios, BigDecimal total, BigDecimal anticipoMinimo) {

    public ReservaBorrador {
        servicios = Arrays.copyOf(servicios, servicios.length);
    }

    @Override
    public ReservaServicioItem[] servicios() {
        return Arrays.copyOf(servicios, servicios.length);
    }
}
