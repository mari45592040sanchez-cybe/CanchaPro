package canchapro.usuarios;

// POO - Encapsulamiento: enumeración con su propio atributo (nombre visible) y comportamiento (toString).
public enum Rol {

    ADMINISTRADOR("Administrador"),
    EMPLEADO("Empleado");

    private final String nombreVisible;

    Rol(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}
