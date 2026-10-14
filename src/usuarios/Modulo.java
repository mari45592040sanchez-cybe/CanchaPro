package canchapro.usuarios;

/**
 * Módulos (pantallas) del sistema. El orden de declaración es el orden en
 * que aparecen en el menú principal.
 */
public enum Modulo {

    DASHBOARD("Inicio", false),
    DISPONIBILIDAD("Disponibilidad", false),
    RESERVAS("Reservas", false),
    PAGOS("Pagos", false),
    CLIENTES("Clientes", false),
    CANCHAS("Canchas", false),
    TARIFAS("Tarifas", false),
    SERVICIOS("Servicios adicionales", false),
    REPORTES("Reportes", false),
    TIENDA("Tienda / Cafetería", false),
    USUARIOS("Usuarios", true),
    CONFIGURACION("Configuración", true);

    private final String nombreVisible;
    private final boolean soloAdministrador;

    Modulo(String nombreVisible, boolean soloAdministrador) {
        this.nombreVisible = nombreVisible;
        this.soloAdministrador = soloAdministrador;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    /** Los módulos exclusivos del administrador no se pueden asignar a empleados. */
    public boolean isSoloAdministrador() {
        return soloAdministrador;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
