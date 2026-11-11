package canchapro.util.excepciones;

// POO - Herencia: excepción específica que hereda de CanchaProException.
public class EntidadNoEncontradaException extends CanchaProException {

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
