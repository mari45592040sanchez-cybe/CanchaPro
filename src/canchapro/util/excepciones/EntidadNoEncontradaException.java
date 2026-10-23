package canchapro.util.excepciones;

/**
 * Se lanza cuando se busca un registro por código y no existe.
 */
// POO - Herencia: excepción específica que hereda de CanchaProException.
public class EntidadNoEncontradaException extends CanchaProException {

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
