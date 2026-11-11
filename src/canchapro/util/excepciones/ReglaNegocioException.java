package canchapro.util.excepciones;

// POO - Herencia: excepción específica que hereda de CanchaProException.
public class ReglaNegocioException extends CanchaProException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
