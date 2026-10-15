package canchapro.util.excepciones;

/**
 * Se lanza cuando se busca un registro por código y no existe.
 */
public class EntidadNoEncontradaException extends CanchaProException {

    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
