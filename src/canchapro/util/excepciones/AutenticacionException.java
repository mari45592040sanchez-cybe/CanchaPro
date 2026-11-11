package canchapro.util.excepciones;

// POO - Herencia: excepción específica que hereda de CanchaProException.
public class AutenticacionException extends CanchaProException {

    public AutenticacionException(String mensaje) {
        super(mensaje);
    }
}
