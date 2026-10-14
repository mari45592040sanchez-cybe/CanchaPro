package canchapro.usuarios;

import canchapro.util.excepciones.ReglaNegocioException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * El empleado solo accede a los módulos que el administrador le asignó.
 */
public class Empleado extends Usuario {

    private final Set<Modulo> modulosAsignados = EnumSet.noneOf(Modulo.class);

    public Empleado(String codigo, String nombre, String telefono, String correo,
            String username, String passwordHash, String salt,
            boolean activo, boolean debeCambiarPassword, Set<Modulo> modulos) {
        super(codigo, nombre, telefono, correo, username, passwordHash, salt, activo, debeCambiarPassword);
        asignarModulos(modulos);
    }

    @Override
    public Rol getRol() {
        return Rol.EMPLEADO;
    }

    @Override
    public boolean puedeAcceder(Modulo modulo) {
        return !modulo.isSoloAdministrador() && modulosAsignados.contains(modulo);
    }

    /**
     * Reemplaza los módulos asignados.
     *
     * @throws ReglaNegocioException si se intenta asignar un módulo exclusivo del administrador
     */
    public final void asignarModulos(Set<Modulo> modulos) {
        for (Modulo modulo : modulos) {
            if (modulo.isSoloAdministrador()) {
                throw new ReglaNegocioException("El módulo '" + modulo + "' es exclusivo del administrador.");
            }
        }
        modulosAsignados.clear();
        modulosAsignados.addAll(modulos);
    }

    public Set<Modulo> getModulos() {
        return Collections.unmodifiableSet(modulosAsignados);
    }
}
