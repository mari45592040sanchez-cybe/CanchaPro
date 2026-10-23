package canchapro.login;

import canchapro.usuarios.Modulo;
import canchapro.usuarios.Usuario;
import java.time.LocalDateTime;

/**
 * Usuario autenticado y momento en que inició sesión.
 * Se pasa por constructor a las pantallas que lo necesitan.
 */
// POO - Relaciones (asociación): la sesión TIENE el usuario que inició sesión.
public class Sesion {

    private final Usuario usuario;
    private final LocalDateTime inicio;

    public Sesion(Usuario usuario) {
        this.usuario = usuario;
        this.inicio = LocalDateTime.now();
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    // POO - Polimorfismo: delega en usuario.puedeAcceder(); se ejecuta la versión de Administrador o de Empleado.
    public boolean puedeAcceder(Modulo modulo) {
        return usuario.puedeAcceder(modulo);
    }
}
