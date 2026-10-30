package canchapro.reportes;

import canchapro.canchas.Cancha;
import canchapro.canchas.CanchaServicio;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;

/**
 * Reservas, horas e importe de cada cancha en el período.
 */
// POO - Herencia: hereda de ReporteBase las funciones comunes de los reportes.
// POO - Polimorfismo: sobrescribe getNombre(), filtrosAplicables() y generar().
public class ReporteReservasPorCancha extends ReporteBase {

    private final CanchaServicio canchaServicio;

    public ReporteReservasPorCancha(ReservaServicio reservaServicio, CanchaServicio canchaServicio) {
        super(reservaServicio);
        this.canchaServicio = canchaServicio;
    }

    @Override
    public String getNombre() {
        return "Reservas por cancha";
    }

    @Override
    public String getDescripcion() {
        return "Cantidad de reservas, horas e importe de cada cancha. Sin filtro de estado se excluyen las canceladas.";
    }

    @Override
    public Filtro[] filtrosAplicables() {
        return new Filtro[]{Filtro.FECHAS, Filtro.ESTADO};
    }

    @Override
    public ResultadoReporte generar(FiltroReporte f) {
        ResultadoReporte r = new ResultadoReporte(getNombre(), periodo(f) + (f.estado() == null ? "" : " · " + f.estado()),
                "Cancha", "Tipo", "Reservas", "% de reservas", "Horas reservadas", "Importe");
        Reserva[] reservas = reservas(f);
        for (Cancha cancha : canchaServicio.listar()) {
            Reserva[] deCancha = deCancha(reservas, cancha.getCodigo());
            r.agregarFila(cancha.getCodigo() + " " + cancha.getNombre(), cancha.getTipo(), deCancha.length,
                    porcentaje(deCancha.length, reservas.length), horas(sumarMinutos(deCancha)),
                    dinero(sumarTotales(deCancha)));
        }
        r.agregarTotal("Reservas", reservas.length);
        r.agregarTotal("Horas reservadas", horas(sumarMinutos(reservas)));
        r.agregarTotal("Importe total", dinero(sumarTotales(reservas)));
        return r;
    }
}
