package canchapro.usuarios;

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
