package canchapro.reportes;

import canchapro.pagos.MetodoPago;
import canchapro.pagos.Pago;
import canchapro.pagos.PagoServicio;
import canchapro.reservas.FormatoReserva;
import canchapro.reservas.ReservaServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Dinero recibido (pagos) por día y por método de pago. Con el período de
 * un mes completo es el reporte mensual de ingresos.
 */
// POO - Herencia: hereda de ReporteBase las funciones comunes de los reportes.
// POO - Polimorfismo: sobrescribe getNombre(), filtrosAplicables() y generar().
public class ReporteIngresosPorPeriodo extends ReporteBase {

    private final PagoServicio pagoServicio;

    public ReporteIngresosPorPeriodo(ReservaServicio reservaServicio, PagoServicio pagoServicio) {
        super(reservaServicio);
        this.pagoServicio = pagoServicio;
    }

    @Override
    public String getNombre() {
        return "Ingresos por período";
    }

    @Override
    public String getDescripcion() {
        return "Pagos recibidos por día y por método de pago (se cuentan en la fecha en que se recibieron). "
                + "Use \"Este mes\" para el reporte mensual.";
    }

    @Override
    public Filtro[] filtrosAplicables() {
        return new Filtro[]{Filtro.FECHAS};
    }

    @Override
    public ResultadoReporte generar(FiltroReporte f) {
        MetodoPago[] metodos = MetodoPago.values();
        // Columnas: Fecha, un método de pago por columna, Pagos y Total del día.
        String[] columnas = new String[metodos.length + 3];
        columnas[0] = "Fecha";
        for (int i = 0; i < metodos.length; i++) {
            columnas[i + 1] = metodos[i].toString();
        }
        columnas[metodos.length + 1] = "Pagos";
        columnas[metodos.length + 2] = "Total del día";
        ResultadoReporte r = new ResultadoReporte(getNombre(), periodo(f), columnas);

        // Los pagos llegan ordenados por fecha y hora: se recorren agrupando los del mismo día.
        Pago[] pagos = pagoServicio.pagosPorPeriodo(f.desde(), f.hasta());
        // POO - Colecciones: total por método de pago; la posición es el ordinal del método.
        BigDecimal[] totalesMetodo = new BigDecimal[metodos.length];
        Arrays.fill(totalesMetodo, BigDecimal.ZERO);
        BigDecimal totalGeneral = BigDecimal.ZERO;

        int i = 0;
        while (i < pagos.length) {
            LocalDate dia = pagos[i].getFechaHora().toLocalDate();
            BigDecimal[] montosDia = new BigDecimal[metodos.length];
            Arrays.fill(montosDia, BigDecimal.ZERO);
            int pagosDia = 0;
            while (i < pagos.length && pagos[i].getFechaHora().toLocalDate().equals(dia)) {
                int m = pagos[i].getMetodo().ordinal();
                montosDia[m] = montosDia[m].add(pagos[i].getMonto());
                pagosDia++;
                i++;
            }
            Object[] fila = new Object[metodos.length + 3];
            fila[0] = FormatoReserva.fechaLarga(dia);
            BigDecimal totalDia = BigDecimal.ZERO;
            for (int m = 0; m < metodos.length; m++) {
                fila[m + 1] = dinero(montosDia[m]);
                totalesMetodo[m] = totalesMetodo[m].add(montosDia[m]);
                totalDia = totalDia.add(montosDia[m]);
            }
            fila[metodos.length + 1] = pagosDia;
            fila[metodos.length + 2] = dinero(totalDia);
            r.agregarFila(fila);
            totalGeneral = totalGeneral.add(totalDia);
        }

        for (int m = 0; m < metodos.length; m++) {
            r.agregarTotal(metodos[m].toString(), dinero(totalesMetodo[m]));
        }
        r.agregarTotal("Pagos", pagos.length);
        r.agregarTotal("TOTAL INGRESOS", dinero(totalGeneral));
        return r;
    }
}
