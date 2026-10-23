package canchapro.usuarios;

/**
 * El administrador tiene acceso a todos los módulos del sistema.
 */
// POO - Herencia: Administrador ES UN Usuario (y por lo tanto una Persona).
public class Administrador extends Usuario {

    public Administrador(String codigo, String nombre, String telefono, String correo,
            String username, String passwordHash, String salt,
            boolean activo, boolean debeCambiarPassword) {
        super(codigo, nombre, telefono, correo, username, passwordHash, salt, activo, debeCambiarPassword);
    }

    @Override
    public Rol getRol() {
        return Rol.ADMINISTRADOR;
    }

    // POO - Polimorfismo: el administrador puede acceder a todos los módulos.
    @Override
    public boolean puedeAcceder(Modulo modulo) {
        return true;
    }
}
