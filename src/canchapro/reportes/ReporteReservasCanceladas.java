package canchapro.reportes;

import canchapro.reservas.Cancelacion;
import canchapro.reservas.EstadoReserva;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import canchapro.util.FormatoFecha;
import java.math.BigDecimal;

/**
 * Historial de reservas canceladas con motivo, responsable y lo que se había pagado.
 */
// POO - Herencia: hereda de ReporteBase las funciones comunes de los reportes.
// POO - Polimorfismo: sobrescribe getNombre(), filtrosAplicables() y generar().
public class ReporteReservasCanceladas extends ReporteBase {

    public ReporteReservasCanceladas(ReservaServicio reservaServicio) {
        super(reservaServicio);
    }

    @Override
    public String getNombre() {
        return "Reservas canceladas";
    }

    @Override
    public String getDescripcion() {
        return "Reservas canceladas del período (por fecha de la reserva): cuándo, quién y por qué se canceló.";
    }

    @Override
    public Filtro[] filtrosAplicables() {
        return new Filtro[]{Filtro.FECHAS, Filtro.CANCHA, Filtro.CLIENTE};
    }

    @Override
    public ResultadoReporte generar(FiltroReporte f) {
        ResultadoReporte r = new ResultadoReporte(getNombre(), periodo(f),
                "Reserva", "Fecha reserva", "Horario", "Cancha", "Cliente", "Cancelada el", "Por", "Motivo", "Pagado");
        FiltroReporte soloCanceladas = new FiltroReporte(f.desde(), f.hasta(), f.codigoCancha(),
                EstadoReserva.CANCELADA, f.textoCliente());
        Reserva[] canceladas = reservas(soloCanceladas);
        BigDecimal pagado = BigDecimal.ZERO;
        for (Reserva x : canceladas) {
            Cancelacion c = x.getCancelacion();
            r.agregarFila(x.getCodigo(), FormatoFecha.vista(x.getFecha()), x.getHoraInicio() + " - " + x.getHoraFin(),
                    x.getCancha().getCodigo(), x.getCliente().getNombre(), FormatoFecha.vista(c.fechaHora()),
                    c.usuario().getNombre(), c.motivo(), dinero(x.getTotalPagado()));
            pagado = pagado.add(x.getTotalPagado());
        }
        r.agregarTotal("Reservas canceladas", canceladas.length);
        r.agregarTotal("Monto pagado en reservas canceladas", dinero(pagado));
        return r;
    }
}
