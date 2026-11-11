package canchapro.util.excepciones;

// POO - Herencia: hereda de ReglaNegocioException (herencia de varios niveles).
public class StockInsuficienteException extends ReglaNegocioException {

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
