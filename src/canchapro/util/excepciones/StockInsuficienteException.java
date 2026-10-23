package canchapro.util.excepciones;

/**
 * Se lanza cuando se intenta vender más unidades de las que hay en existencia.
 */
// POO - Herencia: hereda de ReglaNegocioException (herencia de varios niveles).
public class StockInsuficienteException extends ReglaNegocioException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
