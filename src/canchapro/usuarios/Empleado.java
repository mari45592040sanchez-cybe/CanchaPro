package canchapro.usuarios;

import canchapro.util.excepciones.ReglaNegocioException;

/**
 * El empleado solo accede a los módulos que el administrador le asignó.
 */
// POO - Herencia: Empleado ES UN Usuario (y por lo tanto una Persona).
public class Empleado extends Usuario {

    // POO - Colecciones / Relaciones: el empleado TIENE un arreglo con los módulos que le asignó el administrador.
    private Modulo[] modulosAsignados = new Modulo[0];

    public Empleado(String codigo, String nombre, String telefono, String correo,
            String username, String passwordHash, String salt,
            boolean activo, boolean debeCambiarPassword, Modulo[] modulos) {
        super(codigo, nombre, telefono, correo, username, passwordHash, salt, activo, debeCambiarPassword);
        asignarModulos(modulos);
    }

    @Override
    public Rol getRol() {
        return Rol.EMPLEADO;
    }

    // POO - Polimorfismo: el empleado solo accede a los módulos que tiene asignados.
    @Override
    public boolean puedeAcceder(Modulo modulo) {
        return !modulo.isSoloAdministrador() && tieneModulo(modulo);
    }

    /**
     * Reemplaza los módulos asignados.
     *
     * @throws ReglaNegocioException si se intenta asignar un módulo exclusivo del administrador
     */
    public final void asignarModulos(Modulo[] modulos) {
        for (Modulo modulo : modulos) {
            if (modulo.isSoloAdministrador()) {
                throw new ReglaNegocioException("El módulo '" + modulo + "' es exclusivo del administrador.");
            }
        }
        // Se guardan sin repetir y en el orden del menú: se recorren todos los módulos
        // y se toman los que vienen en el arreglo recibido.
        Modulo[] todos = Modulo.values();
        Modulo[] elegidos = new Modulo[todos.length];
        int cantidad = 0;
        for (Modulo modulo : todos) {
            if (contiene(modulos, modulo)) {
                elegidos[cantidad] = modulo;
                cantidad++;
            }
        }
        modulosAsignados = new Modulo[cantidad];
        for (int i = 0; i < cantidad; i++) {
            modulosAsignados[i] = elegidos[i];
        }
    }

    /** true si el módulo está asignado (búsqueda secuencial). */
    public boolean tieneModulo(Modulo modulo) {
        return contiene(modulosAsignados, modulo);
    }

    private static boolean contiene(Modulo[] modulos, Modulo buscado) {
        for (Modulo m : modulos) {
            if (m == buscado) {
                return true;
            }
        }
        return false;
    }

    // POO - Encapsulamiento: se devuelve una copia; desde afuera no se puede modificar el arreglo interno.
    public Modulo[] getModulos() {
        Modulo[] copia = new Modulo[modulosAsignados.length];
        for (int i = 0; i < modulosAsignados.length; i++) {
            copia[i] = modulosAsignados[i];
        }
        return copia;
    }
}
