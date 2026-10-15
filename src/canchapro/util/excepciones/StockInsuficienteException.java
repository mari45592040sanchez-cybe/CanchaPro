package canchapro.util.excepciones;

/**
 * Se lanza cuando se intenta vender más unidades de las que hay en existencia.
 */
public class StockInsuficienteException extends ReglaNegocioException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
