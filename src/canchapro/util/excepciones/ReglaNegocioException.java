package canchapro.util.excepciones;

/**
 * Se lanza cuando una operación viola una regla del negocio
 * (por ejemplo: usuario duplicado, transición de estado inválida).
 */
public class ReglaNegocioException extends CanchaProException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
