package canchapro.reportes;

import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import canchapro.util.FormatoFecha;
import java.math.BigDecimal;

/**
 * Reservas no canceladas que todavía tienen saldo por cobrar.
 */
// POO - Herencia: hereda de ReporteBase las funciones comunes de los reportes.
// POO - Polimorfismo: sobrescribe getNombre(), filtrosAplicables() y generar().
public class ReportePagosPendientes extends ReporteBase {

    public ReportePagosPendientes(ReservaServicio reservaServicio) {
        super(reservaServicio);
    }

    @Override
    public String getNombre() {
        return "Pagos pendientes";
    }

    @Override
    public String getDescripcion() {
        return "Reservas no canceladas con saldo pendiente, con los datos de contacto del cliente.";
    }

    @Override
    public Filtro[] filtrosAplicables() {
        return new Filtro[]{Filtro.FECHAS, Filtro.CANCHA, Filtro.CLIENTE};
    }

    @Override
    public ResultadoReporte generar(FiltroReporte f) {
        ResultadoReporte r = new ResultadoReporte(getNombre(), periodo(f),
                "Reserva", "Fecha", "Horario", "Cancha", "Cliente", "Teléfono", "Estado", "Total", "Pagado", "Saldo");
        int conSaldo = 0;
        BigDecimal saldoTotal = BigDecimal.ZERO;
        for (Reserva x : reservas(f)) {
            if (x.getSaldo().signum() <= 0) {
                continue;
            }
            r.agregarFila(x.getCodigo(), FormatoFecha.vista(x.getFecha()), x.getHoraInicio() + " - " + x.getHoraFin(),
                    x.getCancha().getCodigo(), x.getCliente().getNombre(), x.getCliente().getTelefono(),
                    x.getEstado(), dinero(x.getTotal()), dinero(x.getTotalPagado()), dinero(x.getSaldo()));
            saldoTotal = saldoTotal.add(x.getSaldo());
            conSaldo++;
        }
        r.agregarTotal("Reservas con saldo", conSaldo);
        r.agregarTotal("SALDO TOTAL POR COBRAR", dinero(saldoTotal));
        return r;
    }
}
