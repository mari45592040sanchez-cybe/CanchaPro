package canchapro.reservas;

import canchapro.servicios.ServicioAdicional;
import canchapro.util.excepciones.DatosInvalidosException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;

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

    public static ReservaServicioItem crear(ServicioAdicional servicio, int cantidad, Duration duracion) {
        return new ReservaServicioItem(servicio, cantidad, servicio.getPrecio(),
                servicio.calcularImporte(cantidad, duracion));
    }
}
