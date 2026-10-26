package canchapro.reservas;

import canchapro.disponibilidad.FuenteOcupacion;
import canchapro.disponibilidad.Ocupacion;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Entrega al motor de disponibilidad los horarios ocupados por reservas.
 * Las reservas CANCELADAS no ocupan la cancha.
 */
// POO - Polimorfismo (interfaz): implementa FuenteOcupacion; el motor de disponibilidad la usa sin conocer las reservas.
public class OcupacionReservas implements FuenteOcupacion {

    // POO - Relaciones: usa el repositorio de reservas para saber qué horarios están ocupados.
    private final ReservaRepositorio repositorio;

    public OcupacionReservas(ReservaRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Ocupacion[] ocupaciones(String codigoCancha, LocalDate fecha) {
        Reserva[] reservas = repositorio.listar();
        Ocupacion[] ocupaciones = new Ocupacion[reservas.length];
        int cantidad = 0;
        for (Reserva r : reservas) {
            if (r.getEstado().ocupaCancha() && r.getCancha().getCodigo().equals(codigoCancha)
                    && r.getFecha().equals(fecha)) {
                ocupaciones[cantidad] = new Ocupacion(codigoCancha, r.getIntervalo(), r.getCodigo(),
                        r.getCliente().getNombre());
                cantidad++;
            }
        }
        return Arrays.copyOf(ocupaciones, cantidad);
    }
}
