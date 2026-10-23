package canchapro.util.excepciones;

/**
 * Se lanza cuando un dato ingresado o leído de archivo no tiene un
 * formato válido (campo vacío, teléfono incorrecto, número no numérico, etc.).
 */
// POO - Herencia: excepción específica que hereda de CanchaProException.
public class DatosInvalidosException extends CanchaProException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
