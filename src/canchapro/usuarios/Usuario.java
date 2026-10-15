package canchapro.usuarios;

import canchapro.util.SeguridadPassword;
import canchapro.util.Validador;
import canchapro.util.excepciones.DatosInvalidosException;
import java.util.regex.Pattern;

/**
 * Persona que puede iniciar sesión en el sistema.
 * Cada subclase decide a qué módulos tiene acceso (polimorfismo).
 */
public abstract class Usuario extends Persona {

    private static final Pattern FORMATO_USERNAME = Pattern.compile("[A-Za-z0-9._]{4,20}");

    private String username;
    private String passwordHash;
    private String salt;
    private boolean activo;
    private boolean debeCambiarPassword;

    protected Usuario(String codigo, String nombre, String telefono, String correo,
            String username, String passwordHash, String salt,
            boolean activo, boolean debeCambiarPassword) {
        super(codigo, nombre, telefono, correo);
        setUsername(username);
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.activo = activo;
        this.debeCambiarPassword = debeCambiarPassword;
    }

    /** Rol del usuario, usado para guardarlo en archivo y mostrarlo. */
    public abstract Rol getRol();

    /** Indica si el usuario puede ver y usar el módulo indicado. */
    public abstract boolean puedeAcceder(Modulo modulo);

    // --- Validaciones ---

    public static String validarUsername(String username) {
        String limpio = Validador.requerido(username, "El nombre de usuario");
        if (!FORMATO_USERNAME.matcher(limpio).matches()) {
            throw new DatosInvalidosException(
                    "El nombre de usuario debe tener de 4 a 20 caracteres: letras, números, punto o guion bajo.");
        }
        return limpio;
    }

    public static void validarPassword(String password) {
        if (password == null || password.length() < 6 || password.length() > 50) {
            throw new DatosInvalidosException("La contraseña debe tener entre 6 y 50 caracteres.");
        }
    }

    // --- Contraseña ---

    public boolean verificarPassword(String password) {
        return password != null && SeguridadPassword.verificar(password, salt, passwordHash);
    }

    /**
     * Guarda una nueva contraseña (como hash con un salt nuevo).
     *
     * @param debeCambiar true si el usuario deberá cambiarla en su próximo ingreso
     */
    public void establecerPassword(String password, boolean debeCambiar) {
        validarPassword(password);
        this.salt = SeguridadPassword.generarSalt();
        this.passwordHash = SeguridadPassword.hash(password, salt);
        this.debeCambiarPassword = debeCambiar;
    }

    // --- Getters y setters ---

    public String getUsername() {
        return username;
    }

    public final void setUsername(String username) {
        this.username = validarUsername(username);
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }
}
