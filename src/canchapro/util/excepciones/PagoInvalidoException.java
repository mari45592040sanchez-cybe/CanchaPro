package canchapro.util.excepciones;

// POO - Herencia: hereda de ReglaNegocioException (herencia de varios niveles).
public class PagoInvalidoException extends ReglaNegocioException {

    public PagoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
