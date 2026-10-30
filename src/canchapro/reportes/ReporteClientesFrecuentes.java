package canchapro.reportes;

import canchapro.clientes.Cliente;
import canchapro.reservas.Reserva;
import canchapro.reservas.ReservaServicio;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Clientes ordenados por cantidad de reservas (y por monto en caso de empate).
 */
// POO - Herencia: hereda de ReporteBase las funciones comunes de los reportes.
// POO - Polimorfismo: sobrescribe getNombre(), filtrosAplicables() y generar().
public class ReporteClientesFrecuentes extends ReporteBase {

    public ReporteClientesFrecuentes(ReservaServicio reservaServicio) {
        super(reservaServicio);
    }

    @Override
    public String getNombre() {
        return "Clientes con más reservas";
    }

    @Override
    public String getDescripcion() {
        return "Ranking de clientes por cantidad de reservas en el período (sin contar canceladas).";
    }

    @Override
    public Filtro[] filtrosAplicables() {
        return new Filtro[]{Filtro.FECHAS, Filtro.CANCHA};
    }

    @Override
    public ResultadoReporte generar(FiltroReporte f) {
        ResultadoReporte r = new ResultadoReporte(getNombre(), periodo(f),
                "Posición", "Cliente", "Teléfono", "Reservas", "Horas", "Monto total");
        Reserva[] reservas = reservas(f);

        // POO - Colecciones: un objeto Frecuencia por cliente distinto (se acumula al recorrer las reservas).
        Frecuencia[] frecuencias = new Frecuencia[reservas.length];
        int cantidad = 0;
        for (Reserva x : reservas) {
            int indice = -1;
            for (int i = 0; i < cantidad; i++) {
                if (frecuencias[i].cliente.getCodigo().equals(x.getCliente().getCodigo())) {
                    indice = i;
                    break;
                }
            }
            if (indice < 0) {
                frecuencias[cantidad] = new Frecuencia(x.getCliente());
                indice = cantidad;
                cantidad++;
            }
            frecuencias[indice].agregar(x);
        }
        Frecuencia[] ranking = Arrays.copyOf(frecuencias, cantidad);
        Arrays.sort(ranking, Comparator.comparingInt((Frecuencia e) -> e.reservas).reversed()
                .thenComparing(e -> e.monto, Comparator.reverseOrder())
                .thenComparing(e -> e.cliente.getNombre()));

        int posicion = 1;
        for (Frecuencia e : ranking) {
            r.agregarFila(posicion++, e.cliente.getCodigo() + " " + e.cliente.getNombre(), e.cliente.getTelefono(),
                    e.reservas, horas(e.minutos), dinero(e.monto));
        }
        r.agregarTotal("Clientes", ranking.length);
        if (ranking.length > 0) {
            r.agregarTotal("Cliente más frecuente", ranking[0].cliente.getNombre()
                    + " (" + ranking[0].reservas + " reservas)");
        }
        return r;
    }

    /** Totales acumulados de un cliente. */
    // POO - Encapsulamiento: clase privada que solo usa este reporte. POO - Relaciones: TIENE un Cliente.
    private static final class Frecuencia {

        private final Cliente cliente;
        private int reservas;
        private long minutos;
        private BigDecimal monto = BigDecimal.ZERO;

        Frecuencia(Cliente cliente) {
            this.cliente = cliente;
        }

        void agregar(Reserva reserva) {
            reservas++;
            minutos += reserva.getDuracion().toMinutes();
            monto = monto.add(reserva.getTotal());
        }
    }
}
