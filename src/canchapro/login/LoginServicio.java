package canchapro.login;

import canchapro.usuarios.Usuario;
import canchapro.usuarios.UsuarioRepositorio;
import canchapro.util.excepciones.AutenticacionException;
import canchapro.util.excepciones.DatosInvalidosException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Inicio de sesión y cambio de contraseña.
 */
// POO - Responsabilidad: solo se encarga de autenticar y de bloquear tras 3 intentos fallidos.
public class LoginServicio {

    private static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos.";
    private static final int MAX_INTENTOS = 3;
    private static final Duration TIEMPO_BLOQUEO = Duration.ofSeconds(60);

    // POO - Relaciones (composición): usa el repositorio de usuarios para buscar al usuario.
    private final UsuarioRepositorio repositorio;
    /** Intentos fallidos por nombre de usuario (solo en memoria). */
    // POO - Colecciones: arreglo de objetos IntentosFallidos + contador; cada uno guarda su username.
    private IntentosFallidos[] intentos = new IntentosFallidos[10];
    private int cantidadIntentos;

    public LoginServicio(UsuarioRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * Valida las credenciales y devuelve la sesión iniciada.
     * El mensaje de error es el mismo si el usuario no existe o la
     * contraseña es incorrecta, para no revelar qué usuarios existen.
     *
     * @throws AutenticacionException si las credenciales no son válidas
     */
    public Sesion autenticar(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new AutenticacionException("Ingrese usuario y contraseña.");
        }
        String clave = username.trim().toLowerCase(Locale.ROOT);
        IntentosFallidos registro = obtenerIntentos(clave);
        registro.verificarBloqueo();

        Optional<Usuario> encontrado = repositorio.buscarPorUsername(username);
        if (encontrado.isEmpty() || !encontrado.get().verificarPassword(password)) {
            registro.registrarFallo();
            throw new AutenticacionException(MENSAJE_CREDENCIALES);
        }
        Usuario usuario = encontrado.get();
        if (!usuario.isActivo()) {
            throw new AutenticacionException("El usuario está desactivado. Contacte al administrador.");
        }
        quitarIntentos(clave);
        return new Sesion(usuario);
    }

    /** Busca el registro de intentos del username; si no existe, lo crea al final del arreglo. */
    private IntentosFallidos obtenerIntentos(String clave) {
        for (int i = 0; i < cantidadIntentos; i++) {
            if (intentos[i].clave.equals(clave)) {
                return intentos[i];
            }
        }
        if (cantidadIntentos == intentos.length) {
            intentos = Arrays.copyOf(intentos, intentos.length * 2);
        }
        IntentosFallidos nuevo = new IntentosFallidos(clave);
        intentos[cantidadIntentos] = nuevo;
        cantidadIntentos++;
        return nuevo;
    }

    /** Al iniciar sesión correctamente se borran sus intentos (los siguientes se corren una posición). */
    private void quitarIntentos(String clave) {
        for (int i = 0; i < cantidadIntentos; i++) {
            if (intentos[i].clave.equals(clave)) {
                for (int j = i; j < cantidadIntentos - 1; j++) {
                    intentos[j] = intentos[j + 1];
                }
                cantidadIntentos--;
                intentos[cantidadIntentos] = null;
                return;
            }
        }
    }

    /**
     * Cambia la contraseña del usuario después de verificar la actual.
     */
    public void cambiarPassword(Usuario usuario, String actual, String nueva, String confirmacion) {
        if (!usuario.verificarPassword(actual)) {
            throw new ReglaNegocioException("La contraseña actual no es correcta.");
        }
        Usuario.validarPassword(nueva);
        if (!nueva.equals(confirmacion)) {
            throw new DatosInvalidosException("La confirmación no coincide con la nueva contraseña.");
        }
        if (nueva.equals(actual)) {
            throw new ReglaNegocioException("La nueva contraseña debe ser distinta a la actual.");
        }
        usuario.establecerPassword(nueva, false);
        repositorio.actualizar(usuario);
        repositorio.guardar();
    }

    /** Control de intentos fallidos de un nombre de usuario. */
    // POO - Encapsulamiento: clase privada; solo LoginServicio controla los intentos.
    private static class IntentosFallidos {

        private final String clave;
        private int cantidad;
        private LocalDateTime bloqueadoHasta;

        IntentosFallidos(String clave) {
            this.clave = clave;
        }

        void verificarBloqueo() {
            if (bloqueadoHasta == null) {
                return;
            }
            LocalDateTime ahora = LocalDateTime.now();
            if (ahora.isBefore(bloqueadoHasta)) {
                long segundos = Math.max(1, Duration.between(ahora, bloqueadoHasta).toSeconds());
                throw new AutenticacionException("Demasiados intentos fallidos. Intente de nuevo en "
                        + segundos + " segundos.");
            }
            bloqueadoHasta = null;
            cantidad = 0;
        }

        void registrarFallo() {
            cantidad++;
            if (cantidad >= MAX_INTENTOS) {
                bloqueadoHasta = LocalDateTime.now().plus(TIEMPO_BLOQUEO);
            }
        }
    }
}
