package canchapro.util.excepciones;

/**
 * Se lanza cuando el inicio de sesión falla.
 */
public class AutenticacionException extends CanchaProException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}
