package canchapro.reportes;

import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Cantidad de reservas según la hora de inicio y el día de la semana.
 */
// POO - Herencia: hereda de ReporteBase las funciones comunes de los reportes.
// POO - Polimorfismo: sobrescribe getNombre(), filtrosAplicables() y generar().
public class ReporteHorariosMayorDemanda extends ReporteBase {

    private static final Locale ES = Locale.of("es");

    public ReporteHorariosMayorDemanda(ReservaServicio reservaServicio) {
        super(reservaServicio);
    }

    @Override
    public String getNombre() {
        return "Horarios de mayor demanda";
    }

    @Override
    public String getDescripcion() {
        return "Reservas por hora de inicio (filas) y día de la semana (columnas). Ayuda a ubicar las horas pico.";
    }

    @Override
    public Filtro[] filtrosAplicables() {
        return new Filtro[]{Filtro.FECHAS, Filtro.CANCHA};
    }

    @Override
    public ResultadoReporte generar(FiltroReporte f) {
        DayOfWeek[] diasSemana = DayOfWeek.values();
        String[] columnas = new String[diasSemana.length + 2];
        columnas[0] = "Hora de inicio";
        for (int d = 0; d < diasSemana.length; d++) {
            columnas[d + 1] = diasSemana[d].getDisplayName(TextStyle.SHORT, ES);
        }
        columnas[diasSemana.length + 1] = "Total";
        ResultadoReporte r = new ResultadoReporte(getNombre(), periodo(f), columnas);

        // POO - Colecciones: matriz [hora 0-23][día 0-6] con la cantidad de reservas,
        // y un arreglo con el total por día de la semana.
        int[][] conteo = new int[24][diasSemana.length];
        int[] totalPorDia = new int[diasSemana.length];
        for (Reserva x : reservas(f)) {
            int hora = x.getHoraInicio().getHour();
            int dia = x.getFecha().getDayOfWeek().ordinal();
            conteo[hora][dia]++;
            totalPorDia[dia]++;
        }

        int horaPico = -1;
        int maximo = 0;
        for (int hora = 0; hora < 24; hora++) {
            int total = 0;
            for (int d = 0; d < diasSemana.length; d++) {
                total += conteo[hora][d];
            }
            if (total == 0) {
                continue; // Solo se muestran las horas con reservas.
            }
            Object[] valores = new Object[diasSemana.length + 2];
            valores[0] = String.format("%02d:00 - %02d:59", hora, hora);
            for (int d = 0; d < diasSemana.length; d++) {
                valores[d + 1] = conteo[hora][d] == 0 ? "" : conteo[hora][d];
            }
            valores[diasSemana.length + 1] = total;
            r.agregarFila(valores);
            if (total > maximo) {
                maximo = total;
                horaPico = hora;
            }
        }

        if (horaPico < 0) {
            r.agregarTotal("Hora de mayor demanda", "sin reservas en el período");
        } else {
            r.agregarTotal("Hora de mayor demanda", String.format("%02d:00 (%d reservas)", horaPico, maximo));
            int diaPico = 0;
            for (int d = 1; d < diasSemana.length; d++) {
                if (totalPorDia[d] > totalPorDia[diaPico]) {
                    diaPico = d;
                }
            }
            r.agregarTotal("Día de mayor demanda", diasSemana[diaPico].getDisplayName(TextStyle.FULL, ES)
                    + " (" + totalPorDia[diaPico] + " reservas)");
        }
        return r;
    }
}
