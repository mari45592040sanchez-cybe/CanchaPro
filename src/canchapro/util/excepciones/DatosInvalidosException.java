package canchapro.util.excepciones;

// POO - Herencia: excepción específica que hereda de CanchaProException.
public class DatosInvalidosException extends CanchaProException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
