package canchapro.login;

import canchapro.usuarios.Usuario;
import canchapro.usuarios.UsuarioRepositorio;
import canchapro.util.excepciones.AutenticacionException;
import canchapro.util.excepciones.DatosInvalidosException;
import canchapro.util.excepciones.ReglaNegocioException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Inicio de sesión y cambio de contraseña.
 */
public class LoginServicio {

    private static final String MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos.";
    private static final int MAX_INTENTOS = 3;
    private static final Duration TIEMPO_BLOQUEO = Duration.ofSeconds(60);

    private final UsuarioRepositorio repositorio;
    /** Intentos fallidos por nombre de usuario (solo en memoria). */
    private final Map<String, IntentosFallidos> intentos = new HashMap<>();

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
        IntentosFallidos registro = intentos.computeIfAbsent(clave, k -> new IntentosFallidos());
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
        intentos.remove(clave);
        return new Sesion(usuario);
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
    private static class IntentosFallidos {

        private int cantidad;
        private LocalDateTime bloqueadoHasta;

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
