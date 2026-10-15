package canchapro.util.excepciones;

/**
 * Se lanza cuando un pago no cumple las reglas (monto inválido, mayor al
 * saldo, reserva cancelada, etc.).
 */
public class PagoInvalidoException extends ReglaNegocioException {

    public PagoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
