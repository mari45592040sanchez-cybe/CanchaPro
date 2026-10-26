package canchapro.disponibilidad;

import java.time.LocalDate;

/**
 * Origen de los horarios ocupados de las canchas.
 *
 * El motor de disponibilidad depende de esta interfaz y no de las reservas
 * directamente: el módulo de Reservas la implementa (devolviendo solo las
 * reservas que no están canceladas) y, en el futuro, también podría
 * implementarla un registro de bloqueos por mantenimiento.
 */
// POO - Polimorfismo (interfaz): el motor de disponibilidad trabaja con cualquier clase que la implemente
// (OcupacionReservas), sin conocer las reservas directamente.
@FunctionalInterface
public interface FuenteOcupacion {

    /** Fuente sin ocupaciones (se usa mientras no existan reservas). */
    FuenteOcupacion VACIA = (codigoCancha, fecha) -> new Ocupacion[0];

    /**
     * Horarios ocupados de una cancha en una fecha.
     */
    Ocupacion[] ocupaciones(String codigoCancha, LocalDate fecha);
}
