package canchapro.reportes;

import canchapro.reservas.EstadoReserva;
import canchapro.reservas.FormatoReserva;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Cantidad de reservas, horas e importe por cada día del período.
 */
// POO - Herencia: hereda de ReporteBase las funciones comunes de los reportes.
// POO - Polimorfismo: sobrescribe getNombre(), filtrosAplicables() y generar().
public class ReporteReservasPorFecha extends ReporteBase {

    public ReporteReservasPorFecha(ReservaServicio reservaServicio) {
        super(reservaServicio);
    }

    @Override
    public String getNombre() {
        return "Reservas por fecha";
    }

    @Override
    public String getDescripcion() {
        return "Reservas, canceladas, horas e importe de cada día del período.";
    }

    @Override
    public Filtro[] filtrosAplicables() {
        return new Filtro[]{Filtro.FECHAS, Filtro.CANCHA, Filtro.CLIENTE};
    }

    @Override
    public ResultadoReporte generar(FiltroReporte f) {
        ResultadoReporte r = new ResultadoReporte(getNombre(), periodo(f),
                "Fecha", "Reservas", "Canceladas", "Horas reservadas", "Importe");
        Reserva[] todas = todasLasReservas(f);

        // POO - Colecciones: arreglo de fechas distintas (sin repetir), ordenado de la más antigua a la más reciente.
        LocalDate[] dias = new LocalDate[todas.length];
        int cantidadDias = 0;
        for (Reserva x : todas) {
            boolean yaEsta = false;
            for (int i = 0; i < cantidadDias; i++) {
                if (dias[i].equals(x.getFecha())) {
                    yaEsta = true;
                    break;
                }
            }
            if (!yaEsta) {
                dias[cantidadDias] = x.getFecha();
                cantidadDias++;
            }
        }
        dias = Arrays.copyOf(dias, cantidadDias);
        Arrays.sort(dias);

        int totalReservas = 0;
        int totalCanceladas = 0;
        for (LocalDate dia : dias) {
            Reserva[] activas = new Reserva[todas.length];
            int cantidadActivas = 0;
            int canceladas = 0;
            for (Reserva x : todas) {
                if (!x.getFecha().equals(dia)) {
                    continue;
                }
                if (x.getEstado().ocupaCancha()) {
                    activas[cantidadActivas] = x;
                    cantidadActivas++;
                } else if (x.getEstado() == EstadoReserva.CANCELADA) {
                    canceladas++;
                }
            }
            activas = Arrays.copyOf(activas, cantidadActivas);
            r.agregarFila(FormatoReserva.fechaLarga(dia), activas.length, canceladas,
                    horas(sumarMinutos(activas)), dinero(sumarTotales(activas)));
            totalReservas += activas.length;
            totalCanceladas += canceladas;
        }
        Reserva[] activas = reservas(f);
        r.agregarTotal("Reservas", totalReservas);
        r.agregarTotal("Canceladas", totalCanceladas);
        r.agregarTotal("Horas reservadas", horas(sumarMinutos(activas)));
        r.agregarTotal("Importe total", dinero(sumarTotales(activas)));
        return r;
    }
}
