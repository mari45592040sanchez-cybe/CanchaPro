package canchapro.login;

import canchapro.usuarios.Modulo;
import canchapro.usuarios.Usuario;
import java.time.LocalDateTime;

/**
 * Usuario autenticado y momento en que inició sesión.
 * Se pasa por constructor a las pantallas que lo necesitan.
 */
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

    public boolean puedeAcceder(Modulo modulo) {
        return usuario.puedeAcceder(modulo);
    }
}
