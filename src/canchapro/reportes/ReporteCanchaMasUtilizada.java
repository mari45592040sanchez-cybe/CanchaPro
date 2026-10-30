package canchapro.reportes;

import canchapro.canchas.Cancha;
import canchapro.canchas.CanchaServicio;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Ranking de canchas por horas reservadas y porcentaje de ocupación.
 */
// POO - Herencia: hereda de ReporteBase las funciones comunes de los reportes.
// POO - Polimorfismo: sobrescribe getNombre(), filtrosAplicables() y generar().
public class ReporteCanchaMasUtilizada extends ReporteBase {

    private final CanchaServicio canchaServicio;

    public ReporteCanchaMasUtilizada(ReservaServicio reservaServicio, CanchaServicio canchaServicio) {
        super(reservaServicio);
        this.canchaServicio = canchaServicio;
    }

    @Override
    public String getNombre() {
        return "Cancha más utilizada";
    }

    @Override
    public String getDescripcion() {
        return "Canchas ordenadas por horas reservadas. Ocupación = horas reservadas / horas de atención del período.";
    }

    @Override
    public Filtro[] filtrosAplicables() {
        return new Filtro[]{Filtro.FECHAS};
    }

    /** Datos de una cancha para ordenar el ranking. */
    // POO - Encapsulamiento: record privado e inmutable, solo lo usa este reporte.
    private record Uso(Cancha cancha, int reservas, long minutos) {
    }

    @Override
    public ResultadoReporte generar(FiltroReporte f) {
        ResultadoReporte r = new ResultadoReporte(getNombre(), periodo(f),
                "Posición", "Cancha", "Tipo", "Reservas", "Horas reservadas", "Ocupación");
        Reserva[] reservas = reservas(f);
        long dias = ChronoUnit.DAYS.between(f.desde(), f.hasta()) + 1;

        // POO - Colecciones: un objeto Uso por cancha, en un arreglo que luego se ordena.
        Cancha[] canchas = canchaServicio.listar();
        Uso[] usos = new Uso[canchas.length];
        for (int i = 0; i < canchas.length; i++) {
            Reserva[] deCancha = deCancha(reservas, canchas[i].getCodigo());
            usos[i] = new Uso(canchas[i], deCancha.length, sumarMinutos(deCancha));
        }
        Arrays.sort(usos, Comparator.comparingLong(Uso::minutos).reversed().thenComparing(u -> u.cancha().getCodigo()));

        int posicion = 1;
        for (Uso u : usos) {
            long minutosAbiertos = dias * Duration.between(u.cancha().getHoraApertura(), u.cancha().getHoraCierre()).toMinutes();
            r.agregarFila(posicion++, u.cancha().getCodigo() + " " + u.cancha().getNombre(), u.cancha().getTipo(),
                    u.reservas(), horas(u.minutos()), porcentaje(u.minutos(), minutosAbiertos));
        }
        if (usos.length > 0 && usos[0].minutos() > 0) {
            Uso primera = usos[0];
            r.agregarTotal("Cancha más utilizada", primera.cancha().getCodigo() + " " + primera.cancha().getNombre()
                    + " (" + horas(primera.minutos()) + ")");
        } else {
            r.agregarTotal("Cancha más utilizada", "sin reservas en el período");
        }
        r.agregarTotal("Días del período", dias);
        return r;
    }
}
