package canchapro.usuarios;

// POO - Encapsulamiento: cada módulo guarda su nombre visible y si es exclusivo del administrador.
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

    public boolean isSoloAdministrador() {
        return soloAdministrador;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
