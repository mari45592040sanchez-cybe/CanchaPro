package canchapro.util.excepciones;

/**
 * Excepción base del sistema. Todas las excepciones propias de CanchaPro
 * heredan de esta clase, de modo que las vistas pueden capturarlas en un
 * solo bloque y mostrar su mensaje al usuario.
 */
// POO - Herencia: excepción base; ReglaNegocioException, DatosInvalidosException, etc. heredan de ella.
// POO - Polimorfismo: un solo catch (CanchaProException e) atrapa cualquiera de sus subclases.
public class CanchaProException extends RuntimeException {

    public CanchaProException(String mensaje) {
        super(mensaje);
    }

    public CanchaProException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
