package canchapro.disponibilidad;

import java.time.LocalDate;

// POO - Polimorfismo (interfaz): el motor de disponibilidad trabaja con cualquier clase que la implemente
// (OcupacionReservas), sin conocer las reservas directamente.
@FunctionalInterface
public interface FuenteOcupacion {

    FuenteOcupacion VACIA = (codigoCancha, fecha) -> new Ocupacion[0];

    Ocupacion[] ocupaciones(String codigoCancha, LocalDate fecha);
}
