package canchapro.tienda;

import canchapro.configuracion.Configuracion;
import canchapro.reportes.FiltroReporte;
import canchapro.reportes.Reporte;
import canchapro.reportes.ResultadoReporte;
import canchapro.util.FormatoFecha;
import canchapro.util.FormatoMoneda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Reporte de la tienda: productos más vendidos y total de ventas del
 * período. Solo aparece en la lista si el módulo Tienda está activo.
 */
// POO - Polimorfismo (interfaz): implementa Reporte, así aparece en la pantalla de Reportes sin que
// el módulo Reportes dependa de la Tienda. Sobrescribe estaDisponible() según la configuración.
public class ReporteVentasTienda implements Reporte {

    private final TiendaServicio tiendaServicio;
    private final Configuracion configuracion;

    public ReporteVentasTienda(TiendaServicio tiendaServicio, Configuracion configuracion) {
        this.tiendaServicio = tiendaServicio;
        this.configuracion = configuracion;
    }

    @Override
    public String getNombre() {
        return "Tienda: productos más vendidos";
    }

    @Override
    public String getDescripcion() {
        return "Unidades e importe vendido por producto en el período (sin ventas anuladas), y total de ventas.";
    }

    @Override
    public Filtro[] filtrosAplicables() {
        return new Filtro[]{Filtro.FECHAS};
    }

    @Override
    public boolean estaDisponible() {
        return configuracion.isModuloTienda();
    }

    /** Acumulado de un producto. */
    // POO - Encapsulamiento: clase privada que solo usa este reporte. POO - Relaciones: TIENE un Producto.
    private static final class Acumulado {

        private final Producto producto;
        private int unidades;
        private BigDecimal importe = BigDecimal.ZERO;

        Acumulado(Producto producto) {
            this.producto = producto;
        }
    }

    @Override
    public ResultadoReporte generar(FiltroReporte f) {
        String periodo = FormatoFecha.vista(f.desde()) + " al " + FormatoFecha.vista(f.hasta());
        ResultadoReporte r = new ResultadoReporte(getNombre(), periodo,
                "Posición", "Producto", "Categoría", "Unidades", "Importe");

        Venta[] ventas = tiendaServicio.ventasPorPeriodo(f.desde(), f.hasta());
        int registradas = 0;
        // POO - Colecciones: un objeto Acumulado por producto distinto, en un arreglo que crece al doble.
        Acumulado[] porProducto = new Acumulado[10];
        int cantidad = 0;
        for (Venta v : ventas) {
            if (v.getEstado() != Venta.Estado.REGISTRADA) {
                continue;
            }
            registradas++;
            for (DetalleVenta d : v.getDetalles()) {
                int indice = -1;
                for (int i = 0; i < cantidad; i++) {
                    if (porProducto[i].producto.getCodigo().equals(d.getProducto().getCodigo())) {
                        indice = i;
                        break;
                    }
                }
                if (indice < 0) {
                    if (cantidad == porProducto.length) {
                        porProducto = Arrays.copyOf(porProducto, porProducto.length * 2);
                    }
                    porProducto[cantidad] = new Acumulado(d.getProducto());
                    indice = cantidad;
                    cantidad++;
                }
                porProducto[indice].unidades += d.getCantidad();
                porProducto[indice].importe = porProducto[indice].importe.add(d.getSubtotal());
            }
        }

        Acumulado[] ranking = Arrays.copyOf(porProducto, cantidad);
        Arrays.sort(ranking, Comparator.comparingInt((Acumulado a) -> a.unidades).reversed()
                .thenComparing(a -> a.producto.getNombre()));
        int posicion = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (Acumulado a : ranking) {
            r.agregarFila(posicion++, a.producto.getCodigo() + " " + a.producto.getNombre(), a.producto.getCategoria(),
                    a.unidades, FormatoMoneda.formatear(a.importe));
            total = total.add(a.importe);
        }

        r.agregarTotal("Ventas", registradas);
        r.agregarTotal("Anuladas", ventas.length - registradas);
        r.agregarTotal("TOTAL VENDIDO", FormatoMoneda.formatear(total));
        if (registradas > 0) {
            r.agregarTotal("Ticket promedio", FormatoMoneda.formatear(
                    total.divide(BigDecimal.valueOf(registradas), 2, RoundingMode.HALF_UP)));
        }
        return r;
    }

    @Override
    public String toString() {
        return getNombre();
    }
}
