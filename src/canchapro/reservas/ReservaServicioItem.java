package canchapro.reservas;

import canchapro.servicios.ServicioAdicional;
import canchapro.util.excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

/**
 * Servicio adicional incluido en una reserva. Guarda el precio unitario y el
 * subtotal vigentes al momento de crear la reserva, de modo que cambios
 * posteriores en el catálogo no alteren el historial.
 *
 * @param servicio       servicio del catálogo
 * @param cantidad       unidades solicitadas
 * @param precioUnitario precio del servicio al crear la reserva
 * @param subtotal       importe calculado al crear la reserva
 */
// POO - Encapsulamiento: record inmutable; guarda el precio vigente al crear la reserva.
// POO - Relaciones: TIENE el servicio adicional que se agregó a la reserva.
public record ReservaServicioItem(ServicioAdicional servicio, int cantidad, BigDecimal precioUnitario,
        BigDecimal subtotal) {

    public ReservaServicioItem {
        Objects.requireNonNull(servicio, "El servicio es obligatorio");
        if (cantidad < 1) {
            throw new DatosInvalidosException("La cantidad debe ser mayor que cero.");
        }
    }

    /** Crea el ítem calculando el importe con el precio actual del servicio. */
    public static ReservaServicioItem crear(ServicioAdicional servicio, int cantidad, Duration duracion) {
        return new ReservaServicioItem(servicio, cantidad, servicio.getPrecio(),
                servicio.calcularImporte(cantidad, duracion));
    }
}
