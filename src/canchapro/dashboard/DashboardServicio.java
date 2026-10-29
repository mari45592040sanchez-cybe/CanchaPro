package canchapro.dashboard;

import canchapro.canchas.Cancha;
import canchapro.canchas.CanchaServicio;
import canchapro.pagos.PagoServicio;
import canchapro.reservas.EstadoReserva;
import canchapro.reservas.FiltroReservas;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Calcula los indicadores del día para la pantalla de inicio.
 */
// POO - Responsabilidad: solo calcula los indicadores; la pantalla solo los muestra.
public class DashboardServicio {

    // POO - Relaciones (composición): usa los servicios de reservas, pagos y canchas.
    private final ReservaServicio reservaServicio;
    private final PagoServicio pagoServicio;
    private final CanchaServicio canchaServicio;
    private final Clock reloj;

    public DashboardServicio(ReservaServicio reservaServicio, PagoServicio pagoServicio,
            CanchaServicio canchaServicio, Clock reloj) {
        this.reservaServicio = reservaServicio;
        this.pagoServicio = pagoServicio;
        this.canchaServicio = canchaServicio;
        this.reloj = reloj;
    }

    public ResumenDashboard resumen() {
        LocalDate hoy = LocalDate.now(reloj);
        Reserva[] deHoy = reservasDeHoy();

        int enCurso = 0;
        for (Reserva r : deHoy) {
            if (r.getEstado() == EstadoReserva.EN_CURSO) {
                enCurso++;
            }
        }

        // Pendientes y saldo se calculan sobre todas las reservas no canceladas.
        int pendientes = 0;
        BigDecimal saldoPorCobrar = BigDecimal.ZERO;
        for (Reserva r : reservaServicio.buscar(new FiltroReservas(null, null, null, null, null))) {
            if (!r.getEstado().ocupaCancha()) {
                continue;
            }
            if (r.getEstado() == EstadoReserva.PENDIENTE && !r.getFecha().isBefore(hoy)) {
                pendientes++;
            }
            if (r.getSaldo().signum() > 0) {
                saldoPorCobrar = saldoPorCobrar.add(r.getSaldo());
            }
        }

        return new ResumenDashboard(hoy, deHoy.length, enCurso, pendientes, pagoServicio.ingresosDeHoy(),
                saldoPorCobrar, porcentajeOcupacion(deHoy));
    }

    /** Reservas de hoy no canceladas, en orden de hora. */
    public Reserva[] reservasDeHoy() {
        Reserva[] delDia = reservaServicio.reservasDelDia(LocalDate.now(reloj));
        // POO - Colecciones: se copian a un arreglo temporal solo las no canceladas.
        Reserva[] activas = new Reserva[delDia.length];
        int cantidad = 0;
        for (Reserva r : delDia) {
            if (r.getEstado().ocupaCancha()) {
                activas[cantidad] = r;
                cantidad++;
            }
        }
        return Arrays.copyOf(activas, cantidad);
    }

    /**
     * Porcentaje de las horas de atención de hoy (de las canchas disponibles)
     * que ya están reservadas.
     */
    private int porcentajeOcupacion(Reserva[] deHoy) {
        Cancha[] reservables = canchaServicio.listarReservables();
        long minutosAbiertos = 0;
        for (Cancha c : reservables) {
            minutosAbiertos += Duration.between(c.getHoraApertura(), c.getHoraCierre()).toMinutes();
        }
        if (minutosAbiertos == 0) {
            return 0;
        }
        long minutosReservados = 0;
        for (Reserva r : deHoy) {
            if (esReservable(r.getCancha().getCodigo(), reservables)) {
                minutosReservados += r.getDuracion().toMinutes();
            }
        }
        return (int) Math.round(minutosReservados * 100.0 / minutosAbiertos);
    }

    /** Búsqueda secuencial del código en el arreglo de canchas reservables. */
    private static boolean esReservable(String codigoCancha, Cancha[] reservables) {
        for (Cancha c : reservables) {
            if (c.getCodigo().equals(codigoCancha)) {
                return true;
            }
        }
        return false;
    }
}
