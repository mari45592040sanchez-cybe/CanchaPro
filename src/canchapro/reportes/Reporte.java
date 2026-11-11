package canchapro.reportes;

/**
 * Un reporte del sistema. La pantalla de reportes trabaja solo con esta
 * interfaz: no sabe qué reporte concreto está generando (polimorfismo).
 */
// POO - Polimorfismo (interfaz): los 8 reportes la implementan; la pantalla y el servicio
// los tratan a todos como "Reporte", sin conocer su clase concreta.
public interface Reporte {

    enum Filtro {
        FECHAS, CANCHA, ESTADO, CLIENTE
    }

    String getNombre();

    String getDescripcion();

    // POO - Colecciones: arreglo con los filtros que usa el reporte.
    Filtro[] filtrosAplicables();

    ResultadoReporte generar(FiltroReporte filtro);

    /**
     * Indica si el reporte se muestra. Los reportes de módulos opcionales
     * (como la Tienda) lo sobrescriben según la configuración.
     */
    default boolean estaDisponible() {
        return true;
    }

    // POO - Reutilización: método por defecto que heredan todos los reportes.
    default boolean usaFiltro(Filtro filtro) {
        for (Filtro f : filtrosAplicables()) {
            if (f == filtro) {
                return true;
            }
        }
        return false;
    }
}
