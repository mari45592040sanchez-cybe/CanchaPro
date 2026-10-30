package canchapro.reportes;

import canchapro.reservas.FiltroReservas;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import canchapro.util.BloquesHorario;
import canchapro.util.FormatoFecha;
import canchapro.util.FormatoMoneda;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;

/**
 * Funciones comunes a todos los reportes: obtener las reservas que cumplen
 * el filtro y dar formato a montos y horas.
 */
// POO - Herencia / Reutilización: clase abstracta que heredan los 8 reportes; las funciones comunes
// (filtrar reservas, sumar, dar formato) se escriben una sola vez.
// POO - Polimorfismo: implementa Reporte; cada subclase define getNombre(), filtrosAplicables() y generar().
public abstract class ReporteBase implements Reporte {

    // POO - Encapsulamiento: protected, para que solo las subclases (los reportes) lo usen.
    protected final ReservaServicio reservaServicio;

    protected ReporteBase(ReservaServicio reservaServicio) {
        this.reservaServicio = reservaServicio;
    }

    /**
     * Reservas que cumplen el filtro. Si no se filtra por estado, se excluyen
     * las canceladas (no ocuparon la cancha ni generan ingresos).
     */
    protected Reserva[] reservas(FiltroReporte f) {
        Reserva[] todas = todasLasReservas(f);
        Reserva[] resultado = new Reserva[todas.length];
        int cantidad = 0;
        for (Reserva r : todas) {
            if (f.estado() != null || r.getEstado().ocupaCancha()) {
                resultado[cantidad] = r;
                cantidad++;
            }
        }
        return Arrays.copyOf(resultado, cantidad);
    }

    /** Reservas de una cancha dentro del arreglo recibido. */
    protected static Reserva[] deCancha(Reserva[] reservas, String codigoCancha) {
        Reserva[] resultado = new Reserva[reservas.length];
        int cantidad = 0;
        for (Reserva r : reservas) {
            if (r.getCancha().getCodigo().equals(codigoCancha)) {
                resultado[cantidad] = r;
                cantidad++;
            }
        }
        return Arrays.copyOf(resultado, cantidad);
    }

    /** Reservas que cumplen el filtro, incluidas las canceladas. */
    protected Reserva[] todasLasReservas(FiltroReporte f) {
        return reservaServicio.buscar(new FiltroReservas(f.desde(), f.hasta(), f.codigoCancha(), f.estado(),
                f.textoCliente()));
    }

    /** "01/10/2026 al 31/10/2026" */
    protected static String periodo(FiltroReporte f) {
        if (f.desde() == null || f.hasta() == null) {
            return "Todas las fechas";
        }
        return f.desde().equals(f.hasta())
                ? FormatoFecha.vista(f.desde())
                : FormatoFecha.vista(f.desde()) + " al " + FormatoFecha.vista(f.hasta());
    }

    protected static String dinero(BigDecimal monto) {
        return FormatoMoneda.formatear(monto);
    }

    protected static String horas(long minutos) {
        return minutos == 0 ? "0 h" : BloquesHorario.describir(Duration.ofMinutes(minutos));
    }

    protected static BigDecimal sumarTotales(Reserva[] reservas) {
        BigDecimal total = BigDecimal.ZERO;
        for (Reserva r : reservas) {
            total = total.add(r.getTotal());
        }
        return total;
    }

    protected static long sumarMinutos(Reserva[] reservas) {
        long minutos = 0;
        for (Reserva r : reservas) {
            minutos += r.getDuracion().toMinutes();
        }
        return minutos;
    }

    /** Porcentaje con un decimal: 12.5 % */
    protected static String porcentaje(double parte, double total) {
        return total == 0 ? "0 %" : String.format("%.1f %%", parte * 100.0 / total);
    }

    @Override
    public String toString() {
        return getNombre();
    }
}
