package canchapro.util.excepciones;

/**
 * Se lanza cuando el inicio de sesión falla.
 */
// POO - Herencia: excepción específica que hereda de CanchaProException.
public class AutenticacionException extends CanchaProException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}
